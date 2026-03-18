//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.


import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        String doc = "untitled/instances/Inst1.txt";
        Instance.readFile(doc);
        Schedule schedule1 = new Schedule();
        schedule1.generateHotelPaths();
        // j'ai ajouté un ID dans les sites comme ça on peut affiché H0 H1 H2
        for (int i = 0; i < schedule1.validHotelPaths.size(); i++) {
            System.out.print("path number " + i + ": ");
            for (int j = 0; j < Instance.d+1 ; j++) {
                System.out.print(schedule1.validHotelPaths.get(i).get(j).id+" ");
            }
            System.out.println();
        }
        schedule1.buildGreedySolution(schedule1.validHotelPaths.get(2));
        for (Day day: schedule1.travelDays
             ) {
            System.out.println("Daily distance: "+day.dailyDistance);
            System.out.println("Daily score: "+day.dailyScore);

        }
        System.out.println("-----------------------------------------------------------");
        System.out.println("score total = "+schedule1.totalScore(schedule1.travelDays));


        long starTimeGlobal = System.currentTimeMillis();

        schedule1.executeVNS(schedule1,schedule1.validHotelPaths.get(2));

        long endTimeGlobal = System.currentTimeMillis();
        double timeInSeconds =(endTimeGlobal-starTimeGlobal)/1000.0;

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
    }
}