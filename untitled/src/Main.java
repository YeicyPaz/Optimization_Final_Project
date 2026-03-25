import java.io.IOException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws IOException {
        int nbRuns = 10; // Nombre de lancements par instance

        for (int num = 1; num <= 36; num++) {
            String doc = "untitled/instances/Inst" + num + ".txt";
            System.out.println("\n--- TRAITEMENT INSTANCE : " + num + " ---");

            double totalScoreAllRuns = 0;
            double bestScoreInstance = -1;
            long totalTimeInstance = 0;
            Schedule bestGlobalSchedule = null;

            // On lit le fichier une seule fois par instance
            Instance.readFile(doc);

            for (int run = 1; run <= nbRuns; run++) {
                long startTime = System.currentTimeMillis();

                // Initialisation d'un nouveau planning pour ce run
                Schedule currentSchedule = new Schedule();

                // construction de la solution initiale (Greedy)
                currentSchedule.chooseHotelPath();
                currentSchedule.buildGreedySolution(currentSchedule.bestHotelPath);

                //optimisation par ALNS
                ALNS alns = new ALNS(currentSchedule, 20000, 0.9996);
                alns.runALNSForSites(20000, 0.15);
                //pour les test (exécution rapide)
                //ALNS alns = new ALNS(currentSchedule, 5000, 0.998);
                //alns.runALNSForSites(5000, 0.15);

                long endTime = System.currentTimeMillis();
                long runTime = endTime - startTime;

                // calcul du score propre (en utilisant ta méthode de nettoyage)
                double currentScore = currentSchedule.getCleanTotalScore();

                totalScoreAllRuns += currentScore;
                totalTimeInstance += runTime;

                // on garde la trace du meilleur run pour cette instance
                if (currentScore > bestScoreInstance) {
                    bestScoreInstance = currentScore;
                    // On clone ou on sauvegarde le meilleur schedule pour le fichier final
                    bestGlobalSchedule = currentSchedule;
                }

                System.out.println("  Run " + run + " : Score = " + currentScore + " | Temps = " + runTime + "ms");
            }

            // --- BILAN INSTANCE ---
            double averageScore = totalScoreAllRuns / nbRuns;
            double averageTime = (double) totalTimeInstance / nbRuns;

            System.out.println(">> Instance " + num + " TERMINEE");
            System.out.println(">> Meilleur Score : " + bestScoreInstance);
            System.out.println(">> Score Moyen     : " + averageScore);
            System.out.println(">> Temps Moyen     : " + averageTime + "ms");

            // Sauvegarde de la meilleure solution trouvée sur les 10 runs
            if (bestGlobalSchedule != null) {
                String instanceNum = String.valueOf(num);
                // On s'assure que l'ALNS utilisé pour sauvegarder pointe sur le bon schedule
                ALNS finalSaver = new ALNS(bestGlobalSchedule, 0, 0);
                finalSaver.saveSolution(instanceNum);
            }

            System.out.println("------------------------------------------");
        }
    }
}

