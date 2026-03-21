import java.util.*;

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

    private void destroyRandomSite(Day day, Schedule schedule) {
        System.out.println("Destruction aléatoire pour le jour " + day.index);
        Random random = new Random();
        if (!day.chosenSites.isEmpty()) {
            int indexToRemove = random.nextInt(day.chosenSites.size());
            System.out.println("Nombre de sites avant suppression : " + day.chosenSites.size());
            System.out.println("Index du site retiré : " + indexToRemove);
            TouristicSite removedSite = day.chosenSites.get(indexToRemove);
            System.out.println("Destruction aléatoire : site " + removedSite.id + " retiré du jour " + day.index);
            // Mise à jour de la distance et du score du jour
            System.out.println("Distance avant suppression : " + day.dailyDistance);
            //on supprime la distance du site retiré -> on enlève la distance entre le removedSite et le site juste avant lui et le site juste après lui
            Site prevSite = null;
            Site nextSite = null;

            if(indexToRemove-1 >= 0 ){
                System.out.println("Index du site précédent : " + (indexToRemove - 1));
                prevSite = day.chosenSites.get(indexToRemove-1);
            }else{
                prevSite = schedule.bestHotelPath.get(day.index);
            }
            if(indexToRemove+1 <= day.chosenSites.size()-1) {
                System.out.println("Index du site suivant : " + (indexToRemove+1));
                nextSite = day.chosenSites.get(indexToRemove+1);
            }else{
                nextSite = schedule.bestHotelPath.get(day.index+1);
            }
            double distToPrev = Instance.getDistMatrix(prevSite, removedSite);
            double distToNext = Instance.getDistMatrix(removedSite, nextSite);
            day.dailyDistance = day.dailyDistance - (distToPrev + distToNext) + Instance.getDistMatrix(prevSite, nextSite);
            System.out.println("Distance après suppression : " + day.dailyDistance);
            day.dailyScore -= removedSite.score;
            day.chosenSites.remove(indexToRemove);
            schedule.siteAlreadyVisited.remove(removedSite);
        }
    }
    private void destroyWorstRatioSites(ArrayList<Day> days, Schedule schedule) {
        System.out.println("Destruction basée sur le pire ratio score/distance");
        for (Day day : days) {
            if (day.chosenSites.isEmpty()) {
                continue;
            }

            // Trouver le site avec le pire ratio score/distance
            TouristicSite worstSite = null;
            double worstRatio = Double.MAX_VALUE;
            double worstSiteDistance = 0.0;
            double worstSiteDirectDist = 0.0;

            for (int i = 0; i < day.chosenSites.size()-1 ; i++) {
                Site prevSite = null;
                Site nextSite = null;
                if(i-1 >= 0 ){ // si le site est le premier de la liste, on prend l'hôtel de départ
                    prevSite = day.chosenSites.get(i-1);
                }else{
                    prevSite = schedule.bestHotelPath.get(day.index);
                }
                if(i+1 <= day.chosenSites.size()-1) { //si le site n'est pas le dernier de la liste, on prend le site suivant, sinon on prend l'hôtel d'arrivée
                    nextSite = day.chosenSites.get(i+1);
                }else{
                    nextSite = schedule.bestHotelPath.get(day.index+1);
                }
                double distToPrev = Instance.getDistMatrix(prevSite, day.chosenSites.get(i));
                double distToNext = Instance.getDistMatrix(day.chosenSites.get(i), nextSite);
                double detourDistance = distToPrev + distToNext;
                double ratio = day.chosenSites.get(i).score / detourDistance;
                if (ratio < worstRatio) {
                    worstRatio = ratio;
                    worstSite = day.chosenSites.get(i);
                    worstSiteDistance = detourDistance;
                    worstSiteDirectDist = Instance.getDistMatrix(prevSite, nextSite);

                }
            }

            if (worstSite != null) {
                System.out.println("Distance avnat suppression : " + day.dailyDistance);

                day.dailyDistance = day.dailyDistance - worstSiteDistance + worstSiteDirectDist;
                System.out.println("Distance après suppression : " + day.dailyDistance);
                day.dailyScore -= worstSite.score;
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
        TabuList tabuList = new TabuList(10); // Taille de la liste tabou


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
                        repairWithBestScore(schedule,day);
                    }
                    break;
                case 1:

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


    private void repairWithRegret(Schedule schedule, Day day, Hotel hotelDep, Hotel hotelArr) {
        int dayNb = day.index;
        Site lastSite = day.getLastSite();

        // calcul du regret pour chaque site à la fin de la journée (en partant de l'hôtel d'arrivée)

        if (lastSite == null) {
            lastSite = hotelDep;
        }

        // Calculer le regret pour chaque site non visité
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
        // Calculer le regret pour chaque site candidat
        double maxRegret = -1.0;
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

            // Ajout du site
            day.addChosenSite(bestSite);
            schedule.siteAlreadyVisited.add(bestSite);
            day.dailyScore += bestSite.score;

            // Logs pour débogage
            System.out.println("Ajout du site " + bestSite.id + " avec un regret de " + maxRegret);
            System.out.println("Nouvelle distance totale : " + day.dailyDistance);
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
        double detourDistance = distToSite + distToHotel - distDirect;

        double arrivalTime = day.dailyDistance + distToSite;
        if (arrivalTime > site.closingTime) {
            return -1; // Si le site n'est pas accessible, le regret est négatif
        }

        double waitingTime = Math.max(0, site.openingTime - arrivalTime);
        double realCost = detourDistance + waitingTime;

        if (day.dailyDistance + distToSite > Instance.tD[day.index]) {
            return -1; // Si le site dépasse le budget distance, le regret est négatif
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

        // Le regret est la différence entre le meilleur score et le deuxième meilleur score
        return siteScore - secondBestScore;
    }

    private ArrayList<ArrayList<Day>> generateNeighbors(Schedule schedule, ArrayList<Day> days) {
        ArrayList<ArrayList<Day>> neighbors = new ArrayList<>();
        System.out.println("Génération des voisins...");

        // Exemple : Échanger des sites entre deux jours
        for (int i = 0; i < days.size(); i++) {
            for (int j = i + 1; j < days.size(); j++) {
                if (!days.get(i).chosenSites.isEmpty() && !days.get(j).chosenSites.isEmpty()) {
                    ArrayList<Day> neighbor = cloneDays(days);

                    // Échanger un site entre les jours i et j
                    TouristicSite siteI = neighbor.get(i).chosenSites.get(0);
                    TouristicSite siteJ = neighbor.get(j).chosenSites.get(0);

                    neighbor.get(i).chosenSites.set(0, siteJ);
                    neighbor.get(j).chosenSites.set(0, siteI);

                    neighbors.add(neighbor);
                    System.out.println("Voisin généré: Échange entre jour " + i + " et jour " + j + ": " + siteI.id + " ↔ " + siteJ.id);
                }
            }
        }

        System.out.println("Nombre total de voisins générés: " + neighbors.size());
        return neighbors;
    }

    private String generateMoveDescription(ArrayList<Day> currentDays, ArrayList<Day> neighborDays) {
        StringBuilder description = new StringBuilder();

        for (int dayNb = 0; dayNb < currentDays.size(); dayNb++) {
            Day currentDay = currentDays.get(dayNb);
            Day neighborDay = neighborDays.get(dayNb);

            if (!currentDay.chosenSites.equals(neighborDay.chosenSites)) {
                description.append("Day ").append(dayNb).append(": ");
                description.append(currentDay.chosenSites).append(" -> ").append(neighborDay.chosenSites).append("; ");
            }
        }

        return description.toString();
    }



    private void repairWithBestScore(Schedule schedule, Day day) {
        //reparation du jour en choisissant le site avec le meilleur score si possible
        double bestRatio = -1.0;
        TouristicSite bestSite = null;
        int dayNb = day.index;
        System.out.println("Réparation du jour " + dayNb );

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
        for (Score score : scores
             ) {
            System.out.println("Site " + score.site.id + " avec un score de " + score.score1);
        }

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
            weights[methodIndex] *= (1 + reactionFactor);
        } else {
            weights[methodIndex] *= (1 - reactionFactor);
        }
    }



    private void selectBestPosition(List<Score> sitesScore, Day day) {
        int bestInsertionIndex = -1;
        //List<Score> candidates = new ArrayList<>();
        System.out.println("Best position : Placement des sites pour le jour " + day.index);
        String idBefore = "";
        String idAfter = "";
        //on essaye de placer les hotels avec la même heuristique, et on choisit le suivant si la distance le permet
        for (int i = 5; i < sitesScore.size() ; i++) {
            //if(day.index >= Instance.d-1) break; // on a déjà choisi les hotels pour tous les jours
            TouristicSite candidate = (TouristicSite) sitesScore.get(i).site;

            if(day.chosenSites.size() > 0){ // on cherche à insérer le prochain candidat au meilleur endroit possible dans le chemin déjà construit

                double bestInsertionDetour = Double.MAX_VALUE;
                double distanceDirect = 0.0;
                for(int j = 0;  j<day.chosenSites.size(); j++){ //on prends le detour le plus petit pour insérer le candidat
                    if(schedule.siteAlreadyVisited.contains(candidate) || day.chosenSites.contains(candidate)) {
                        continue; // Éviter les doublons
                    }
                    if(candidate == null) {
                        continue;
                    }
                    double distToSite = 0.0;
                    double distToHotel = 0.0;
                    double detourDistance = 0.0;

                    if(day.chosenSites.size()<j+1){//si on veut le placer entre le dernier site et l'hôtel d'arrivée du jour
                        idBefore = day.chosenSites.get(j).id;
                        idAfter = day.chosenHotel.id;
                        System.out.println("test placement entre "+ day.chosenSites.get(j).id + " et " + schedule.bestHotelPath.get(day.index+1).id);
                        System.out.println(schedule.bestHotelPath.get(day.index+1)+" == ? "+ day.chosenHotel);
                        distToSite = Instance.getDistMatrix(day.chosenSites.get(j), candidate);
                        distToHotel = Instance.getDistMatrix(candidate, day.chosenHotel);
                        detourDistance = distToSite + distToHotel ;
                    } else if (j == 0) {//si on veut le placer entre le premier site et l'hôtel de départ du jour
                        idBefore = schedule.bestHotelPath.get(day.index).id;
                        idAfter = day.chosenSites.get(j).id;
                        System.out.println("test placement entre "+ schedule.bestHotelPath.get(day.index).id + " et " + day.chosenSites.get(j).id);
                        distToSite = Instance.getDistMatrix(schedule.bestHotelPath.get(day.index), candidate);
                        distToHotel = Instance.getDistMatrix(candidate, day.chosenSites.get(j));
                        detourDistance = distToSite + distToHotel ;
                    }else{ // entre deux sites
                        idBefore = day.chosenSites.get(j-1).id;
                        idAfter = day.chosenSites.get(j).id;
                        System.out.println("test placement entre "+ day.chosenSites.get(j-1).id + " et " + day.chosenSites.get(j).id);
                        distToSite = Instance.getDistMatrix(day.chosenSites.get(j-1), candidate);
                        distToHotel = Instance.getDistMatrix(candidate, day.chosenSites.get(j));
                        detourDistance = distToSite + distToHotel ;
                    }

                    if(day.chosenSites.size()<j+1){
                        distanceDirect = Instance.getDistMatrix(day.chosenSites.get(j), day.chosenHotel);
                    } else if (j == 0) {
                        distanceDirect = Instance.getDistMatrix(schedule.bestHotelPath.get(day.index), day.chosenSites.get(j));
                    }else{
                        distanceDirect = Instance.getDistMatrix(day.chosenSites.get(j-1), day.chosenSites.get(j));
                    }
                    System.out.println("le detour pour insérer le site "+ candidate.id + " entre le site "+ idBefore + " et le site "+ idAfter+ " est de "+ detourDistance + " avec une distance vers le candidat de "+ distToSite + " et une distance vers le site/hotel de "+ distToHotel + " et une distance directe de "+ distanceDirect);
                    bestInsertionDetour = detourDistance;
                    System.out.println("distance directe" + distanceDirect);
                    if(day.dailyDistance + detourDistance -distanceDirect > Instance.tD[day.index] ){
                        // si le detour est trop grand on ne l'insère pas
                        System.out.println("the candidate "+ candidate.id + " is too far to be inserted before "+ idBefore + " aand after "+ idAfter);
                        continue;
                    }
                    System.out.println("------------------------the best insertion index for the candidate "+ candidate.id + " is: "+ j + " with a detour of "+ bestInsertionDetour);
                    System.out.println("the index indicates that the candidate "+ candidate.id + " will be inserted between "+ idBefore + " and the hotel of day "+ idAfter);

                    day.chosenSites.add(j, candidate);
                    day.dailyDistance = day.dailyDistance + bestInsertionDetour - distanceDirect;
                    day.dailyScore += candidate.score;
                    schedule.siteAlreadyVisited.add(candidate);
                    System.out.println("daily distance after insertion: "+ day.dailyDistance);
                    return;



                }

            }
        }




    }
}

