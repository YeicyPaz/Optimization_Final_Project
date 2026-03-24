import java.util.*;
public class Schedule {

    ArrayList<Day> travelDays = new ArrayList<>(); //les D jour du voyage
    int daysNb = Instance.d;            //nombre de jours (D)
    public Set<TouristicSite> siteAlreadyVisited;
    public List<Hotel> bestHotelPath = new ArrayList<>(); // best path parmis les hotels choisi avec l'heuristique

    // Pour la recherche locale
    private double bestGlobalScore = -1.0;
    public Schedule bestGlobalSchedule = null;
    public Schedule(){
        for (int d = 0; d < daysNb; d++) {
            travelDays.add(new Day(d)); // Initialisation of nbDays days in the schedule
        }
        siteAlreadyVisited = new HashSet<>();
    }

    public void buildGreedySolution(List<Hotel> hotelPath) {
        // hotelPath = [H0, H2, H3, H5] pour 3 jours par exemple

        for (Day d : travelDays) {
            d.chosenSites.clear();
            d.setDailyDistance(0.0);
            d.setDailyScore(0.0);
        }
        siteAlreadyVisited.clear();

        for (int jour = 0; jour < daysNb; jour++) {
            System.out.println("------JOUR n°"+jour+"-----");
            Hotel dep = hotelPath.get(jour);
            System.out.println("-----Hotel de départ------"+ dep.id);
            Hotel arr = hotelPath.get(jour + 1);
            System.out.println("-----Hotel d'arrivée------"+ arr.id);
            greedyFillDay(jour, dep, arr);

        }
    }

