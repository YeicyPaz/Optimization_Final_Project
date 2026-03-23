import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class VNS {

    public double bestGlobalScore = -1.0;
    public Schedule bestGlobalSchedule = null;


    //------------------LOCAL SEARCH----------------------
    public Schedule applyLocalSearch(Schedule currentSchedule, List<Hotel> currentHotelPath){
//        for(int dayNb=0; dayNb < currentSchedule.daysNb; dayNb++){
//            Hotel hotelDep = currentHotelPath.get(dayNb);
//            Hotel hotelArr = currentHotelPath.get(dayNb +1);
//        }

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
                //---------fase D'INSERTION-----------
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

                // -----------PHASE DE REMPLACEMENT--------
                // Si rien de nouveau ne se produit, nous essayons de trouver un meilleur.
                // ==========================================
                for (int i = 0; i < day.chosenSites.size(); i++) {
                    TouristicSite visitedSite = (TouristicSite) day.chosenSites.get(i);

                    for (TouristicSite candidateSite : unvisited) {
                        // on a essayé le changement que si le candidat donne plus de points, ou s’il donne les mêmes points mais on soupçonne qu’il économise la distance.
                        if (candidateSite.score < visitedSite.score) continue;

                        List<TouristicSite> testSequence = new ArrayList<>();
                        for (Object obj : day.chosenSites) testSequence.add((TouristicSite) obj);

                        // on a fait le swap sur la même position
                        testSequence.set(i, candidateSite);

                        double[] result = currentSchedule.evaluateSequence(dayNb, testSequence, hotelDep, hotelArr);

                        if (result[0] == 1.0) {
                            // si le nouvel itinéraire est meilleur, le score augmente, OU si le score est le même mais que nous passons moins de distance
                            if (result[2] > day.dailyScore || (result[2] == day.dailyScore && result[1] < day.dailyDistance)) {

                                day.chosenSites.clear();
                                day.chosenSites.addAll(testSequence);
                                day.setDailyDistance(result[1]);
                                day.setDailyScore(result[2]);

                                // on met à jour les personnes visitées
                                currentSchedule.siteAlreadyVisited.remove(visitedSite);
                                currentSchedule.siteAlreadyVisited.add(candidateSite);

                                improved = true;
                                break; //pour la boucle des candidats
                            }
                        }
                    }
                    if (improved) break; // pour le sites du jour
                }

                if (improved) break; // dans la bucle principale si il y a eu un remplacement
            }
        }
        return currentSchedule;
    }

    //--------------VNS SOLUTION------------------------
    //----------------------------------------------------------------
    //-----------------------------EXECUTION RUN VNS------------------
    public void executeVNS(Schedule initialSchedule, List<Hotel> currentHotelPath) {
        long startTime = System.currentTimeMillis();
        long maxDuration = 15 * 1000; // seconds in miliseconds

        //on utilise la solution greedy

        //la route qu'on va detruir et repare constantement
        Schedule currentSchedule = initialSchedule.cloneSchedule();

        //ici on garde le meilleur schedule qu'on a trouver et on va pas toucher
        this.bestGlobalSchedule = currentSchedule.cloneSchedule();
        this.bestGlobalScore = currentSchedule.totalScore(bestGlobalSchedule.travelDays);

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
                this.shake(shakenSchedule, k, currentHotelPath);

                //LOCAL SEARCH
                //klau methode pour prendre le shakenSchedule et l'ameliorer
                Schedule localOptimum =this.applyLocalSearch(shakenSchedule,currentHotelPath);

                //mise à jour
                double newScore = localOptimum.totalScore(localOptimum.travelDays);
                double currentScore = currentSchedule.totalScore(currentSchedule.travelDays);

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

    //----------------------------METODE SHAKE----------------------------------
    public void shake(Schedule currentSchedule, int k, List<Hotel> currentHotelPath) {
        Random rand = new Random();

        if (k == 1) {
            //le premier type de neighborhood |(Swap) N1=échange d'un site avec une autre qu'on a jamais tester mais il faut varifie que le nouveau site respect les regles(avec metode evaluate)

            //election d'un jour aleatoire
            int dayIndex = rand.nextInt(currentSchedule.daysNb);
            Day randomDay = currentSchedule.travelDays.get(dayIndex);

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
                if (!currentSchedule.siteAlreadyVisited.contains(s)) {
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

                double[] result = currentSchedule.evaluateSequence(dayIndex, testSequence, hotelDep, hotelArr);
                boolean isValid = (result[0] == 1.0);

                if (isValid) {
                    //c'est valid le changement trouvé donc, on change le jour
                    randomDay.chosenSites.clear();
                    randomDay.chosenSites.addAll(testSequence);

                    currentSchedule.siteAlreadyVisited.remove(removedSite);
                    currentSchedule.siteAlreadyVisited.add(newSite);

                    randomDay.setDailyDistance(result[1]);
                    randomDay.setDailyScore(result[2]);
                } else {
                    //on laisse l'espace vide pour que la recherche local remplisse l'espace vide
                    testSequence.remove(newSite); //on enleve le site invalide
                    double[] resultRemoved = currentSchedule.evaluateSequence(dayIndex, testSequence, hotelDep, hotelArr);

                    randomDay.chosenSites.clear();
                    randomDay.chosenSites.addAll(testSequence);
                    currentSchedule.siteAlreadyVisited.remove(removedSite);

                    randomDay.setDailyDistance(resultRemoved[1]);
                    randomDay.setDailyScore(resultRemoved[2]);
                }
            }
        }
        else if(k==2){
            // N2: ÉCHANGE ENTRE JOURS (Echange d’un site du Jour 1 avec un du Jour 2)

            // on a besoin d’au moins 2 jours pour faire un échange
            if (currentSchedule.daysNb < 2) return;

            for (int trys =0; trys <15; trys++){
                // on a choisi deux jours aléatoires distincts
                int dayIdx1 = rand.nextInt(currentSchedule.daysNb);
                int dayIdx2 = rand.nextInt(currentSchedule.daysNb);
                while (dayIdx1 == dayIdx2) {
                    dayIdx2 = rand.nextInt(currentSchedule.daysNb); // on s'assure qu’ils ne sont pas le même jour
                }

                Day day1 = currentSchedule.travelDays.get(dayIdx1);
                Day day2 = currentSchedule.travelDays.get(dayIdx2);

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
                double[] res1 = currentSchedule.evaluateSequence(dayIdx1, testSeq1, hotelDep1, hotelArr1);

                Hotel hotelDep2 = currentHotelPath.get(dayIdx2);
                Hotel hotelArr2 = currentHotelPath.get(dayIdx2 + 1);
                double[] res2 = currentSchedule.evaluateSequence(dayIdx2, testSeq2, hotelDep2, hotelArr2);

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
            int dayIdx = rand.nextInt(currentSchedule.daysNb);
            Day randomDay = currentSchedule.travelDays.get(dayIdx);

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
                currentSchedule.siteAlreadyVisited.remove(removedSite);
            }

            // on a recalculé le temps et la distance qui nous restait
            Hotel hotelDep = currentHotelPath.get(dayIdx);
            Hotel hotelArr = currentHotelPath.get(dayIdx + 1);

            List<TouristicSite> remainingSites = new ArrayList<>();
            for (Object obj : randomDay.chosenSites) remainingSites.add((TouristicSite) obj);

            double[] result = currentSchedule.evaluateSequence(dayIdx, remainingSites, hotelDep, hotelArr);

            // on a mis à jour la journée avec les nouvelles valeurs
            if (result[0] == 1.0) {
                randomDay.setDailyDistance(result[1]);
                randomDay.setDailyScore(result[2]);
            }


            //le VNS principal appellera applyLocalSearch() juste après avoir quitté le shake.
            //la recherche Locale verra l’énorme trou et le remplira intelligemment.
        }
    }
}
