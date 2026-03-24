import java.util.*;

public class VNS {
    public double bestGlobalScore = -1.0;
    public Schedule bestGlobalSchedule = null;

    public List<Hotel> bestGlobalHotelPath = null;

    //-----------------------------EXECUTION RUN VNS------------------
    public void executeVNS(Schedule initialSchedule, List<Hotel> currentHotelPath, long maxDurationMs) {
        long startTime = System.currentTimeMillis();

        Schedule currentSchedule = initialSchedule.cloneSchedule();
        Schedule runBestSchedule = currentSchedule.cloneSchedule();
        double runBestScore = currentSchedule.totalScore(runBestSchedule.travelDays);


        if (this.bestGlobalSchedule == null || runBestScore > this.bestGlobalScore) {
            this.bestGlobalSchedule = runBestSchedule.cloneSchedule();
            this.bestGlobalScore = runBestScore;
        }

        int k_max = 4; // 4 niveles de destrucción

        while ((System.currentTimeMillis() - startTime) < maxDurationMs) {
            int k = 1;

            while (k <= k_max && (System.currentTimeMillis() - startTime) < maxDurationMs) {

                Schedule shakenSchedule = currentSchedule.cloneSchedule();
                this.shake(shakenSchedule, k, currentHotelPath);

                Schedule localOptimum = this.applyLocalSearch(shakenSchedule, currentHotelPath);

                double newScore = localOptimum.totalScore(localOptimum.travelDays);
                double currentScore = currentSchedule.totalScore(currentSchedule.travelDays);

                double newDist = 0, currentDist = 0;
                for (int i = 0; i < localOptimum.travelDays.size(); i++) newDist += localOptimum.travelDays.get(i).dailyDistance;
                for (int i = 0; i < currentSchedule.travelDays.size(); i++) currentDist += currentSchedule.travelDays.get(i).dailyDistance;

                if (newScore > currentScore || (newScore == currentScore && newDist < currentDist - 0.001)) {
                    currentSchedule = localOptimum;
                    k = 1; // Reiniciamos la búsqueda al mejorar

                    if (newScore > runBestScore) {
                        runBestSchedule = currentSchedule.cloneSchedule();
                        runBestScore = newScore;
                    }

                    if (newScore > this.bestGlobalScore) {
                        this.bestGlobalSchedule = currentSchedule.cloneSchedule();
                        this.bestGlobalScore = newScore;
                        this.bestGlobalHotelPath = new ArrayList<>(currentHotelPath);
                        System.out.println(">>> NOUVEAU RECORD GLOBAL: " + this.bestGlobalScore + " <<<");
                    }
                } else {
                    k++; // Destruimos más fuerte si nos estancamos
                }
            }
            // Retorno seguro a la cima de la montaña si no pudimos mejorar
            currentSchedule = runBestSchedule.cloneSchedule();
        }
    }

