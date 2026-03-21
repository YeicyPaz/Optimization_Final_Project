//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.


import java.io.IOException;


public class Main {
    public static void main(String[] args) throws IOException {
        String doc = "untitled/instances/Inst1.txt";
        Instance.readFile(doc);
        Schedule schedule1 = new Schedule();
        schedule1.chooseHotelPath();
//        // j'ai ajouté un ID dans les sites comme ça on peut affiché H0 H1 H2
//        for (int i = 0; i < schedule1.validHotelPaths.size(); i++) {
//            System.out.print("path number " + i + ": ");
//            for (int j = 0; j < Instance.d+1 ; j++) {
//                System.out.print(schedule1.validHotelPaths.get(i).get(j).id+" ");
//            }
//            System.out.println();
//        }

        // 2. Verificar que se encontraron hoteles y ejecutar Greedy
        if (!schedule1.bestHotelPath.isEmpty()) {

            // Construimos la solución inicial Greedy
            schedule1.buildGreedySolution(schedule1.bestHotelPath);

            System.out.println("------------------------------------------------------------");
            System.out.println("Solution initiale (après Greedy) :");
            for (int dayNb = 0; dayNb < schedule1.daysNb; dayNb++) {
                Day day = schedule1.travelDays.get(dayNb);
                System.out.println("-----------------------------------------------");
                System.out.println("Jour " + dayNb + " :");

                //on imprime l'hotel de sorti de ce jour
                System.out.println("  Hôtel : " + schedule1.bestHotelPath.get(dayNb).id);
//                for (int i =0;i<day.chosenSites.size(); i++ ){
//                    System.out.println(day.chosenSites.get(i).id+", point x de site: " + day.chosenSites.get(i).getCoordinates().x + " , point y: " + day.chosenSites.get(i).getCoordinates().y);
//                    System.out.println(day.chosenSites.get(i).openingTime);
//
//                }
                System.out.println("  Sites visités :");


                for (TouristicSite site : day.chosenSites) {
                    System.out.println("    - " + site.id + " (score: " + site.score + ")");
                }
                System.out.println("  Score journalier : " + day.dailyScore);
                System.out.println("  Distance journalière : " + day.dailyDistance);
            }

            System.out.println("-------------------------------------------------------");
            System.out.println("Score total initial : " + schedule1.totalScore(schedule1.travelDays));

        //  ========================================================================
        //  ================================  VNS ===================//       execution de VNS avec la route choisi
        long starTimeGlobal = System.currentTimeMillis();

        schedule1.executeVNS(schedule1, schedule1.bestHotelPath);

        long endTimeGlobal = System.currentTimeMillis();
        double timeInSeconds =(endTimeGlobal- starTimeGlobal)/1000.0;

        System.out.println("--------------------------------------");
        System.out.println("--------FINAL RESULTS VNS--------------");
        System.out.println("---------------------------------------");
        System.out.println("Execution time: " + timeInSeconds+ "seconds");

        //on parcour le meillieur schedule trouvé par le VNS, On montre jour par jour
        for (int i = 0; i< schedule1.bestGlobalSchedule.travelDays.size(); i++){
            Day day = (Day) schedule1.bestGlobalSchedule.travelDays.get(i);
            System.out.println("Day: " + i + " | distance: " + String.format("%.2f", day.dailyDistance) + " | Score: "+ day.dailyScore);
            System.out.print("Sites visited: ");
            for (Object obj: day.chosenSites){
                TouristicSite site = (TouristicSite) obj;
                System.out.print(site.id + " ");
            }
            System.out.println();

        }
        System.out.println("--------------------------------------");
        System.out.println("SCORE GREEDY INITIAL: " + schedule1.totalScore(schedule1.travelDays));
        System.out.println("SCORE TOTAL VNS FINAL: " +schedule1.totalScore(schedule1.bestGlobalSchedule.travelDays));
        System.out.println("-------------------------------------");

        } else {
            System.out.println("ERROR: we couldn't create a hotel path.");
        }
    }
}