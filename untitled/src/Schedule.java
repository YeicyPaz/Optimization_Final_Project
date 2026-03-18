import java.util.*;
public class Schedule {

    ArrayList<Day> travelDays = new ArrayList<>(); //les D jour du voyage
    int daysNb = Instance.d;            //nombre de jours (D)
    public Set<TouristicSite> siteAlreadyVisited;
    public List<List<Hotel>> validHotelPaths = new ArrayList<>(); // combinations with fixed start and end. TOUTES les sequence valides des hotels

    // Pour la recherche locale
    private double bestGlobalScore = -1.0;
    public Schedule bestGlobalSchedule = null;

    public Schedule() {
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
            System.out.println("---------------------------------");
            System.out.println("------JOUR n°" + jour + "-----");
            Hotel dep = hotelPath.get(jour);
            System.out.println("-----Hotel de départ------" + dep.id);
            Hotel arr = hotelPath.get(jour + 1);
            System.out.println("-----Hotel d'arrivée------" + arr.id);
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
        System.out.println("-----------------------------");
        System.out.println("Day number: " + dayNb);

        while (true) {
            TouristicSite best = null;
            double bestSiteRatio = -1.0; //score/distance
            double bestDetour = 0.0; //détour pour visiter le site
            double bestDepartureTime = 0.0;

            for (TouristicSite site : Instance.touristicSites
            ) {
                if (siteAlreadyVisited.contains(site)) {
                    continue; // on ne visite pas un site déjà visité
                }
                // détour(site) = dist(current → site) + dist(site → hotelArrivee)
                //               - dist(current → hotelArrivee)  // on enlève la distance de l'ancien chemin qui aurait été parcouru si on n'avait pas visité le site

                double distToSite = Instance.getDistMatrix(current, site);
                double distToHotel = Instance.getDistMatrix(site, hotelArr);
                double distDirect = Instance.getDistMatrix(current, hotelArr);
                double detourDistance = distToSite + distToHotel - distDirect;

                double arrivalTime = distanceTraveled + distToSite;

                //verification du budget distance
                //si le détour est trop grand on ignore le site
                if (arrivalTime > site.closingTime) continue; // Arrivée après fermeture impossible
                double waitingTime = Math.max(0, site.openingTime - arrivalTime); //prend un temps d'attente si existe sinon 0
                double departureTime = arrivalTime + waitingTime; //soit on part dès qu'on est arrivé soit dès que cela ouvre
                double realCost = detourDistance + waitingTime;

                if (distanceTraveled + realCost > distanceLeft) continue;

                double ratio = site.score / realCost; //score/détour

                if (ratio > bestSiteRatio) {
                    bestSiteRatio = ratio;
                    best = site;
                    bestDetour = realCost;
                    bestDepartureTime = departureTime;
                    //System.out.println("the best site's ratio is: "+bestSiteRatio);

                }
            }
            if (best == null) {
                break; // pas de site à visiter, on termine le jour
            }
            day.addChosenSite(best);
            System.out.println("the best site is: " + best.id);
            siteAlreadyVisited.add(best);
            distanceTraveled += bestDetour;
            distanceLeft -= bestDetour;
            counterScore += best.score;
            current = best; // on se déplace vers le site visité
            System.out.println("-------------------------------");
            System.out.println("score " + counterScore);
            System.out.println("Site " + best.id + " added to the day " + dayNb);
            System.out.println("Distance traveled of: " + distanceTraveled);
        }
        day.setDailyDistance(distanceTraveled);
        day.setDailyScore(counterScore);
        System.out.println("-------------------------------------");
        System.out.println("=========End of the day: " + dayNb +"===========");
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

        for (int i = 0; i < Instance.hotels.size(); i++) {
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

    public double totalScore(ArrayList<Day> days) {
        double count = 0.0;
        for (Day day : days
        ) {
            count += day.dailyScore;
        }
        return count;
    }

    //------------------LOCAL SEARCH----------------------
    public Schedule applyLocalSearch(Schedule currentSchedule, List<Hotel> currentHotelPath){
        boolean improved= true;

        //on repete pendant qu'on continue a trouver des ameliorations
        while(improved){
            improved=false;

            //on regarde jour par jour
            for (int dayNb =0; dayNb < currentSchedule.daysNb; dayNb++){
                Day day = currentSchedule.travelDays.get(dayNb);
                Hotel hotelDep= currentHotelPath.get(dayNb);
                Hotel hotelArr= currentHotelPath.get(dayNb + 1);

                //on met ensemble tous les sites qu'on n'as pas encore visité
                List<TouristicSite> unvisited = new ArrayList<>();
                for (TouristicSite s: Instance.touristicSites){
                    if (!currentSchedule.siteAlreadyVisited.contains(s)){
                        unvisited.add(s);
                    }
                }
                //on essaie d'inserer ces sites dans le jour actuel
                for (TouristicSite candidateSite: unvisited){

                    boolean insertedInThisday = false;
                    int bestPosition = -1;
                    double bestDistanceCost = Double.MAX_VALUE;
                    double bestNewScore = -1.0;

                    //avec le bucle for: on a essayé d’insérer le candidat dans TOUTES les positions possibles de la journée
                    for (int pos=0; pos<= day.chosenSites.size(); pos++){
                        //creation d'un list temporel pour tester l'insertion
                        List<TouristicSite> testSequence = new ArrayList<>();
                        for (Object obj : day.chosenSites){
                            testSequence.add((TouristicSite) obj);
                        }

                        //on insert le site dans la position "pos" pas que dans la fin de la liste
                        testSequence.add(pos, candidateSite);

                        //on evalue si ce nouvelle route est valid avec le metode evaluateSequence
                        double[] result = currentSchedule.evaluateSequence(dayNb, testSequence, hotelDep, hotelArr);

                        if (result[0]==1.0){
                            //si ce valide, on verifie si c'est la distance la moins cher pour lui ajouter
                            if (result[1]< bestDistanceCost){
                                bestDistanceCost=result[1];
                                bestNewScore=result[2];
                                bestPosition = pos;
                                insertedInThisday= true;
                            }
                        }
                    }

                    //si on a trouvé au moins une solution valide pour ce candidate
                    if (insertedInThisday){
                        //la route est valide est entre dans le budget journalier et les horaires
                        //et on aplique les changement au jour
                        day.chosenSites.add(bestPosition, candidateSite);
                        day.setDailyDistance(bestDistanceCost);
                        day.setDailyScore(bestNewScore);

                        //on marque le site comme visité
                        currentSchedule.siteAlreadyVisited.add(candidateSite);

                        //on indique qu'il y a eu une amelioration pour que le bucle while returne à s'executer
                        improved=true;
                        break; // on sorte du bucle for pour reinitialice la recherche avec la nouvelle information(la route a changé)
                    }
                }
                if (improved) break; //si on trouve une amelioration pour ce jour-la, on redemarer la recherche general


            }
        }
        return currentSchedule;
    }


    //--------------VNS SOLUTION------------------------
    //on fait la clonation deu schedule pour eviter de modifier la list original et que le resultat soit pire
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
        clonedSchedule.validHotelPaths = this.validHotelPaths;

        return clonedSchedule;
    }

    //----------------------------------------------------------------
    //-----------------------------EXECUTION RUN VNS------------------
    public void executeVNS(Schedule initialSchedule, List<Hotel> currentHotelPath) {
        long startTime = System.currentTimeMillis();
        long maxDuration = 120 * 1000; //120 seconds in miliseconds

        //on utilise la solution greedy
        Schedule bestSchedule = initialSchedule.cloneSchedule();
        this.bestGlobalScore = totalScore(bestSchedule.travelDays);
        int k_max = 3; //on va utiliser 3 neighbohood differents

        System.out.println("-----------------------------------------------");
        System.out.println("Starting VNS with base SCORE = " + this.bestGlobalScore);

        //creation d'un bucle de temps pour l'impresion  de resultat(1 second=1000 milisecond)
        while ((System.currentTimeMillis() - startTime) < (maxDuration - 1000)) {
            int k=1; //premier shake de neighborhood

            while (k <= k_max && (System.currentTimeMillis() - startTime) < (maxDuration - 1000)) {

                //SHAKING
                //on clone la meilleure solution actuelle (celle qu'on a trouvée avec greedy) pour éviter de l'endommager et on secoue
                Schedule shakenSchedule = bestSchedule.cloneSchedule();
                shakenSchedule.shake(k, currentHotelPath); //revisar

                //LOCAL SEARCH
                //klau methode pour prendre le shakenSchedule et l'ameliorer
                Schedule localOptimum = shakenSchedule.applyLocalSearch(shakenSchedule,currentHotelPath); // REEMPLAZAR LUEGO POR: shakenSchedule.applyLocalSearch()

                //mise à jour
                double newScore = totalScore(localOptimum.travelDays);

                if (newScore > this.bestGlobalScore) {
                    //on a trouvé une amelioration
                    bestSchedule = localOptimum;
                    this.bestGlobalScore = newScore;
                    k = 1;// vu qu'on a amelioré, on essaie à noveau avec une perturbation plus petite
                } else {
                    k++; //on n'a pas amelioré, alors on fait une soute plus grand
                }
            }
        }
        this.bestGlobalSchedule = bestSchedule;
//        System.out.println("the best GLOBAL SCORE after VNS= " + bestGlobalScore);
    }

    //---------------------------------------------------------------------------------

    //----------------------METODE EVALUATE ROUTES----------------------------
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

    //----------------------------METODE SHAKE----------------------------------
    public void shake(int k, List<Hotel> currentHotelPath) {
        Random rand = new Random();

        if (k == 1) {
            //le premier type de neighborhood |(Swap) N1=échange d'un site avec une autre qu'on a jamais tester mais il faut varifie que le nouveau site respect les regles(avec metode evaluate)

            //election d'un jour aleatoire
            int dayIndex = rand.nextInt(this.daysNb);
            Day randomDay = this.travelDays.get(dayIndex);

            if (randomDay.chosenSites.isEmpty())
                return; //si le jour est vide , il n'y a rien à enlever et on fini le shake

            //on choisi un site aleatoire pour enlever
            int siteToRemoveIdx = rand.nextInt(randomDay.chosenSites.size());
            TouristicSite removedSite = (TouristicSite) randomDay.chosenSites.get(siteToRemoveIdx);

            //on cree une nouvelle sequence de test temporaire sans ce site
            List<TouristicSite> testSequence = new ArrayList<>();
            for (Object obj : randomDay.chosenSites) {
                testSequence.add((TouristicSite) obj);
            }
            testSequence.remove(siteToRemoveIdx);

            // on cherche des sites que ne sont pas encore visité
            List<TouristicSite> unvisited = new ArrayList<>();
            for (TouristicSite s : Instance.touristicSites) {
                if (!this.siteAlreadyVisited.contains(s)) {
                    unvisited.add(s);
                }
            }
            //on essaie d'insérer un site nouvelle aleatoirement
            if (!unvisited.isEmpty()) {
                TouristicSite newSite = unvisited.get(rand.nextInt(unvisited.size()));
                testSequence.add(siteToRemoveIdx, newSite);//on l'ajoute dans la place qui a rester apres avoir enlever un site aleatoirement

                //on evalue si la nouvelle posibilité est valide
                Hotel hotelDep = currentHotelPath.get(dayIndex);
                Hotel hotelArr = currentHotelPath.get(dayIndex + 1);

                double[] result = evaluateSequence(dayIndex, testSequence, hotelDep, hotelArr);
                boolean isValid = (result[0] == 1.0);

                if (isValid) {
                    //c'est valid le changement trouvé donc, on change le jour
                    randomDay.chosenSites.clear();
                    randomDay.chosenSites.addAll(testSequence);

                    this.siteAlreadyVisited.remove(removedSite);
                    this.siteAlreadyVisited.add(newSite);

                    randomDay.setDailyDistance(result[1]);
                    randomDay.setDailyScore(result[2]);
                } else {
                    //on laisse l'espace vide pour que la recherche local remplisse l'espace vide
                    testSequence.remove(newSite); //on enleve le site invalide
                    double[] resultRemoved = evaluateSequence(dayIndex, testSequence, hotelDep, hotelArr);

                    randomDay.chosenSites.clear();
                    randomDay.chosenSites.addAll(testSequence);
                    this.siteAlreadyVisited.remove(removedSite);

                    randomDay.setDailyDistance(resultRemoved[1]);
                    randomDay.setDailyScore(resultRemoved[2]);
                }
            }
        }
        else if(k==2){
            //voisin 2
        } else if(k==3){
            //voisin 3
        }
    }
}








