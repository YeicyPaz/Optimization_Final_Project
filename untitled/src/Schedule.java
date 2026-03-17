import java.util.*;
public class Schedule {

    ArrayList<Day> travelDays = new ArrayList<>(); //les D jour du voyage
    int daysNb = Instance.d;            //nombre de jours (D)
    public Set<TouristicSite> siteAlreadyVisited;
    public List<List<Hotel>> validHotelPaths = new ArrayList<>(); // combinations with fixed start and end. TOUTES les sequence valides des hotels

    // Pour la recherche locale
    private double bestGlobalScore = -1.0;
    private Schedule bestGlobalSchedule = null;
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

        System.out.println("day number "+ dayNb);

        while (true){
            TouristicSite best = null;
            double bestSiteRatio = -1.0; //score/distance
            double bestDetour = 0.0; //détour pour visiter le site
            double bestDepartureTime = 0.0;

            for (TouristicSite site : Instance.touristicSites
                 ) {
                if (siteAlreadyVisited.contains(site)){
                    continue; // on ne visite pas un site déjà visité
                }
                // détour(site) = dist(current → site) + dist(site → hotelArrivee)
                //               - dist(current → hotelArrivee)  // on enlève la distance de l'ancien chemin qui aurait été parcouru si on n'avait pas visité le site

                double distToSite = Instance.getDistMatrix(current, site);
                double distToHotel = Instance.getDistMatrix(site, hotelArr);
                double distDirect = Instance.getDistMatrix(current, hotelArr);
                double detourDistance = distToSite + distToHotel - distDirect;

                double arrivalTime   = distanceTraveled + distToSite;

                //verification du budget distance
                //si le détour est trop grand on ignore le site
                if (arrivalTime > site.closingTime) continue; // Arrivée après fermeture impossible
                double waitingTime   = Math.max(0, site.openingTime - arrivalTime); //prend un temps d'attente si existe sinon 0
                double departureTime = arrivalTime + waitingTime; //soit on part dès qu'on est arrivé soit dès que cela ouvre
                double realCost = detourDistance + waitingTime;

                if(distanceTraveled + realCost > distanceLeft) continue;

                double ratio = site.score / realCost; //score/détour

                if(ratio > bestSiteRatio){
                    bestSiteRatio = ratio;
                    best = site;
                    bestDetour = realCost;
                    bestDepartureTime = departureTime;
                    System.out.println("the best site's ratio is: "+bestSiteRatio);
                    System.out.println("the site id "+ best.id);

                }
            }
            if(best == null){
                break; // pas de site à visiter, on termine le jour
            }
            day.addChosenSite(best);
            System.out.println("the best site is --------------> "+best.id);
            siteAlreadyVisited.add(best);
            distanceTraveled += bestDetour;
            distanceLeft -= bestDetour;
            counterScore += best.score;
            current = best; // on se déplace vers le site visité
            System.out.println("score counter: "+ counterScore);
            System.out.println("Site "+best.id+" added to the day "+dayNb);
            System.out.println("Distance traveled of "+ distanceTraveled);
        }
        day.setDailyDistance(distanceTraveled);
        day.setDailyScore(counterScore);
        System.out.println("End of the day "+ dayNb);
    }

    //heuristique pour les hotels
    public void chooseHotelPath(){
        List<Hotel> currentPath = new ArrayList<>();
        Hotel firstHotel = Instance.getFirstHotel();
        Hotel lastHotel = Instance.getLastHotel();
        List<Score> hotelsScores = new ArrayList<>();

        //on calcule les concentrations des sites au alentours de chaque hotel
        double perimeter = 10.0; //à changer à moyenne des distances journalière /2?
        for (Hotel hotel: Instance.hotels
             ) {
            //on regarde tous les sites et on compte ceux qui sont à une distance inférieure au périmètre
            double nearbySitesCount = 0.0;

            double nearbySitesScore = 0.0; //on compte aussi le score pour voir départager les meilleures concentrations
            double nearbySitesHoursTime = 0.0;// compter aussi le temps d'ouverture ? ex: le temps d'ouverture le plus grand

            for (TouristicSite site: Instance.touristicSites
                 ) {
                double distToSite = Instance.getDistMatrix(hotel, site);
                if (distToSite <= perimeter) {
                    nearbySitesCount += 1.0;
                    nearbySitesScore += site.score;
                    nearbySitesHoursTime += (site.closingTime - site.openingTime); // le temps d'ouverture total dans la journée
                }
            }
            double distanceFromSegment = calculateDistanceFromSegment(hotel, firstHotel, lastHotel);

            // Ajout du score de l'hôtel
            hotelsScores.add(new Score(hotel, nearbySitesCount, nearbySitesScore, nearbySitesHoursTime, distanceFromSegment));

            // Affichage des scores (pour débogage)
            for (Score s : hotelsScores) {
                System.out.println(s.site.id + ": nearbySitesCount=" + s.score1 +
                        ", nearbySitesScore=" + s.score2 +
                        ", distanceFromSegment=" + s.score4);
            }

            // Sélection des hôtels pour le chemin
            currentPath.add(firstHotel);
            for (int day = 1; day < daysNb; day++) {
                // Choisir le meilleur hôtel pour le jour `day` (en excluant le dernier hôtel)
                Hotel nextHotel = (Hotel) selectBestHotel(hotelsScores, currentPath.get(day - 1), day);
                currentPath.add(nextHotel);
            }
            currentPath.add(lastHotel);

            // Affichage du chemin final
            System.out.println("Chemin d'hôtels sélectionné : " + currentPath);
        }

    }
    private Site selectBestHotel(List<Score> hotelScores, Hotel currentHotel, int day) {
        List<Score> candidates = new ArrayList<>();
        for (Score score : hotelScores) {
            // Vérifier que la distance entre l'hôtel actuel et le candidat est réalisable
            double dist = Instance.getDistMatrix(currentHotel, score.site);
            if (dist <= Instance.tD[day]) {
                candidates.add(score);
            }
        }

        // Retourner le meilleur candidat selon l'heuristique
        if (candidates.isEmpty()) {
            return Instance.getLastHotel();  // Retour à l'hôtel final si aucun candidat valide
        }
        Collections.sort(candidates, Comparator.comparingDouble(this::calculateHeuristicScore).reversed());
        return candidates.get(0).site;
    }


    private double calculateHeuristicScore(Score score) {
        // Poids pour chaque critère, à changer ??????
        double weightCount = 0.3;
        double weightScore = 0.5;
        double weightHours = 0.1;
        double weightDistance = 0.1;

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


    private double calculateDistanceFromSegment(Hotel hotel, Hotel start, Hotel end) {
        // Coordonnées des hôtels (à adapter selon ta structure de données)
        double x = hotel.getCoordinates().x, y = hotel.getCoordinates().y;
        double x1 = start.getCoordinates().x, y1 = start.getCoordinates().y;
        double x2 = end.getCoordinates().x, y2 = end.getCoordinates().y;

        // Calcul de la distance entre le point (x, y) et le segment [(x1, y1), (x2, y2)]
        double segmentLength = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        if (segmentLength == 0) {
            return Math.sqrt(Math.pow(x - x1, 2) + Math.pow(y - y1, 2));
        }

        double t = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / (segmentLength * segmentLength);
        t = Math.max(0, Math.min(1, t));

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


}





