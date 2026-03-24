import java.util.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;


public class ALNS {
    Schedule schedule;
    int iterations;
    double alpha;
    /*
     * Description de l'ALNS :
     * On a une solution courante (currentDays) et une meilleure solution globale (bestDays)
     * A chaque itération, on sélectionne une méthode de destruction et une méthode de réparation selon des poids adaptatifs
     * On applique la destruction puis la réparation sur currentDays pour obtenir une nouvelle solution candidate
     * On évalue la nouvelle solution candidate :
     *   - Si elle est meilleure que currentDays, on l'accepte comme nouvelle solution courante (currentDays = candidate)
     *   - Si elle est meilleure que bestDays, on l'accepte comme nouvelle meilleure solution globale (bestDays = candidate)
     *   - Sinon, on peut accepter la solution candidate avec une certaine probabilité (Simulated Annealing) pour éviter les minima locaux
     * Les poids des méthodes de destruction et de réparation sont mis à jour en fonction de leur performance (amélioration du score)
     *
     */

    public ALNS(Schedule schedule, int iterations, double alpha){
        this.alpha = alpha;
        this.iterations = iterations;
        this.schedule = schedule;
    }

    // Méthode pour sauvegarder la solution dans un fichier .sol
    public void saveSolution(String instanceNumber) {
        String fileName = "Instance" + instanceNumber + ".sol";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            double totalScore = schedule.totalScore(schedule.travelDays);
            writer.write(String.valueOf(totalScore));
            writer.newLine();

             for (int i = 0; i < schedule.daysNb; i++) {
                StringBuilder line = new StringBuilder();
                Day day = schedule.travelDays.get(i);

                line.append(schedule.bestHotelPath.get(i).id);

                for (TouristicSite site : day.chosenSites) {
                    line.append(" ").append(site.id);
                }

                line.append(" ").append(schedule.bestHotelPath.get(i + 1).id);

                writer.write(line.toString());
                writer.newLine();
            }

        } catch (IOException e) {
            System.err.println("Erreur lors de la création du fichier : " + e.getMessage());
        }
    }

    private void relocateOperator(ArrayList<Day> days, Schedule schedule) {
        for (Day day : days) {
            if (day.chosenSites.size() < 2) continue;

            for (int i = 0; i < day.chosenSites.size(); i++) {
                TouristicSite siteToMove = day.chosenSites.remove(i);
                double bestGain = Double.MAX_VALUE;
                int bestPos = -1;

                // On cherche la meilleure nouvelle position dans le même jour
                for (int j = 0; j <= day.chosenSites.size(); j++) {
                    // Simulation de l'insertion (utiliser ta logique de calcul de temps)
                    double newDist = simulateInsertionDist(day, siteToMove, j, schedule);
                    if (newDist < bestGain && newDist <= Instance.tD[day.index]) {
                        bestGain = newDist;
                        bestPos = j;
                    }
                }

                if (bestPos != -1) {
                    day.chosenSites.add(bestPos, siteToMove);
                    day.dailyDistance = recalculateRouteTime(day, schedule);
                } else {
                    day.chosenSites.add(i, siteToMove); // Remettre à sa place si rien de mieux
                }
            }
        }
    }

    /**
     * Simule la distance (durée totale) d'un jour si on insérait un site à une position donnée.
     * @return La nouvelle dailyDistance (incluant les attentes) ou Double.MAX_VALUE si invalide.
     */
    private double simulateInsertionDist(Day day, TouristicSite candidate, int position, Schedule schedule) {
        Hotel hotelDep = schedule.bestHotelPath.get(day.index);
        Hotel hotelArr = schedule.bestHotelPath.get(day.index + 1);

        // Création d'une liste temporaire pour la simulation
        List<TouristicSite> tempSites = new ArrayList<>(day.chosenSites);
        tempSites.add(position, candidate);

        double currentTime = 0;
        Site prev = hotelDep;

        // Parcours de la nouvelle séquence
        for (TouristicSite current : tempSites) {
            // Trajet de prev vers le site actuel
            currentTime += Instance.getDistMatrix(prev, current);

            // Vérification de la fermeture (Fenêtre horaire)
            if (currentTime > current.closingTime) {
                return Double.MAX_VALUE; // Invalide
            }

            // Prise en compte de l'attente si on arrive avant l'ouverture
            if (currentTime < current.openingTime) {
                currentTime = current.openingTime;
            }
            prev = current;
        }

        // Trajet final vers l'hôtel d'arrivée
        currentTime += Instance.getDistMatrix(prev, hotelArr);

        // Vérification du budget temps/distance total du jour
        if (currentTime > Instance.tD[day.index]) {
            return Double.MAX_VALUE; // Dépasse le budget du jour
        }

        return currentTime;
    }
    private void destroyRandomSite(Day day, Schedule schedule) {
        if (!day.chosenSites.isEmpty()) {
            Random random = new Random();
            int indexToRemove = random.nextInt(day.chosenSites.size());
            TouristicSite removedSite = day.chosenSites.get(indexToRemove);

            day.chosenSites.remove(indexToRemove);
            day.dailyScore -= removedSite.score;
            schedule.siteAlreadyVisited.remove(removedSite);

            // on recalcule car le waitingTime des sites suivants va changer
            day.dailyDistance = recalculateRouteTime(day, schedule);

            //System.out.println("Destruction aléatoire : site " + removedSite.id + " retiré. Nouvelle durée totale : " + day.dailyDistance);
        }
    }
    private void destroyWorstRatioSites(ArrayList<Day> days, Schedule schedule) {
        for (Day day : days) {
            if (day.chosenSites.isEmpty()) continue;

            TouristicSite worstSite = null;
            double worstRatio = Double.MAX_VALUE;
            int worstIndex = -1;

            for (int i = 0; i < day.chosenSites.size(); i++) {
                double currentTotalTime = day.dailyDistance;

                // simulation de la suppression pour voir le gain réel (distance + attente)
                ArrayList<TouristicSite> simulatedSites = new ArrayList<>(day.chosenSites);
                TouristicSite candidateToRemove = simulatedSites.remove(i);

                double newTotalTime = calculateTimeForList(simulatedSites, day.index, schedule);
                double timeSaved = currentTotalTime - newTotalTime;

                // eviter la division par zéro si timeSaved est minuscule
                double ratio = candidateToRemove.score / Math.max(timeSaved, 0.001);

                if (ratio < worstRatio) {
                    worstRatio = ratio;
                    worstSite = candidateToRemove;
                    worstIndex = i;
                }
            }

            if (worstSite != null) {
                day.dailyScore -= worstSite.score;
                day.chosenSites.remove(worstIndex);
                schedule.siteAlreadyVisited.remove(worstSite);
                day.dailyDistance = recalculateRouteTime(day, schedule);
            }
        }
    }
    public void runALNSForSites(int maxIterations, double reactionFactor) {
        double[] destroyWeights = {1.0, 1.0, 1.0};
        double[] repairWeights  = {1.0, 1.0, 1.0};

        int destroyedHotel = -1;
        ArrayList<Day> currentDays = cloneDays(schedule.travelDays);
        double currentScore = schedule.totalScore(currentDays);

        ArrayList<Day> bestDays = cloneDays(currentDays);
        double bestScore = currentScore;

        // Température initiale pour le critère d'acceptation (Simulated Annealing)
        //T0 pour accepter une degradation de 5% avec une probabilité de 0.5 au début
        double temperature = currentScore * 0.05 / Math.log(2);
        //refroidissement : à chaque itération T = T * alpha

        for (int iteration = 0; iteration < maxIterations; iteration++) {

            //------ Destruction ------
            int destroyMethodIndex = selectMethod(destroyWeights);
            switch (destroyMethodIndex) {
                case 0:
                    for (Day day : currentDays) destroyRandomSite(day, schedule);
                    break;
                case 1:
                    destroyWorstRatioSites(currentDays, schedule);
                    break;
                case 2:
                    destroyedHotel = destroyRandomHotel(schedule);
                    break;
            }

            // Dans la phase de réparation de runALNSForSites
            if (schedule.bestHotelPath.size() < schedule.daysNb + 1) {
                repairHotelRandomly(schedule, destroyedHotel);
            }

            schedule.siteAlreadyVisited = rebuildVisited(currentDays);

            //-----Reparation-----
            int repairMethodIndex = selectMethod(repairWeights);
            switch (repairMethodIndex) {
                case 0:
                    for (int dayNb = 0; dayNb < schedule.daysNb; dayNb++)
                        repairWithBestScore(schedule, currentDays.get(dayNb));
                    break;
                case 1:
                    for (int dayNb = 0; dayNb < schedule.daysNb; dayNb++)
                        repairWithClosestPosition(schedule, currentDays.get(dayNb));
                    break;
                case 2:
                    for (int dayNb = 0; dayNb < schedule.daysNb; dayNb++)
                        repairWithRegret(schedule, currentDays.get(dayNb),
                                schedule.bestHotelPath.get(dayNb),
                                schedule.bestHotelPath.get(dayNb + 1));
                    break;
            }

            relocateInterDays(currentDays, schedule);
            for(int d=0; d < currentDays.size(); d++) {
                currentDays.get(d).refreshStats(d, schedule);
            }

            double newScore = schedule.totalScore(currentDays);
            double delta = newScore - currentScore; // positif = amélioration

            // ---- Critere d'acceptation (Simulated Annealing) ----
            boolean accept = false;
            if (delta >= 0) {
                //accepter l'amelioration
                accept = true;
            } else if (temperature > 1e-6) {
                // Accepter une dégradation avec probabilité exp(delta / T)
                // Plus T est grand, plus on accepte de dégradations
                double probability = Math.exp(delta / temperature);
                accept = Math.random() < probability;
            }

            // Mise à jour des poids selon le résultat
            if (accept) {
                if (delta > 0) {
                    updateWeights(destroyWeights, destroyMethodIndex, delta, reactionFactor);
                    updateWeights(repairWeights,  repairMethodIndex,  delta, reactionFactor);
                }
                currentScore = newScore;
                currentDays  = cloneDays(currentDays); // on garde cette solution comme courante

                if (newScore > bestScore) {
                    bestScore = newScore;
                    bestDays  = cloneDays(currentDays);
                    //System.out.println("Nouvelle meilleure solution à l'itération " + iteration + " : score = " + bestScore);
                }
            } else {
                //si refus revenir à la solution courante (pas la meilleure globale)
                currentDays = cloneDays(currentDays); // reset au currentDays avant modif
                schedule.siteAlreadyVisited = rebuildVisited(currentDays);
            }
            // ---- Refroidissement : T = T * alpha ----
            temperature *= alpha;
        }

        schedule.travelDays = cloneDays(bestDays);
        schedule.getCleanTotalScore();
        schedule.siteAlreadyVisited = rebuildVisited(bestDays);
    }

    private int destroyRandomHotel(Schedule schedule) {
        if (schedule.bestHotelPath.size() <= 2) return -1;

        Random rand = new Random();
        int hotelIdx = rand.nextInt(schedule.bestHotelPath.size() - 2) + 1;

        schedule.bestHotelPath.remove(hotelIdx);
        return hotelIdx;
    }

    private void repairHotelRandomly(Schedule schedule, int destroyedHotelIndex ) {
        if (destroyedHotelIndex == -1) return;

        Hotel hPrev = schedule.bestHotelPath.get(destroyedHotelIndex - 1);
        Hotel hNext = schedule.bestHotelPath.get(destroyedHotelIndex);

        // trouver les hôtels valides à insérer entre hPrev et hNext
        List<Hotel> validHotels = new ArrayList<>();
        for (Hotel h : Instance.hotels) {
            double d1 = Instance.getDistMatrix(hPrev, h);
            double d2 = Instance.getDistMatrix(h, hNext);

            // Si l'hôtel est physiquement accessible dans les budgets temps
            if (d1 < Instance.tD[destroyedHotelIndex - 1] && d2 < Instance.tD[destroyedHotelIndex]) {
                validHotels.add(h);
            }
        }

        //choisir un hotel au hasard parmi les valides (diversification)
        if (!validHotels.isEmpty()) {
            Random rand = new Random();
            Hotel randomHotel = validHotels.get(rand.nextInt(validHotels.size()));
            schedule.bestHotelPath.add(destroyedHotelIndex, randomHotel);
        } else {
            // Backup : remettre l'hôtel de fin pour ne pas casser la structure
            schedule.bestHotelPath.add(destroyedHotelIndex, Instance.getLastHotel());
        }

        destroyedHotelIndex = -1; // Reset
    }

    private ArrayList<Day> cloneDays(ArrayList<Day> days) {
        ArrayList<Day> clonedDays = new ArrayList<>();
        for (Day day : days) {
            Day clonedDay = new Day(day.index);
            clonedDay.chosenSites.addAll(day.chosenSites);
            clonedDay.dailyDistance = day.dailyDistance;
            clonedDay.dailyScore = day.dailyScore;
            clonedDay.setChosenHotel(day.getChosenHotel());
            clonedDays.add(clonedDay);
        }
        return clonedDays;
    }

    // reconstruire siteAlreadyVisited à partir d'un clone de days
    private Set<TouristicSite> rebuildVisited(ArrayList<Day> days) {
        Set<TouristicSite> visited = new HashSet<>();
        for (Day day : days) {
            visited.addAll(day.chosenSites);
        }
        return visited;
    }


    private void relocateInterDays(ArrayList<Day> days, Schedule schedule) {
        for (int d = 0; d < days.size() - 1; d++) { // pour chaque paire de jours consécutifs
            Day day1 = days.get(d);
            Day day2 = days.get(d+1);

            for (int i = 0; i < day1.chosenSites.size(); i++) {
                TouristicSite site = day1.chosenSites.get(i);

                //on essaie de le mettre dans le jour suivant
                for (int pos = 0; pos <= day2.chosenSites.size(); pos++) {
                    double newDistJ2 = simulateInsertionDist(day2, site, pos, schedule);

                    if (newDistJ2 != Double.MAX_VALUE) { // Si ça rentre dans J2
                        day1.chosenSites.remove(i);
                        day2.chosenSites.add(pos, site);

                        // Mise à jour des distances
                        day1.dailyDistance = recalculateRouteTime(day1, schedule);
                        day2.dailyDistance = recalculateRouteTime(day2, schedule);
                        return; // On a fait un transfert, on sort pour cette itération
                    }
                }
            }
        }
    }
    private void repairWithRegret(Schedule schedule, Day day, Hotel hotelDep, Hotel hotelArr) {
        int dayNb = day.index;
        Site lastSite = day.getLastSite();

        // calcul du regret pour chaque site à la fin de la journée (en partant de l'hotel d'arrivée)

        if (lastSite == null) {
            lastSite = hotelDep;
        }

        List<TouristicSite> candidates = new ArrayList<>();
        for (TouristicSite site : Instance.touristicSites) {
            if (schedule.siteAlreadyVisited.contains(site) || day.chosenSites.contains(site)) {
                continue;
            }

            double distToSite = Instance.getDistMatrix(lastSite, site);
            double distToHotel = Instance.getDistMatrix(site, hotelArr);
            double distDirect = Instance.getDistMatrix(lastSite, hotelArr);
            double detourDistance = distToSite + distToHotel ;

            double arrivalTime = day.dailyDistance + distToSite;
            if (arrivalTime > site.closingTime) {
                continue;
            }

            double waitingTime = Math.max(0, site.openingTime - arrivalTime);
            double realCost = detourDistance + waitingTime;

            if (day.dailyDistance - distDirect + realCost > Instance.tD[dayNb]) {
                continue;
            }

            candidates.add(site);
        }

        if (candidates.isEmpty()) {
            return;
        }
        double maxRegret = -1.0; //si positif considéré comme une bonne option
        TouristicSite bestSite = null;

        for (TouristicSite site : candidates) {
            double siteRegret = calculateRegret(schedule, day, site, hotelDep, hotelArr);
            if (siteRegret > maxRegret) {
                maxRegret = siteRegret;
                bestSite = site;
            }
        }

        if (bestSite != null) {
            // Mise à jour de la distance parcourue
            double distToSite = Instance.getDistMatrix(lastSite, bestSite);
            day.dailyDistance += distToSite;

            day.addChosenSite(bestSite);
            schedule.siteAlreadyVisited.add(bestSite);
            day.dailyScore += bestSite.score;
            //System.out.println("Ajout du site " + bestSite.id + " avec un regret de " + maxRegret);
            //System.out.println("Nouvelle distance totale : " + day.dailyDistance);
        }
    }

    private double calculateRegret(Schedule schedule, Day day, TouristicSite site, Hotel hotelDep, Hotel hotelArr) {
        Site lastSite = day.getLastSite();
        if (lastSite == null) {
            lastSite = hotelDep;
        }
        // Calculer le score du site
        double distToSite = Instance.getDistMatrix(lastSite, site);
        double distToHotel = Instance.getDistMatrix(site, hotelArr);
        double distDirect = Instance.getDistMatrix(lastSite, hotelArr);
        double detourDistance = distToSite + distToHotel ;

        double arrivalTime = day.dailyDistance + distToSite;
        if (arrivalTime > site.closingTime) {
            return -1; // pas accessible
        }

        double waitingTime = Math.max(0, site.openingTime - arrivalTime);
        double realCost = detourDistance + waitingTime;

        if (day.dailyDistance + distToSite > Instance.tD[day.index]) {
            return -1; // trop loin pour être ajouté
        }

        double siteScore = site.score / realCost;

        // Trouver le deuxième meilleur site
        double secondBestScore = -1.0;
        for (TouristicSite otherSite : Instance.touristicSites) {
            if (schedule.siteAlreadyVisited.contains(otherSite) || day.chosenSites.contains(otherSite) || otherSite == site) {
                continue;
            }

            double otherDistToSite = Instance.getDistMatrix(lastSite, otherSite);
            double otherDistToHotel = Instance.getDistMatrix(otherSite, hotelArr);
            double otherDistDirect = Instance.getDistMatrix(lastSite, hotelArr);
            double otherDetourDistance = otherDistToSite + otherDistToHotel - otherDistDirect;

            double otherArrivalTime = day.dailyDistance + otherDistToSite;
            if (otherArrivalTime > otherSite.closingTime) {
                continue;
            }

            double otherWaitingTime = Math.max(0, otherSite.openingTime - otherArrivalTime);
            double otherRealCost = otherDetourDistance + otherWaitingTime;

            if (day.dailyDistance + otherDistToSite > Instance.tD[day.index]) {
                continue;
            }

            double otherScore = otherSite.score / otherRealCost;
            if (otherScore > secondBestScore) {
                secondBestScore = otherScore;
            }
        }

        //Le regret est la différence entre le meilleur score et le deuxième meilleur score
        return siteScore - secondBestScore;
    }


    private void repairWithClosestPosition(Schedule schedule, Day day) {
        //reparation du jour en choisissant le site le plus proche du dernier site visité
        selectClosestPosition(schedule,day);
    }
    private void repairWithBestScore(Schedule schedule, Day day) {
        //reparation du jour en choisissant le site avec le meilleur score si possible
        double bestRatio = -1.0;
        TouristicSite bestSite = null;
        int dayNb = day.index;

        List<Score> scores = new ArrayList<>();
        ArrayList<TouristicSite> tSites = Instance.getTouristicSites();
        for (int i = 0; i < tSites.size() ; i++) {
            if (schedule.siteAlreadyVisited.contains(tSites.get(i)) || day.chosenSites.contains(tSites.get(i))) {
                continue; // Éviter les doublons
            }
            if (tSites.get(i) == null) {
                continue;
            }
            scores.add(new Score(tSites.get(i), (double)tSites.get(i).score));
        }
        //tri décroissant des meilleurs scores
        scores.sort((s1, s2) -> Double.compare(s2.score1, s1.score1));
        //affichage des scores pour le jour

        selectBestPosition(scores, day);

    }
    private int selectMethod(double[] weights) {
        double totalWeight = Arrays.stream(weights).sum();
        double randomValue = Math.random() * totalWeight;
        double cumulativeWeight = 0.0;

        for (int i = 0; i < weights.length; i++) {
            cumulativeWeight += weights[i];
            if (randomValue <= cumulativeWeight) {
                return i;
            }
        }

        return weights.length - 1;
    }

    private void updateWeights(double[] weights, int methodIndex, double scoreImprovement, double reactionFactor) {
        if (scoreImprovement > 0) {
            weights[methodIndex] *= (1 + reactionFactor); // Augmente le poids de la méthode si elle a contribué à une amélioration
        } else {
            weights[methodIndex] *= (1 - reactionFactor);
        }
    }
    private void selectBestPosition(List<Score> sitesScore, Day day) {
        Hotel hotelDep = schedule.bestHotelPath.get(day.index);
        Hotel hotelArr = schedule.bestHotelPath.get(day.index + 1);

        for (Score s : sitesScore) {
            TouristicSite candidate = (TouristicSite) s.site;

            // site déjà visité
            if (schedule.siteAlreadyVisited.contains(candidate)) continue;

            int bestIdx = -1;
            double minDetour = Double.MAX_VALUE;

            // Tester chaque position d'insertion possible pour ce site
            for (int j = 0; j <= day.chosenSites.size(); j++) {
                Site prev = (j == 0) ? hotelDep : day.chosenSites.get(j - 1);
                Site next = (j == day.chosenSites.size()) ? hotelArr : day.chosenSites.get(j);

                //calcul su temps d'arrivée
                // Il faut recalculer le temps depuis le début de la journée pour être précis
                double currentTime = 0;
                for (int k = 0; k < j; k++) {
                    Site s1 = (k == 0) ? hotelDep : day.chosenSites.get(k - 1);
                    Site s2 = day.chosenSites.get(k);
                    currentTime += Instance.getDistMatrix(s1, s2);
                    if (currentTime < s2.openingTime) currentTime = s2.openingTime;
                }

                double arrivalAtCandidate = currentTime + Instance.getDistMatrix(prev, candidate);

                if (arrivalAtCandidate > candidate.closingTime) continue;

                double departureFromCandidate = Math.max(arrivalAtCandidate, candidate.openingTime);

                // est-ce que les sites suivants restent valides ?
                double nextWaitTime = departureFromCandidate + Instance.getDistMatrix(candidate, next);
                boolean impactValid = true;

                // On simule la suite du parcours
                double tempTime = nextWaitTime;
                for (int k = j; k < day.chosenSites.size(); k++) {
                    TouristicSite followingSite = day.chosenSites.get(k);
                    if (tempTime > followingSite.closingTime) {
                        impactValid = false;
                        break;
                    }
                    tempTime = Math.max(tempTime, followingSite.openingTime) +
                            Instance.getDistMatrix(followingSite, (k == day.chosenSites.size() - 1 ? hotelArr : day.chosenSites.get(k+1)));
                }

                if (!impactValid) continue;

                double distToCand = Instance.getDistMatrix(prev, candidate);
                double distFromCand = Instance.getDistMatrix(candidate, next);
                double distDirect = Instance.getDistMatrix(prev, next);
                double detour = distToCand + distFromCand - distDirect;

                // condition de distance totale
                if (day.dailyDistance + detour <= Instance.tD[day.index]) {
                    if (detour < minDetour) {
                        minDetour = detour;
                        bestIdx = j;
                    }
                }
            }
            // on insère le candidat si la position est trouvée
            if (bestIdx != -1) {
                day.chosenSites.add(bestIdx, candidate);
                day.dailyDistance += minDetour;
                day.dailyScore += candidate.score;
                schedule.siteAlreadyVisited.add(candidate);

                return;
            }
        }
    }
    private void selectClosestPosition(Schedule schedule, Day day) {
        int dayNb = day.index;
        Hotel hotelDep = schedule.bestHotelPath.get(dayNb);
        Hotel hotelArr = schedule.bestHotelPath.get(dayNb + 1);

        TouristicSite globalBestCandidate = null;
        int globalBestPos = -1;
        double globalBestRealCost = Double.MAX_VALUE;

        for (TouristicSite candidate : Instance.getTouristicSites()) {
            if (schedule.siteAlreadyVisited.contains(candidate)) continue;

            double bestRealCostForCandidate = Double.MAX_VALUE;
            int bestPosForCandidate = -1;

            //tester chaque position d'insertion j
            for (int j = 0; j <= day.chosenSites.size(); j++) {
                Site prev = (j == 0) ? hotelDep : day.chosenSites.get(j - 1);
                Site next = (j == day.chosenSites.size()) ? hotelArr : day.chosenSites.get(j);

                // calcul du temps d'arrivée au candidat en partant de l'hôtel de départ
                double currentTime = 0;
                // Temps pour arriver au point 'prev'
                for (int k = 0; k < j; k++) {
                    Site s1 = (k == 0) ? hotelDep : day.chosenSites.get(k - 1);
                    Site s2 = day.chosenSites.get(k);
                    currentTime += Instance.getDistMatrix(s1, s2);
                    if (currentTime < s2.openingTime) currentTime = s2.openingTime;
                }

                double arrivalAtCandidate = currentTime + Instance.getDistMatrix(prev, candidate);
                // vérification des horaires du candidat
                if (arrivalAtCandidate > candidate.closingTime) continue;

                double departureFromCandidate = Math.max(arrivalAtCandidate, candidate.openingTime);

                // est-ce que les sites suivants restent visitables ?
                double tempTime = departureFromCandidate + Instance.getDistMatrix(candidate, next);
                boolean impactValid = true;

                for (int k = j; k < day.chosenSites.size(); k++) {
                    TouristicSite followingSite = day.chosenSites.get(k);
                    if (tempTime > followingSite.closingTime) {
                        impactValid = false;
                        break;
                    }
                    tempTime = Math.max(tempTime, followingSite.openingTime) +
                            Instance.getDistMatrix(followingSite, (k == day.chosenSites.size() - 1 ? hotelArr : day.chosenSites.get(k+1)));
                }
                if (!impactValid) continue;

                // cout réel = Distance + Attente
                double distToCand = Instance.getDistMatrix(prev, candidate);
                double distFromCand = Instance.getDistMatrix(candidate, next);
                double distDirect = Instance.getDistMatrix(prev, next);
                double detour = distToCand + distFromCand - distDirect;

                double waitingTime = Math.max(0, candidate.openingTime - arrivalAtCandidate);
                double realCost = detour + waitingTime;

                // CONDITION 3 : Budget distance/temps total du jour
                if (day.dailyDistance + realCost <= Instance.tD[dayNb]) {
                    if (realCost < bestRealCostForCandidate) {
                        bestRealCostForCandidate = realCost;
                        bestPosForCandidate = j;
                    }
                }
            }

            // Est-ce le meilleur site à insérer globalement ?
            if (bestPosForCandidate != -1 && bestRealCostForCandidate < globalBestRealCost) {
                globalBestRealCost = bestRealCostForCandidate;
                globalBestCandidate = candidate;
                globalBestPos = bestPosForCandidate;
            }
        }

        if (globalBestCandidate != null) {
            day.chosenSites.add(globalBestPos, globalBestCandidate);
            day.dailyDistance = recalculateRouteTime(day, schedule);
            day.dailyScore += globalBestCandidate.score;
            schedule.siteAlreadyVisited.add(globalBestCandidate);

            //System.out.println("Repair Closest: Ajout " + globalBestCandidate.id + " en pos " + globalBestPos);
        }
    }
    private double recalculateRouteTime(Day day, Schedule schedule) {
        return calculateTimeForList(day.chosenSites, day.index, schedule);
    }
    private double calculateTimeForList(ArrayList<TouristicSite> sites, int dayIdx, Schedule schedule) {
        Hotel dep = schedule.bestHotelPath.get(dayIdx);
        Hotel arr = schedule.bestHotelPath.get(dayIdx + 1);

        double currentTime = 0;
        Site previous = dep;

        for (TouristicSite current : sites) {
            currentTime += Instance.getDistMatrix(previous, current);
            if (currentTime < current.openingTime) {
                currentTime = current.openingTime;
            }
            previous = current;
        }
        currentTime += Instance.getDistMatrix(previous, arr);

        return currentTime;
    }
}