    //------------------LOCAL SEARCH MONSTRUO (VND) ----------------------
    public Schedule applyLocalSearch(Schedule currentSchedule, List<Hotel> currentHotelPath) {
        boolean improved = true;

        while (improved) {
            improved = false;

            // FASE 1: Compresión Intra-Día (Ahorro de tiempos de espera)
            for (int dayNb = 0; dayNb < currentSchedule.daysNb; dayNb++) {
                Day day = currentSchedule.travelDays.get(dayNb);
                if (day.chosenSites.size() < 2) continue;
                Hotel hotelDep = currentHotelPath.get(dayNb);
                Hotel hotelArr = currentHotelPath.get(dayNb + 1);

                for (int i = 0; i < day.chosenSites.size(); i++) {
                    for (int j = 0; j <= day.chosenSites.size(); j++) {
                        if (i == j || i == j - 1) continue;
                        List<TouristicSite> testSequence = new ArrayList<>();
                        for (Object obj : day.chosenSites) testSequence.add((TouristicSite) obj);

                        TouristicSite siteToMove = testSequence.remove(i);
                        int insertPos = (j > i) ? j - 1 : j;
                        testSequence.add(insertPos, siteToMove);

                        double[] result = currentSchedule.evaluateSequence(dayNb, testSequence, hotelDep, hotelArr);
                        if (result[0] == 1.0 && result[1] < day.dailyDistance - 0.001) {
                            day.chosenSites.clear(); day.chosenSites.addAll(testSequence);
                            day.setDailyDistance(result[1]);
                            improved = true;
                            break;
                        }
                    }
                    if (improved) break;
                }
            }
            if (improved) continue;

            // FASE 2: Relocación Inter-Día (Mover sitios de un día saturado a uno libre)
            if (currentSchedule.daysNb > 1) {
                for (int fromDayIdx = 0; fromDayIdx < currentSchedule.daysNb; fromDayIdx++) {
                    for (int toDayIdx = 0; toDayIdx < currentSchedule.daysNb; toDayIdx++) {
                        if (fromDayIdx == toDayIdx) continue;
                        Day fromDay = currentSchedule.travelDays.get(fromDayIdx);
                        Day toDay = currentSchedule.travelDays.get(toDayIdx);

                        for (int i = 0; i < fromDay.chosenSites.size(); i++) {
                            TouristicSite siteToMove = (TouristicSite) fromDay.chosenSites.get(i);

                            for (int j = 0; j <= toDay.chosenSites.size(); j++) {
                                List<TouristicSite> testSeqFrom = new ArrayList<>();
                                for (Object obj : fromDay.chosenSites) testSeqFrom.add((TouristicSite) obj);
                                testSeqFrom.remove(i);

                                List<TouristicSite> testSeqTo = new ArrayList<>();
                                for (Object obj : toDay.chosenSites) testSeqTo.add((TouristicSite) obj);
                                testSeqTo.add(j, siteToMove);

                                double[] resFrom = currentSchedule.evaluateSequence(fromDayIdx, testSeqFrom, currentHotelPath.get(fromDayIdx), currentHotelPath.get(fromDayIdx + 1));
                                double[] resTo = currentSchedule.evaluateSequence(toDayIdx, testSeqTo, currentHotelPath.get(toDayIdx), currentHotelPath.get(toDayIdx + 1));

                                // ¡EL FIX ESTÁ AQUÍ!
                                // Solo aceptamos la mudanza si AMBAS rutas son válidas Y ADEMÁS la distancia global de los dos días disminuye.
                                if (resFrom[0] == 1.0 && resTo[0] == 1.0) {
                                    if ((resFrom[1] + resTo[1]) < (fromDay.dailyDistance + toDay.dailyDistance - 0.001)) {
                                        fromDay.chosenSites.clear(); fromDay.chosenSites.addAll(testSeqFrom);
                                        fromDay.setDailyDistance(resFrom[1]); fromDay.setDailyScore(resFrom[2]);

                                        toDay.chosenSites.clear(); toDay.chosenSites.addAll(testSeqTo);
                                        toDay.setDailyDistance(resTo[1]); toDay.setDailyScore(resTo[2]);

                                        improved = true;
                                        break;
                                    }
                                }
                            }
                            if (improved) break;
                        }
                        if (improved) break;
                    }
                    if (improved) break;
                }
            }
            if (improved) continue;

            // Preparar la lista de No Visitados con CAOS CONTROLADO
            List<TouristicSite> unvisited = new ArrayList<>();
            for (TouristicSite s : Instance.touristicSites) {
                if (!currentSchedule.siteAlreadyVisited.contains(s)) unvisited.add(s);
            }
            Collections.shuffle(unvisited);
            unvisited.sort((s1, s2) -> Integer.compare(s2.score, s1.score));

            // FASE 3: Inserción de Nuevos Sitios
            for (int dayNb = 0; dayNb < currentSchedule.daysNb; dayNb++) {
                Day day = currentSchedule.travelDays.get(dayNb);
                Hotel hotelDep = currentHotelPath.get(dayNb);
                Hotel hotelArr = currentHotelPath.get(dayNb + 1);

                for (TouristicSite candidateSite : unvisited) {
                    int bestPosition = -1;
                    double bestDistanceCost = Double.MAX_VALUE;
                    double bestNewScore = -1.0;

                    for (int pos = 0; pos <= day.chosenSites.size(); pos++) {
                        List<TouristicSite> testSequence = new ArrayList<>();
                        for (Object obj : day.chosenSites) testSequence.add((TouristicSite) obj);
                        testSequence.add(pos, candidateSite);

                        double[] result = currentSchedule.evaluateSequence(dayNb, testSequence, hotelDep, hotelArr);
                        if (result[0] == 1.0 && result[1] < bestDistanceCost) {
                            bestDistanceCost = result[1]; bestNewScore = result[2]; bestPosition = pos;
                        }
                    }

                    if (bestPosition != -1) {
                        day.chosenSites.add(bestPosition, candidateSite);
                        day.setDailyDistance(bestDistanceCost); day.setDailyScore(bestNewScore);
                        currentSchedule.siteAlreadyVisited.add(candidateSite);
                        improved = true;
                        break;
                    }
                }
                if (improved) break;
            }
            if (improved) continue;

            // FASE 4: Reemplazo (Sacar un sitio barato y meter uno caro)
            for (int dayNb = 0; dayNb < currentSchedule.daysNb; dayNb++) {
                Day day = currentSchedule.travelDays.get(dayNb);
                Hotel hotelDep = currentHotelPath.get(dayNb);
                Hotel hotelArr = currentHotelPath.get(dayNb + 1);

                for (int i = 0; i < day.chosenSites.size(); i++) {
                    TouristicSite visitedSite = (TouristicSite) day.chosenSites.get(i);

                    for (TouristicSite candidateSite : unvisited) {
                        if (candidateSite.score < visitedSite.score) continue;

                        List<TouristicSite> testSequence = new ArrayList<>();
                        for (Object obj : day.chosenSites) testSequence.add((TouristicSite) obj);
                        testSequence.set(i, candidateSite);

                        double[] result = currentSchedule.evaluateSequence(dayNb, testSequence, hotelDep, hotelArr);
                        if (result[0] == 1.0) {
                            if (result[2] > day.dailyScore || (result[2] == day.dailyScore && result[1] < day.dailyDistance - 0.001)) {
                                day.chosenSites.clear(); day.chosenSites.addAll(testSequence);
                                day.setDailyDistance(result[1]); day.setDailyScore(result[2]);
                                currentSchedule.siteAlreadyVisited.remove(visitedSite);
                                currentSchedule.siteAlreadyVisited.add(candidateSite);
                                improved = true;
                                break;
                            }
                        }
                    }
                    if (improved) break;
                }
                if (improved) break;
            }
        }
        return currentSchedule;
    }