    public void greedyFillDay(int dayNb, Hotel hotelDep, Hotel hotelArr) {
        //On récupère le jour à remplir
        Day day = travelDays.get(dayNb);
        //la 1ere journée commence au 1er hotel
        day.setChosenHotel(hotelDep);
        // initialisation du current comme Site (1er hotel ou dernier site visité)
        Site current = hotelDep;
        double distanceTraveled = 0.0; //distance parcourue
        double distanceLeft = Instance.tD[dayNb]; // distance restante

        double counterScore = 0.0;

        while (true){
            TouristicSite best = null;
            double bestSiteRatio = -1.0; //score/distance
            double bestDetour = 0.0; //détour pour visiter le site
            double bestDepartureTime = 0.0;
            double bestArrivalTime = 0.0;

            double arrivalTime = 0.0;
            for (TouristicSite site : Instance.touristicSites
                 ) {
                if (siteAlreadyVisited.contains(site)){
                    continue; // on ne visite pas un site déjà visité
                }
                // détour(site) = dist(current → site) + dist(site → hotelArrivee)

                double distToSite = Instance.getDistMatrix(current, site);
                double distToHotel = Instance.getDistMatrix(site, hotelArr);
                // on enlève la distance de l'ancien chemin qui aurait été parcouru si on n'avait pas visité le site
                double distDirect = Instance.getDistMatrix(current, hotelArr); // à enlever si on ajoute le site à l'itinéraire
                double detourDistance = distToSite + distToHotel;

                if(distanceTraveled!= 0){
                    arrivalTime   = distanceTraveled - distDirect + distToSite;
                }else {
                    arrivalTime   = distanceTraveled + distToSite;

                }

                //verification du budget distance
                //si le détour est trop grand on ignore le site
                if (arrivalTime > site.closingTime) continue; // Arrivée après fermeture impossible
                double waitingTime   = Math.max(0, site.openingTime - arrivalTime); //prend un temps d'attente si existe sinon 0
                double departureTime = arrivalTime + waitingTime; //soit on part dès qu'on est arrivé soit dès que cela ouvre
                double realCost = detourDistance + waitingTime;

                if(distanceTraveled - distDirect + realCost > distanceLeft) continue;

                double ratio = site.score / realCost; //score/détour

                if(ratio > bestSiteRatio){
                    bestSiteRatio = ratio;
                    best = site;
                    bestDetour = realCost;
                    bestDepartureTime = departureTime;
                    bestArrivalTime = arrivalTime;
                }
            }
            if(best == null){
                break; // pas de site à visiter, on termine le jour
            }
            double distToSite = Instance.getDistMatrix(current, best);
            double distToHotel = Instance.getDistMatrix(best, hotelArr);
            double distDirect = Instance.getDistMatrix(current, hotelArr);
            double detourDistance = distToSite + distToHotel - distDirect;
            day.addChosenSite(best);
            day.departureTimes.add(bestDepartureTime);
            day.arrivalTimes.add(bestArrivalTime);
            System.out.println("the best site is --------------> "+best.id);
            System.out.println("the best site to visit is "+ best.id + " with a score of "+ best.score + " and a detour of "+ bestDetour + " and a departure time of "+ bestDepartureTime);
            System.out.println("the distance to the site is "+ distToSite + " and the distance to the hotel is "+ distToHotel + " and the direct distance is "+ distDirect);
            siteAlreadyVisited.add(best);
            if(distanceTraveled> 0){
                distanceTraveled += bestDetour - distDirect;
            }else{
                distanceTraveled += bestDetour ;
            }

            distanceLeft = distanceLeft + distDirect - bestDetour;
            counterScore += best.score;
            current = best; // on se déplace vers le site visité
            System.out.println("score counter: "+ counterScore);
            System.out.println("Site "+best.id+" added to the day "+dayNb);
            System.out.println("Distance traveled of "+ distanceTraveled);
        }
        day.setDailyDistance(distanceTraveled);
        day.setDailyScore(counterScore);
    }
    //heuristique pour les hotels
    public void chooseHotelPath(){
        Hotel firstHotel = Instance.getFirstHotel();
        Hotel lastHotel = Instance.getLastHotel();
        List<Score> hotelsScores = new ArrayList<>();

        //on calcule les "concentrations" des sites au alentours de chaque hotel
       Double perimeter = (Instance.tMax/Instance.d)/2; //moyenne des distances max par jour / 2 à changer si besoin
                                    // pour avoir un périmètre où l'on peut rentrer à l'hotel après la visite d'un site
        for (Hotel hotel: Instance.hotels
             ) {
            //on regarde tous les sites et on compte ceux qui sont à une distance inférieure au périmètre
            double nearbySitesCount = 0.0;
            double nearbySitesScore = 0.0; //on compte aussi le score pour voir départager les meilleures concentrations
            double nearbySitesHoursTime = 0.0;// compter aussi le temps d'ouverture ? ex: le temps d'ouverture le plus grand

            for (TouristicSite site : Instance.touristicSites
            ) {
                double distToSite = Instance.getDistMatrix(hotel, site);
                if (distToSite <= perimeter) {
                    nearbySitesCount += 1.0;
                    nearbySitesScore += site.score;
                    nearbySitesHoursTime += (site.closingTime - site.openingTime); // le temps d'ouverture total dans la journée
                }
            }
            double distanceFromSegment = calculateDistanceFromSegment(hotel, firstHotel, lastHotel);

            // les différents scores de l'hôtel
            hotelsScores.add(new Score(hotel, nearbySitesCount, nearbySitesScore, nearbySitesHoursTime, distanceFromSegment));
        }
        this.selectBestHotel(hotelsScores, firstHotel, lastHotel);

        // AU CAS OU: Si vous n’avez pas trouvé assez d’hôtels pour couvrir tous les jour:
        // ==========================================
        while (bestHotelPath.size() < Instance.d + 1) {
            // Rellenamos repitiendo el último hotel antes del final
            bestHotelPath.add(bestHotelPath.size() - 1, Instance.getLastHotel());
            System.out.println("Emergency padding: Added " + Instance.getLastHotel().id + " to complete the days.");
        }

    }
    private void selectBestHotel(List<Score> hotelScores, Hotel firstHotel, Hotel lastHotel) {
        List<Score> candidates = new ArrayList<>();
        for (int i = 0; i < hotelScores.size(); i++) {
            double heuristic = this.calculateHeuristicScore(hotelScores.get(i));
            candidates.add(new Score(hotelScores.get(i).site,heuristic));
        }


        //on trie les candidats selon l'heuristique qui combine les différents scores (nombre de sites, score total, distance du segment,et horaires d'ouverture)
        Collections.sort(candidates, (s1, s2) -> Double.compare(s2.score1, s1.score1)); // Tri décroissant selon le score heuristique

        bestHotelPath.add(firstHotel);
        bestHotelPath.add(lastHotel);

        int dayIndex = 0;//the index of the day

        //on essaye de placer les hotels avec la même heuristique, et on choisit le suivant si la distance le permet
        for (int i = 0; i < candidates.size() ; i++) {
            if(dayIndex >= Instance.d-1) break; // on a déjà choisi les hotels pour tous les jours

            if(bestHotelPath.size() > 0){ // on cherche à insérer le prochain candidat au meilleur endroit possible dans le chemin déjà construit
                Hotel candidateHotel = (Hotel) candidates.get(i).site;
                int bestInsertionIndex = -1;
                double bestInsertionDetour = Double.MAX_VALUE;
                for(int j = bestHotelPath.size()-1; j>0; j--){ //on prends le detour le plus petit pour insérer le candidat
                    double distToSite = Instance.getDistMatrix(bestHotelPath.get(j), candidateHotel);
                    double distToHotel = Instance.getDistMatrix(bestHotelPath.get(j-1), candidateHotel);
                    double detourDistance = distToSite + distToHotel ;
                    if(distToSite > Instance.tD[dayIndex] || distToHotel > Instance.tD[dayIndex]){ // si le detour est trop grand on ne l'insère pas
                        //System.out.println("the hotel "+ candidateHotel.id + " is too far from the hotel of day "+ (bestInsertionIndex-1) + " and the hotel of day "+ bestInsertionIndex + " with a detour of "+ bestInsertionDetour);
                        continue;
                    }
                    if(detourDistance< bestInsertionDetour){
                        bestInsertionDetour = detourDistance;
                        bestInsertionIndex = j;
                        //System.out.println("test hotel "+ bestHotelPath.get(j-1).id + " " + bestHotelPath.get(j).id);
                    }
                }
                //System.out.println("the best insertion index for the hotel "+ candidateHotel.id + " is: "+ bestInsertionIndex + " with a detour of "+ bestInsertionDetour);
                //System.out.println("the index indicates that the hotel "+ candidateHotel.id + " will be inserted between the hotel of day "+ (bestInsertionIndex-1) + " and the hotel of day "+ bestInsertionIndex);

                bestHotelPath.add(bestInsertionIndex, candidateHotel);
                dayIndex++;
            }
        }




    }




