import java.util.ArrayList;
import java.util.Random;
import java.util.List;
import java.util.Arrays;

public class ALNS {
    Schedule schedule;
    int iterations;
    double alpha;
    /*
     * Utilisation des destructions et répartions
     * Insertion en meilleure position et ordre aléatoire, et insertion basée sur le regret
     */

    public ALNS(Schedule schedule, int iterations, double alpha){
        this.alpha = alpha;
        this.iterations = iterations;
        this.schedule = schedule;
    }

    private void destroyWorstRatioSites(ArrayList<Day> days, Schedule schedule) {
        for (Day day : days) {
            if (day.chosenSites.isEmpty()) {
                continue;
            }

            // Trouver le site avec le pire ratio score/distance
            TouristicSite worstSite = null;
            double worstRatio = Double.MAX_VALUE;

            for (TouristicSite site : day.chosenSites) {
                double detourDistance = Instance.getDistMatrix(day.getLastSite(), site) + Instance.getDistMatrix(site, schedule.bestHotelPath.get(day.index + 1)) - Instance.getDistMatrix(day.getLastSite(), schedule.bestHotelPath.get(day.index + 1));
                double ratio = site.score / detourDistance;

                if (ratio < worstRatio) {
                    worstRatio = ratio;
                    worstSite = site;
                }
            }

            if (worstSite != null) {
                day.chosenSites.remove(worstSite);
                schedule.siteAlreadyVisited.remove(worstSite);
            }
        }
    }

    public void runALNSForSites(int maxIterations, double reactionFactor) {
        // Initialiser les poids des méthodes de destruction et de réparation
        double[] destroyWeights = {1.0, 1.0, 1.0};
        double[] repairWeights = {1.0, 1.0, 1.0};

        // Sauvegarder la solution initiale
        ArrayList<Day> currentDays = cloneDays(schedule.travelDays);
        double currentScore = schedule.totalScore(currentDays);

        // Meilleure solution
        ArrayList<Day> bestDays = cloneDays(currentDays);
        double bestScore = currentScore;

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            // Sélectionner une méthode de destruction
            int destroyMethodIndex = selectMethod(destroyWeights);
            switch (destroyMethodIndex) {
                case 0:
                    for (Day day : currentDays) {
                        destroyRandomSite(day, schedule);
                    }
                    break;
                case 1:
                    destroyWorstRatioSites(currentDays, schedule);
                    break;
                case 2:
                    // Autre méthode de destruction
                    break;
            }

            // Sélectionner une méthode de réparation
            int repairMethodIndex = selectMethod(repairWeights);
            switch (repairMethodIndex) {
                case 0:
                    for (int dayNb = 0; dayNb < schedule.daysNb; dayNb++) {
                        Day day = currentDays.get(dayNb);
                        Hotel dep = schedule.bestHotelPath.get(dayNb);
                        Hotel arr = schedule.bestHotelPath.get(dayNb + 1);
                        repairWithBestRatio(schedule,day, dep, arr);
                    }
                    break;
                case 1:
                    // Autre méthode de réparation
                    break;
                case 2:
                    // Autre méthode de réparation
                    break;
            }

            // Calculer le score de la nouvelle solution
            double newScore = schedule.totalScore(currentDays);

            // Mettre à jour les poids
            if (newScore > currentScore) {
                updateWeights(destroyWeights, destroyMethodIndex, newScore - currentScore, reactionFactor);
                updateWeights(repairWeights, repairMethodIndex, newScore - currentScore, reactionFactor);
                currentScore = newScore;

                if (newScore > bestScore) {
                    bestScore = newScore;
                    bestDays = cloneDays(currentDays);
                }
            } else {
                // Réinitialiser la solution courante
                currentDays = cloneDays(bestDays);
            }
        }

        // Mettre à jour les jours avec la meilleure solution trouvée
        schedule.travelDays = cloneDays(bestDays);
    }

    private ArrayList<Day> cloneDays(ArrayList<Day> days) {
        ArrayList<Day> clonedDays = new ArrayList<>();
        for (Day day : days) {
            Day clonedDay = new Day(day.index);
            clonedDay.chosenSites.addAll(day.chosenSites);
            clonedDay.dailyDistance = day.dailyDistance;
            clonedDay.dailyScore = day.dailyScore;
            clonedDays.add(clonedDay);
        }
        return clonedDays;
    }

    private void destroyRandomSite(Day day, Schedule schedule) {
        Random random = new Random();
        if (!day.chosenSites.isEmpty()) {
            int indexToRemove = random.nextInt(day.chosenSites.size());
            TouristicSite removedSite = day.chosenSites.remove(indexToRemove);
            schedule.siteAlreadyVisited.remove(removedSite);
        }
    }

    private void repairWithBestRatio(Schedule schedule, Day day, Hotel hotelDep, Hotel hotelArr) {
        double bestRatio = -1.0;
        TouristicSite bestSite = null;
        int dayNb = day.index;
        Site lastSite = day.getLastSite(); // Dernier site ou hôtel de départ
        System.out.println("Réparation du jour " + dayNb + " avec hôtel de départ " + hotelDep.id + " et hôtel d'arrivée " + hotelArr.id);
        if (lastSite == null) {
            lastSite = hotelDep;
        }

        for (TouristicSite site : Instance.touristicSites) {
            if (schedule.siteAlreadyVisited.contains(site) || day.chosenSites.contains(site)) {
                continue; // Éviter les doublons
            }

            if (site == null) {
                continue;
            }

            // Calcul du détour
            double distToSite = Instance.getDistMatrix(lastSite, site);
            double distToHotel = Instance.getDistMatrix(site, hotelArr);
            double distDirect = Instance.getDistMatrix(lastSite, hotelArr);
            double detourDistance = distToSite + distToHotel - distDirect;

            // Temps d'arrivée et d'attente
            double arrivalTime = day.dailyDistance + distToSite;
            if (arrivalTime > site.closingTime) {
                continue; // Arrivée après la fermeture
            }
            double waitingTime = Math.max(0, site.openingTime - arrivalTime);
            double realCost = detourDistance + waitingTime;

            // Vérification du budget distance
            if (day.dailyDistance + distToSite > Instance.tD[dayNb]) {
                continue; // Dépassement du budget distance
            }

            // Calcul du ratio
            double ratio = site.score / realCost;
            if (ratio > bestRatio) {
                bestRatio = ratio;
                bestSite = site;
            }
        }

        if (bestSite != null) {
            // Mise à jour de la distance parcourue
            double distToSite = Instance.getDistMatrix(lastSite, bestSite);
            day.dailyDistance += distToSite;

            // Ajout du site
            day.addChosenSite(bestSite);
            schedule.siteAlreadyVisited.add(bestSite);
            day.dailyScore += bestSite.score;

            // Logs pour débogage
            System.out.println("Ajout du site " + bestSite.id + " avec une distance de " + distToSite);
            System.out.println("Nouvelle distance totale : " + day.dailyDistance);
            System.out.println("Nouveau score journalier : " + day.dailyScore);
        }
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
            weights[methodIndex] *= (1 + reactionFactor);
        } else {
            weights[methodIndex] *= (1 - reactionFactor);
        }
    }



}
