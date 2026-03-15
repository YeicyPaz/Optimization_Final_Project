import java.util.*;
public class Schedule {

    ArrayList<Day> schedule = new ArrayList<>(); //les D jour du voyage
    int daysNb = Instance.d;            //nombre de jours (D)
    public Set<TouristicSite> siteAlreadyVisited;
    public List<List<Hotel>> validHotelPaths = new ArrayList<>(); // combinations with fixed start and end. TOUTES les sequence valides des hotels

    // Pour la recherche locale
    private double bestGlobalScore = -1.0;
    private Schedule bestGlobalSchedule = null;
    public Schedule(){
        for (int d = 0; d < daysNb; d++) {
            schedule.add(new Day(d)); // Initialisation of nbDays days in the schedule
        }
        siteAlreadyVisited = new HashSet<>();
    }

    public void buildGreedySolution(List<Hotel> hotelPath) {
        // hotelPath = [H0, H2, H3, H5] pour 3 jours par exemple

        for (Day d : schedule) {
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
        Day day = schedule.get(dayNb);
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

                }
            }
            if(best == null){
                break; // pas de site à visiter, on termine le jour
            }
            day.addChosenSite(best);
            System.out.println("the best site is: "+best.id);
            siteAlreadyVisited.add(best);
            distanceTraveled += bestDetour;
            distanceLeft -= bestDetour;
            counterScore += best.score;
            current = best; // on se déplace vers le site visité
            System.out.println("score "+ counterScore);
            System.out.println("Site "+best.id+" added to the day "+dayNb);
            System.out.println("Distance traveled of "+ distanceTraveled);
        }
        day.setDailyDistance(distanceTraveled);
        day.setDailyScore(counterScore);
        System.out.println("End of the day "+ dayNb);
    }


        //Explorer en profondeur (DFS) toutes les combinaisons d'hôtels intermédiaires, et pruning des chemin quui dépasse le budget distance d'un jour.
    /*
    faire récursivement
    regarder tout les chemins possibles d'hotels (prenant en compte seulement les hotels)
    dans un arbre on construit les différentes possibilités
    en effacant celle qui épuise la distance de parcours avant d'arriver à l'hotel final
    matrices avec les différents chemins d'hotels (qui respecte la distance)
     */


    // Point d'entrée : lance la récursion
    public void generateHotelPaths() {
        List<Hotel> currentPath = new ArrayList<>();
        currentPath.add(Instance.getFirstHotel());

        exploreHotels(currentPath, Instance.getFirstHotel(), 0, Instance.getLastHotel());
    }

    /*
    currentPath  le chemin en cours de construction
    currentHotel l'hôtel où on se trouve au début du jour `day`
    day          le jour actuel (0-indexed)
    hotelEnd     l'hôtel final fixe
     */
    private void exploreHotels(List<Hotel> currentPath, Hotel currentHotel, int day, Hotel hotelEnd) { //algoritme backtracking recursive pour explorer tout les combinaisons

        // Cas de base : dernier jour → on doit aller directement à l'hôtel final
        if (day == daysNb - 1) {
            double distToEnd = Instance.getDistMatrix(currentHotel, hotelEnd);
            if (distToEnd <= Instance.tD[day]) {
                List<Hotel> completePath = new ArrayList<>(currentPath);
                completePath.add(hotelEnd);
                validHotelPaths.add(completePath);
            }
            return; // On remonte dans tous les cas
        }
        // Cas récursif : on choisit un hôtel intermédiaire pour finir le jour `day`
        // Les hôtels intermédiaires sont indices 1 à hotels.size()-2

        /*
        Si l'on exclut les hotels premier et derniers des hotels intermédiaires:
        // Les hôtels intermédiaires sont indices 1 à hotels.size()-2
        for (int i = 1; i < Instance.hotels.size() - 1; i++) {
         */

        for (int i = 0; i < Instance.hotels.size() ; i++) {
            Hotel nextHotel = Instance.hotels.get(i);

            double distToNext = Instance.getDistMatrix(currentHotel, nextHotel);

            // Élagage : si la distance dépasse le budget du jour, on abandonne ce chemin
            if (distToNext > Instance.tD[day]) {
                continue;
            }

            // On ajoute cet hôtel au chemin et on recurse sur le jour suivant
            currentPath.add(nextHotel);
            exploreHotels(currentPath, nextHotel, day + 1, hotelEnd);
            currentPath.remove(currentPath.size() - 1); // backtrack
        }
    }

    public double totalScore(ArrayList<Day> days){
        double count = 0.0;
        for (Day day: days
             ) {
            count += day.dailyScore;
        }return count;
    }


}





