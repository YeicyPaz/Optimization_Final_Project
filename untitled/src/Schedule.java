import java.util.*;
public class Schedule {

    ArrayList<Day> travelDays = new ArrayList<>(); //les D jour du voyage
    int daysNb = Instance.d;            //nombre de jours (D)
    public Set<TouristicSite> siteAlreadyVisited;
    public List<Hotel> bestHotelPath = new ArrayList<>(); // best paths parmis les hotels choisi avec l'heuristique

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
            Hotel arr = hotelPath.get(jour+1);
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
        System.out.println("End of the day "+ dayNb);
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
        System.out.println("---------------after sorting candidates for day --------------" );
        for (Score score: candidates
        ) {
            System.out.println(" hotel " + score.site.id + " and his score with the heuristic: " + score.score1);
        }
        bestHotelPath.add(firstHotel);
        System.out.println("the first hotel is added correctly: "+ firstHotel.id);
        bestHotelPath.add(lastHotel);
        System.out.println("the last hotel is added correctly: "+ lastHotel.id);

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
                        System.out.println("the hotel "+ candidateHotel.id + " is too far from the hotel of day "+ (bestInsertionIndex-1) + " and the hotel of day "+ bestInsertionIndex + " with a detour of "+ bestInsertionDetour);
                        continue;
                    }
                    if(detourDistance< bestInsertionDetour){
                        bestInsertionDetour = detourDistance;
                        bestInsertionIndex = j;
                        System.out.println("test hotel "+ bestHotelPath.get(j-1).id + " " + bestHotelPath.get(j).id);
                    }
                }
                System.out.println("---the best insertion index for the hotel "+ candidateHotel.id + " is: "+ bestInsertionIndex + " with a detour of "+ bestInsertionDetour);
                // AU CAS OU: Si vous n’avez pas trouvé assez d’hôtels pour couvrir tous les jour:
                if (bestInsertionIndex == -1) {
                    System.out.println("Hotel " + candidateHotel.id + " skipped - no valid position found because it exceeds daily distance.");
                    continue; // Saltamos este hotel y probamos con el siguiente candidato
                }
                System.out.println("---the index indicates that the hotel "+ candidateHotel.id + " will be inserted between the hotel of day "+ (bestInsertionIndex-1) + " and the hotel of day "+ bestInsertionIndex);

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


    //------------------LOCAL SEARCH----------------------
    public Schedule applyLocalSearch(Schedule currentSchedule, List<Hotel> currentHotelPath){
        for(int dayNb=0; dayNb < currentSchedule.daysNb; dayNb++){
            Hotel hotelDep = currentHotelPath.get(dayNb);
            Hotel hotelArr = currentHotelPath.get(dayNb +1);
            apply2OptForDay(currentSchedule, dayNb, hotelDep, hotelArr);
        }

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
                //fase D'INSERTION
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

                // PHASE de REMPLACEMENT
                // Si rien de nouveau ne se produit, nous essayons d’en trouver un meilleur.
                // ==========================================
                for (int i = 0; i < day.chosenSites.size(); i++) {
                    TouristicSite visitedSite = (TouristicSite) day.chosenSites.get(i);

                    for (TouristicSite candidateSite : unvisited) {
                        // REGLA CLAVE: Solo intentamos el cambio si el candidato da MÁS puntos,
                        // o si da los mismos puntos pero sospechamos que ahorra distancia.
                        if (candidateSite.score < visitedSite.score) continue;

                        List<TouristicSite> testSequence = new ArrayList<>();
                        for (Object obj : day.chosenSites) testSequence.add((TouristicSite) obj);

                        // Hacemos el trueque (Swap) en la misma posición
                        testSequence.set(i, candidateSite);

                        double[] result = currentSchedule.evaluateSequence(dayNb, testSequence, hotelDep, hotelArr);

                        if (result[0] == 1.0) {
                            // ¿Es realmente mejor esta nueva ruta?
                            // Sí, si el score sube, O si el score es igual pero gastamos menos distancia
                            if (result[2] > day.dailyScore || (result[2] == day.dailyScore && result[1] < day.dailyDistance)) {

                                day.chosenSites.clear();
                                day.chosenSites.addAll(testSequence);
                                day.setDailyDistance(result[1]);
                                day.setDailyScore(result[2]);

                                // Actualizamos los visitados
                                currentSchedule.siteAlreadyVisited.remove(visitedSite);
                                currentSchedule.siteAlreadyVisited.add(candidateSite);

                                improved = true;
                                break; // Rompemos el bucle de candidatos
                            }
                        }
                    }
                    if (improved) break; // Rompemos el bucle de sitios del día
                }

                if (improved) break; // Reiniciamos el While principal si hubo reemplazo
            }
        }
        return currentSchedule;
    }

    // AMÉLIORATION: 2-opt pour réorganiser l’itinéraire d’un jour
    public boolean apply2OptForDay(Schedule currentSchedule, int dayNb,
                                   Hotel hotelDep, Hotel hotelArr) {
        Day day = currentSchedule.travelDays.get(dayNb);
        if (day.chosenSites.size() < 2) return false;

        boolean improved = false;
        boolean anyImprovement= true;

       while (anyImprovement){
           anyImprovement= false;
           // Probamos invertir cada par de segmentos posibles
           for (int i = 0; i < day.chosenSites.size() - 1; i++) {
               for (int j = i + 1; j < day.chosenSites.size(); j++) {

                   // Creamos la secuencia con el segmento [i..j] invertido
                   List<TouristicSite> newSeq = new ArrayList<>();
                   for (int k = 0; k < i; k++)
                       newSeq.add((TouristicSite) day.chosenSites.get(k));
                   // Añadir [i..j] al revés
                   for (int k = j; k >= i; k--)
                       newSeq.add((TouristicSite) day.chosenSites.get(k));
                   for (int k = j + 1; k < day.chosenSites.size(); k++)
                       newSeq.add((TouristicSite) day.chosenSites.get(k));

                   double[] result = currentSchedule.evaluateSequence(dayNb, newSeq, hotelDep, hotelArr);

                   // ¿La nueva ruta es válida Y tiene menor distancia (dejando espacio para más sitios)?
                   if (result[0] == 1.0 && result[1] < day.dailyDistance - 0.001) {
                       day.chosenSites.clear();
                       day.chosenSites.addAll(newSeq);
                       day.setDailyDistance(result[1]);
                       day.setDailyScore(result[2]);
                       improved = true;
                       anyImprovement= true;
                   }
               }
           }
       }
        return improved;
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
        clonedSchedule.bestHotelPath = this.bestHotelPath;

        return clonedSchedule;
    }

    //----------------------------------------------------------------
    //-----------------------------EXECUTION RUN VNS------------------
    public void executeVNS(Schedule initialSchedule, List<Hotel> currentHotelPath) {
        long startTime = System.currentTimeMillis();
        long maxDuration = 15 * 1000; //120 seconds in miliseconds

        //on utilise la solution greedy

        //la route qu'on va detruir et repare constantement
        Schedule currentSchedule = initialSchedule.cloneSchedule();

        //ici on garde le meilleur schedule qu'on a trouver et on va pas toucher
        this.bestGlobalSchedule = currentSchedule.cloneSchedule();
        this.bestGlobalScore = totalScore(bestGlobalSchedule.travelDays);

        int k_max = 4; //on va utiliser 3 neighbohood differents

        System.out.println("-----------------------------------------------");
        System.out.println("Starting VNS with base SCORE = " + this.bestGlobalScore);

        //creation d'un bucle de temps pour l'impresion  de resultat(1 second=1000 milisecond)
        while ((System.currentTimeMillis() - startTime) < (maxDuration - 1000)) {
            int k=1; //premier shake de neighborhood

            while (k <= k_max && (System.currentTimeMillis() - startTime) < (maxDuration - 1000)) {

                //SHAKING
                //on clone et secoue la meilleure solution actuelle (celle qu'on a trouvée avec greedy) pour éviter de l'endommager, PAS la meilleur de notre historial
                Schedule shakenSchedule = currentSchedule.cloneSchedule();
                shakenSchedule.shake(k, currentHotelPath); //revisar

                //LOCAL SEARCH
                //klau methode pour prendre le shakenSchedule et l'ameliorer
                Schedule localOptimum = shakenSchedule.applyLocalSearch(shakenSchedule,currentHotelPath);

                //mise à jour
                double newScore = totalScore(localOptimum.travelDays);
                double currentScore = totalScore(currentSchedule.travelDays);

                //on a reussi  à ameliorer notre record local de ce qui est mondial?
                if (newScore > currentScore) {
                    currentSchedule = localOptimum; // Aceptamos el cambio temporal
                    k = 1; // Volvemos a empezar con N1

                    // 2. ¿Esta nueva solución de trabajo rompe el RÉCORD MUNDIAL?
                    if (newScore > this.bestGlobalScore) {
                        this.bestGlobalSchedule = currentSchedule.cloneSchedule();
                        this.bestGlobalScore = newScore;
                        System.out.println("NOUVELLE Score TROUVÉ: " + this.bestGlobalScore);
                    }
                } else {
                    k++; //on n'a pas amelioré, alors on fait une soute plus grand
                }
            }
            //si "k" surmonte à k_max, signifie que ni N3 peut ameliorer la solution actuelle
            //donc, le Vns returne à se devenir le dernier meilleur record conneu pour l'essayer d'un autre maniere
            currentSchedule= this.bestGlobalSchedule.cloneSchedule();
        }

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
            // N2: ÉCHANGE ENTRE JOURS (Echange d’un site du Jour 1 avec un du Jour 2)

            // on a besoin d’au moins 2 jours pour faire un échange
            if (this.daysNb < 2) return;

            for (int trys =0; trys <15; trys++){
                // on a choisi deux jours aléatoires distincts
                int dayIdx1 = rand.nextInt(this.daysNb);
                int dayIdx2 = rand.nextInt(this.daysNb);
                while (dayIdx1 == dayIdx2) {
                    dayIdx2 = rand.nextInt(this.daysNb); // on s'assure qu’ils ne sont pas le même jour
                }

                Day day1 = this.travelDays.get(dayIdx1);
                Day day2 = this.travelDays.get(dayIdx2);

                // Si l’un des jours est vide, nous ne pouvons rien échanger
                if (day1.chosenSites.isEmpty() || day2.chosenSites.isEmpty()) return;

                // on choisi un site aléatoire de chaque jour
                int siteIdx1 = rand.nextInt(day1.chosenSites.size());
                int siteIdx2 = rand.nextInt(day2.chosenSites.size());

                TouristicSite site1 = (TouristicSite) day1.chosenSites.get(siteIdx1);
                TouristicSite site2 = (TouristicSite) day2.chosenSites.get(siteIdx2);

                // on a créé des séquences de test pour les deux jours
                List<TouristicSite> testSeq1 = new ArrayList<>();
                for (Object obj : day1.chosenSites) testSeq1.add((TouristicSite) obj);

                List<TouristicSite> testSeq2 = new ArrayList<>();
                for (Object obj : day2.chosenSites) testSeq2.add((TouristicSite) obj);

                // Nous faisons l’échange (Swap) sur les listes de test
                testSeq1.set(siteIdx1, site2);
                testSeq2.set(siteIdx2, site1);

                // on a évalué si les deux jours sont encore valides avec le changement
                Hotel hotelDep1 = currentHotelPath.get(dayIdx1);
                Hotel hotelArr1 = currentHotelPath.get(dayIdx1 + 1);
                double[] res1 = evaluateSequence(dayIdx1, testSeq1, hotelDep1, hotelArr1);

                Hotel hotelDep2 = currentHotelPath.get(dayIdx2);
                Hotel hotelArr2 = currentHotelPath.get(dayIdx2 + 1);
                double[] res2 = evaluateSequence(dayIdx2, testSeq2, hotelDep2, hotelArr2);

                // Si les deux jours respectent la distance et le temps, nous appliquons le changement
                if (res1[0] == 1.0 && res2[0] == 1.0) {

                    day1.chosenSites.clear();
                    day1.chosenSites.addAll(testSeq1);
                    day1.setDailyDistance(res1[1]);
                    day1.setDailyScore(res1[2]);

                    day2.chosenSites.clear();
                    day2.chosenSites.addAll(testSeq2);
                    day2.setDailyDistance(res2[1]);
                    day2.setDailyScore(res2[2]);

                    // Remarque: Nous n’avons pas besoin de mettre à jour 'siteAlreadyVisited' parce que les deux sites
                    // étaient déjà visités, ils ont juste changé de jour.
                }
            }

        } else if(k==3){
            // N3: DESTRUCTION MASSIVE -----(Ruin & Recreate)------
            // on eleve plusieurs site pour laisse un grand trou que apres le local search va a remplir

            // on a choisi un jour au hasard
            int dayIdx = rand.nextInt(this.daysNb);
            Day randomDay = this.travelDays.get(dayIdx);

            // si le jour a 2 sites ou moins, il ne vaut pas la peine de le détruire
            if (randomDay.chosenSites.size() <= 2) return;

            // on decide le nombre de sites à supprimer (entre 2 et 4 sites)
            int sitesToRemove = 2 + rand.nextInt(3);
            // Nos aseguramos de no borrar más sitios de los que hay
            sitesToRemove = Math.min(sitesToRemove, randomDay.chosenSites.size());

            // on a supprimé les sites au hasard de la liste des sites déjà visité
            for (int i = 0; i < sitesToRemove; i++) {
                int idxToRemove = rand.nextInt(randomDay.chosenSites.size());
                TouristicSite removedSite = (TouristicSite) randomDay.chosenSites.remove(idxToRemove);

                // on remet les sites dans la liste de site disponibles
                this.siteAlreadyVisited.remove(removedSite);
            }

            // on a recalculé le temps et la distance qui nous restait
            Hotel hotelDep = currentHotelPath.get(dayIdx);
            Hotel hotelArr = currentHotelPath.get(dayIdx + 1);

            List<TouristicSite> remainingSites = new ArrayList<>();
            for (Object obj : randomDay.chosenSites) remainingSites.add((TouristicSite) obj);

            double[] result = evaluateSequence(dayIdx, remainingSites, hotelDep, hotelArr);

            // on a mis à jour la journée avec les nouvelles valeurs
            if (result[0] == 1.0) {
                randomDay.setDailyDistance(result[1]);
                randomDay.setDailyScore(result[2]);
            }


            //le VNS principal appellera applyLocalSearch() juste après avoir quitté le shake.
            //Cette Recherche Locale verra l’énorme trou et le remplira intelligemment.
        }
    }
}








