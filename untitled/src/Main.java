import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        for (int num = 14; num <=36 ; num++) {
            //automatiser la lecture de tous les fichiers d'instances
            String doc = "untitled/instances/Inst" + num + ".txt";

            System.out.println(doc);
            Instance.readFile(doc);

            Schedule schedule1 = new Schedule();

            schedule1.chooseHotelPath();

            schedule1.buildGreedySolution(schedule1.bestHotelPath);

            // Afficher la solution initiale (avant ALNS)
            System.out.println("Solution initiale (après Greedy) :");
            for (int dayNb = 0; dayNb < schedule1.daysNb; dayNb++) {
                Day day = schedule1.travelDays.get(dayNb);
                System.out.println("Jour " + dayNb + " :");
                System.out.println("  Hôtel : " + schedule1.bestHotelPath.get(dayNb).id);
                System.out.println("  Sites visités :");

                for (int i = 0; i < day.chosenSites.size(); i++) {
                    System.out.println("    - " + day.chosenSites.get(i).id + " (score: " + day.chosenSites.get(i).score + ")");
                }
                System.out.println("  Score journalier : " + day.dailyScore);
                System.out.println("  Distance journalière : " + day.dailyDistance);
            }
            System.out.println("Score total initial : " + schedule1.totalScore(schedule1.travelDays));


            ALNS alns = new ALNS(schedule1, 2000, 0.9); // 100 itérations, alpha = 0.5
            alns.runALNSForSites(2000, 0.3); // 100 itérations ALNS, reactionFactor = 0.3

            String instanceNum = doc.replaceAll("[^0-9]", "");

            // gestion du fichier de sauvegarde
            alns.saveSolution(instanceNum);

            // Afficher la solution après ALNS
            System.out.println("\nSolution après ALNS :");
            for (int dayNb = 0; dayNb < schedule1.daysNb; dayNb++) {
                Day day = schedule1.travelDays.get(dayNb);
                System.out.println("Jour " + dayNb + " :");
                System.out.println("  Hôtel : " + schedule1.bestHotelPath.get(dayNb).id);
                System.out.println("  Sites visités :");

                for (TouristicSite site : day.chosenSites) {
                    System.out.println("    - " + site.id + " (score: " + site.score + ")");
                }
                System.out.println("  Score journalier : " + day.dailyScore);
            }
            System.out.println("Score total après ALNS : " + schedule1.totalScore(schedule1.travelDays));
        }

    }
}