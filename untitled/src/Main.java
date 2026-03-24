//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.


import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {
    public static void main(String[] args) throws IOException {
        int nbRuns = 10; // Número de lanzamientos por instancia exigidos

        // Bucle para iterar por todas las instancias (del 1 al 36)
        for (int num = 1; num <= 36; num++) {
            String doc = "untitled/instances/Inst" + num + ".txt";
            System.out.println("\n========================================================");
            System.out.println("--- TRAITEMENT INSTANCE : " + num + " ---");
            System.out.println("========================================================");

            // 1. Leemos el archivo. (Asegúrate de que tu metodo readFile limpia las listas estáticas de Instance)
            try {
                Instance.readFile(doc);
            } catch (Exception e) {
                System.out.println("No se encontró la instancia " + num + " o hubo un error. Saltando...");
                continue;
            }

            double totalScoreAllRuns = 0;
            double bestScoreInstance = -1;
            long totalTimeInstance = 0;

            Schedule bestGlobalSchedule = null;
            List<Hotel> bestGlobalHotelPath = null;

            // 2. Ejecutamos los 10 Runs reglamentarios
            for (int run = 1; run <= nbRuns; run++) {
                long startTime = System.currentTimeMillis();

                // Construimos la solución inicial Greedy como punto de partida
                Schedule baseSchedule = new Schedule();
                baseSchedule.chooseHotelPath();

                VNS vnsOptimizer = new VNS();
                Random rand = new Random();

                // Damos 11.5 segundos a cada RUN
                long maxRunTimeMs = 5000;
                long runStartTime = System.currentTimeMillis();
                int attempts = 0;

                // Multi-start de hoteles interno para este RUN
                while ((System.currentTimeMillis() - runStartTime) < maxRunTimeMs) {
                    attempts++;
                    Schedule trySchedule = new Schedule();
                    List<Hotel> testHotelPath = new ArrayList<>(baseSchedule.bestHotelPath);

                    // A partir del segundo intento, mutamos un hotel al azar
                    if (attempts > 1 && testHotelPath.size() > 2) {
                        int hotelToChangeIdx = 1 + rand.nextInt(testHotelPath.size() - 2);
                        Hotel randomExtraHotel = Instance.hotels.get(rand.nextInt(Instance.hotels.size()));
                        testHotelPath.set(hotelToChangeIdx, randomExtraHotel);
                    }

                    trySchedule.buildGreedySolution(testHotelPath);

                    // Ejecutamos VNS intensivo durante 2 segundos por cada mutación de hotel
                    vnsOptimizer.executeVNS(trySchedule, testHotelPath, 2000);
                }

                long endTime = System.currentTimeMillis();
                long runTime = endTime - startTime;

                // 3. Capturamos el mejor score de este RUN
                double currentScore = vnsOptimizer.bestGlobalScore;

                totalScoreAllRuns += currentScore;
                totalTimeInstance += runTime;

                // 4. Guardamos la mejor solución absoluta de los 10 runs
                if (currentScore > bestScoreInstance) {
                    bestScoreInstance = currentScore;
                    bestGlobalSchedule = vnsOptimizer.bestGlobalSchedule;
                    bestGlobalHotelPath = vnsOptimizer.bestGlobalHotelPath;
                }

                System.out.println("  Run " + run + " : Score = " + currentScore + " | Temps = " + runTime + "ms | VNS Attempts = " + attempts);
            }

            // --- BILAN INSTANCE (Resultados Finales de la Instancia) ---
            double averageScore = totalScoreAllRuns / nbRuns;
            double averageTime = (double) totalTimeInstance / nbRuns;

            System.out.println("\n>> Instance " + num + " TERMINEE");
            System.out.println(">> Meilleur Score  : " + bestScoreInstance);
            System.out.println(">> Score Moyen     : " + averageScore);
            System.out.println(">> Temps Moyen     : " + averageTime + "ms");

            // 5. Guardado automático en archivo .sol cumpliendo estrictamente el formato
            if (bestGlobalSchedule != null && bestGlobalHotelPath != null) {
                saveSolution(num, bestScoreInstance, bestGlobalSchedule, bestGlobalHotelPath);
            }

            System.out.println("------------------------------------------");
        }
    }

    // Metodo para generar el archivo InstanceX.sol según las consignas del profesor
    public static void saveSolution(int instanceNum, double bestScore, Schedule schedule, List<Hotel> hotelPath) {
        String filename = "Instance" + instanceNum + ".sol";

        try (PrintWriter writer = new PrintWriter(new File(filename))) {
            // Primera línea: El valor de la solución
            writer.println(bestScore);

            // Siguientes líneas: HotelSalida Site1 Site2 ... HotelLlegada
            for (int i = 0; i < schedule.travelDays.size(); i++) {
                Day day = schedule.travelDays.get(i);
                Hotel hotelDep = hotelPath.get(i);
                Hotel hotelArr = hotelPath.get(i + 1);

                writer.print(hotelDep.id + " ");

                for (Object obj : day.chosenSites) {
                    TouristicSite site = (TouristicSite) obj;
                    writer.print(site.id + " ");
                }

                writer.println(hotelArr.id);
            }
            System.out.println(">> Fichier " + filename + " sauvegardé avec succès !");

        } catch (Exception e) {
            System.out.println("Erreur lors de la sauvegarde du fichier : " + e.getMessage());
        }
    }
}