    private double calculateHeuristicScore(Score score) {
        // Poids pour chaque critère, à changer ??????
        double weightCount = 0.3; //nombre de sites dans le périmètre
        double weightScore = 0.2; //score total des sites dans le périmètre
        double weightHours = 0.1; //horaires d'ouverture des sites dans le périmètre
        double weightDistance = 0.4; // distance de l'hotel par rapport au chemin directe entre l'hotel de départ et d'arrivée

        // Normalisation des valeurs (optionnel, pour équilibrer les critères)
        double normalizedCount = score.score1 / 10.0;  // Exemple : diviser par un facteur pour normaliser
        double normalizedScore = score.score2 / 100.0;
        double normalizedHours = score.score3 / 24.0;
        double normalizedDistance = 1.0 / (1.0 + score.score4);  // Plus la distance est petite, meilleur est le score

        return (weightCount * normalizedCount) +
                (weightScore * normalizedScore) +
                (weightHours * normalizedHours) +
                (weightDistance * normalizedDistance);
    }


    /*
    * Permet de savoir à quelle distance se trouve un hôtel par rapport à la ligne droite idéale entre l’hôtel de départ (start) et l’hôtel d’arrivée (end)
    *  Cela permet d’éviter de choisir des hôtels trop éloignés de ce chemin idéal
     */
    private double calculateDistanceFromSegment(Hotel hotel, Hotel start, Hotel end) {
        // Coordonnées des hôtels (à adapter selon ta structure de données)
        double x = hotel.getCoordinates().x, y = hotel.getCoordinates().y;
        double x1 = start.getCoordinates().x, y1 = start.getCoordinates().y;
        double x2 = end.getCoordinates().x, y2 = end.getCoordinates().y;

        // Calcul de la distance entre le point (x, y) et le segment [(x1, y1), (x2, y2)]
        double segmentLength = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2)); // calculer la longueur du segment de ligne entre les points (x1, y1) et (x2, y2) en utilisant la formule de la distance euclidienne
        if (segmentLength == 0) { //si c'est un point on calcule simplement la distance entre les deux points
            return Math.sqrt(Math.pow(x - x1, 2) + Math.pow(y - y1, 2));
        }

        //produit scalaire
        double t = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / (segmentLength * segmentLength); //position relative de la projection orthogonale du point (x, y) sur le segment
        t = Math.max(0, Math.min(1, t));    // si l'hotel est en dehors du segment on le projette sur le segment le plus proche

        double projectionX = x1 + t * (x2 - x1);
        double projectionY = y1 + t * (y2 - y1);

        return Math.sqrt(Math.pow(x - projectionX, 2) + Math.pow(y - projectionY, 2));
    }


    public double totalScore(ArrayList<Day> days){
        double count = 0.0;
        for (Day day: days
             ) {
            count += day.dailyScore;
        }return count;
    }

 //   on fait la clonation deu schedule pour eviter de modifier la list original et que le resultat soit pire
    public Schedule cloneSchedule() {
        Schedule clonedSchedule = new Schedule();
        clonedSchedule.travelDays.clear(); // on nettoie les jours vides creées pour le constructeur

        // on fait la clonation de chaque jour de manier independant
        for (Day day : this.travelDays) {
            clonedSchedule.travelDays.add(day.cloneDay());
        }

        //on clone la list des sites deja visités
        clonedSchedule.siteAlreadyVisited = new HashSet<>(this.siteAlreadyVisited);
        clonedSchedule.daysNb = this.daysNb;

        //les routes de hotels ne change pas, donc on peut mantenir la reference
        clonedSchedule.bestHotelPath = this.bestHotelPath;

        return clonedSchedule;
    }

 //   ----------------------METODE EVALUATE ROUTES----------------------------
    //Retourn un array: [0] = isValid (1.0 si est valid, 0.0 et non), [1] = time/total distance, [2] = score total
    public double[] evaluateSequence(int dayNb, List<TouristicSite> sequence, Hotel hotelDep, Hotel hotelArr){
        double currentTime= 0.0;
        double scoreTotal = 0.0;
        Site current =hotelDep;

        for (TouristicSite site: sequence){
            double travelTime = Instance.getDistMatrix(current,site);
            double arrivalTime= currentTime + travelTime;

            //if on arrive apres la fermeture, la route est invalide
            if (arrivalTime> site.closingTime){
                return new double[]{0.0 , 0.0 , 0.0};
            }
            // si on arrive tôt, on attend que l'endroit ouvre ses portes
            double waitingTime = Math.max(0, site.openingTime-arrivalTime);
            currentTime = arrivalTime+ waitingTime;

            scoreTotal += site.score;
            current=site;
        }

        //on fait l'addition de voyage à l'hotel final du jour
        double returnTime= Instance.getDistMatrix(current,hotelArr);
        currentTime += returnTime;

        //on verifie qu'on n'a pas superé le budget de distance du jour
        if (currentTime> Instance.tD[dayNb]){
            return  new double[]{0.0, 0.0, 0.0};
        }
        return new double[]{1.0, currentTime,scoreTotal};
    }

}