    //---------------------------- SHAKE SIMPLIFICADO ----------------------------------
    // Ahora el Shake solo arranca sitios de la ruta al azar. La Búsqueda Local se encarga de rearmarlo a la perfección.
    public void shake(Schedule currentSchedule, int k, List<Hotel> currentHotelPath) {
        Random rand = new Random();
        int sitesToRemove = k * 2; // k=1 quita 2, k=2 quita 4, k=3 quita 6, k=4 quita 8.

        List<TouristicSite> allChosen = new ArrayList<>();
        for (int dayNb = 0; dayNb < currentSchedule.daysNb; dayNb++) {
            Day day = currentSchedule.travelDays.get(dayNb);
            for (Object obj : day.chosenSites) allChosen.add((TouristicSite) obj);
        }

        sitesToRemove = Math.min(sitesToRemove, allChosen.size());
        if (sitesToRemove == 0) return;

        Collections.shuffle(allChosen, rand);
        List<TouristicSite> toRemove = allChosen.subList(0, sitesToRemove);

        for (int dayNb = 0; dayNb < currentSchedule.daysNb; dayNb++) {
            currentSchedule.travelDays.get(dayNb).chosenSites.removeAll(toRemove);
        }
        for (TouristicSite site : toRemove) {
            currentSchedule.siteAlreadyVisited.remove(site);
        }

        // Recalcular las distancias bases
        for (int dayNb = 0; dayNb < currentSchedule.daysNb; dayNb++) {
            Day day = currentSchedule.travelDays.get(dayNb);
            Hotel hotelDep = currentHotelPath.get(dayNb);
            Hotel hotelArr = currentHotelPath.get(dayNb + 1);

            List<TouristicSite> remaining = new ArrayList<>();
            for (Object obj : day.chosenSites) remaining.add((TouristicSite) obj);

            double[] result = currentSchedule.evaluateSequence(dayNb, remaining, hotelDep, hotelArr);
            if (result[0] == 1.0) {
                day.setDailyDistance(result[1]);
                day.setDailyScore(result[2]);
            }
        }
    }
}