import java.util.ArrayList;
import java.util.List;

public class GreedySolver {
    private Instance instance; //instance du problem

    private List<Day> bestSolution; //la meilleure solution trouvée

    private double bestTotalScore;  //la meilleure solution trouvée

    private List<Hotel> bestHotelPath; //le chemin des hotel correspondent à la meilleure solution

    private double [][] distMatrix;

    private ArrayList<TouristicSite> allSites; //list de tous les sites touristiques

    private int daysNb; //nombre des jours de voyage

    private double[] tD; //buget des distance par jour: tD[0] = dia1


    public GreedySolver(Instance instance){
        this.instance = instance;
        this.distMatrix= instance.getDistMatrix();
        this.allSites= instance.getTouristicSites();
        this.daysNb=instance.d;  //nombre de jours
        this.tD=instance.tD; //budget journalier[jour1, jour2..]
        this.bestTotalScore=0.0;
        this.bestSolution=new ArrayList<>();
        this.bestHotelPath=new ArrayList<>();

    }

    public void solve(List<List<Hotel>> validHotelPaths) {
        System.out.println("___________Starting GREEDY SOLVER______");
        System.out.println("Paths to explore: " + validHotelPaths.size());

        //on fait l'iteration sur chaque chemin des hotels possibles
        for (List<Hotel> hotelPath : validHotelPaths) {

            List<Day> solution = buildGreedySolution(hotelPath); //construction du greedy pour ce chemin especifique (fonction plus en bas)

            //calculer le score total de ce solution
            double totalScore = computeTotalScore(solution);

            //si le score est meilleur que celle qu'on avait avant, on la garde
            if (totalScore > bestTotalScore) {
                bestTotalScore = totalScore;
                bestSolution = solution;
                bestHotelPath = hotelPath;
                System.out.println("Nouvelle meilleur solution: SCORE= " + bestTotalScore);
            }

        }

        System.out.println("-------Greedy finished. Best score: " + bestTotalScore + "-------");

    }

    //pour la construction d'un itineraire complete pour un chemin de hotels donné, pour chaque jour il selection de forma gredy quels sites visiter
        private List<Day> buildGreedySolution(List<Hotel> hotelPath){
        //List de jour qu'on va construir
        List<Day> days= new ArrayList<>();

        //ensemble de sites deja visité pour ne pas le repetir entre les jours
        boolean[] visited= new boolean[allSites.size()]; //on utilise l'indice du site en allsites comme control

        //-----pour retourner chaque jour de voyage-------------
        for (int d=0; d<daysNb; d++){

            Hotel hotelStart = hotelPath.get(d);//hotel premier jour

            Hotel hotelEnd = hotelPath.get(d+1);//hotel dernier jour

            double distanceBudget = tD[d];//budget pour ce jour-la

            //construction de l'itineraire de jour d
            Day day = buildDayItinerary(hotelStart, hotelEnd, distanceBudget, visited, d);

            day.setChosenHotel(hotelEnd);//on asigne l'hotel d'arrivé à object Day

            days.add(day); //on ajoute le jour dans la solution

        }
        return days;
    }

     /* ================================================================
             *  buildDayItinerary()
     * ================================================================
     */
     private Day buildDayItinerary(Hotel hotelStart, Hotel hotelEnd,
                                   double distanceBudget, boolean[] visited,
                                   int dayIndex) {

         // Crear el objeto Day que vamos a rellenar
         Day day = new Day();

         // ── Estado actual del turista ───────────
         // Posición actual: empezamos en el hotel de inicio
         // Usamos el índice del hotel en la lista de hoteles de la instancia
         int currentNodeIndex = getHotelIndex(hotelStart);

         // Tiempo actual: el turista sale al abrir el hotel de inicio
         // El hotel tiene openingTime = 0 en todas las instancias → salida a t=0
         double currentTime = hotelStart.openingTime;

         // Distancia restante disponible para hoy
         double remainingDist = distanceBudget;

         // Índice del hotel de llegada (para consultar la distMatrix)
         int endHotelIndex = getHotelIndex(hotelEnd);

         // ── Bucle greedy: añadir sitios mientras sea posible ─────────
         boolean improvement = true;

         while (improvement) {
             improvement = false;  // asumimos que no encontraremos nada

             // Variables para recordar el mejor candidato de esta iteración
             int    bestSiteIndex = -1;        // índice en allSites del mejor sitio
             double bestRatio     = -1.0;      // mejor ratio score/dist encontrado
             double bestArrival   = 0.0;       // tiempo de llegada al mejor sitio
             double bestDistCost  = 0.0;       // distancia hasta el mejor sitio

             // ── Evaluar cada sitio turístico como candidato ───────────
             for (int i = 0; i < allSites.size(); i++) {

                 // Saltar sitios ya visitados en días anteriores o hoy mismo
                 if (visited[i]) continue;

                 TouristicSite candidate = allSites.get(i);

                 // Índice del candidato en la distMatrix
                 // Los hoteles ocupan las primeras posiciones, luego los sitios
                 int candidateIndex = getSiteIndex(i);

                 // ── Calcular distancia desde posición actual al candidato ──
                 double distToCand = distMatrix[currentNodeIndex][candidateIndex];

                 // ── Calcular tiempo de llegada al candidato ───────────────
                 // Asumimos velocidad = 1 unidad de distancia por unidad de tiempo
                 double arrivalAtCand = currentTime + distToCand;

                 // ── Verificar ventana de tiempo del candidato ────────────
                 // Si llegamos DESPUÉS del cierre → imposible visitar
                 if (arrivalAtCand > candidate.closingTime) continue;

                 // Si llegamos ANTES de la apertura → esperamos hasta que abra
                 // El tiempo de salida será la apertura (service time = 0)
                 double departureFromCand = Math.max(arrivalAtCand, candidate.openingTime);

                 // ── Verificar que podemos llegar al hotel final a tiempo ─
                 // Distancia desde el candidato hasta el hotel de fin
                 double distCandToEnd = distMatrix[candidateIndex][endHotelIndex];

                 // Tiempo de llegada al hotel de fin si visitamos el candidato
                 double arrivalAtEnd = departureFromCand + distCandToEnd;

                 // Si no llegamos antes del cierre del hotel final → descartar
                 if (arrivalAtEnd > hotelEnd.closingTime) continue;

                 // ── Verificar presupuesto de distancia ───────────────────
                 // Coste total de añadir este candidato:
                 //   distancia actual→candidato + candidato→hotelEnd
                 // (en la siguiente iteración actualizaremos con el real)
                 double distCost = distToCand + distCandToEnd;

                 // Si nos pasamos del presupuesto → descartar
                 // Nota: comparamos contra remainingDist que ya descuenta
                 // la distancia de ir directo al hotel final
                 if (distToCand + distCandToEnd > remainingDist) continue;

                 // ── Calcular ratio score / distancia ─────────────────────
                 // Evitar división por cero si el sitio está en la misma posición
                 double ratio = (distToCand > 0)
                         ? (double) candidate.score / distToCand
                         : candidate.score;  // sitio a distancia 0: ratio infinito

                 // ── Actualizar mejor candidato si mejora el ratio ────────
                 if (ratio > bestRatio) {
                     bestRatio      = ratio;
                     bestSiteIndex  = i;
                     bestArrival    = arrivalAtCand;
                     bestDistCost   = distToCand;
                 }
             }

             // ── Si encontramos un candidato válido → añadirlo al día ──
             if (bestSiteIndex != -1) {

                 TouristicSite chosenSite = allSites.get(bestSiteIndex);

                 // Añadir el sitio al objeto Day
                 day.addChosenSites(chosenSite);

                 // Marcar como visitado para que no se repita
                 visited[bestSiteIndex] = true;

                 // Actualizar el tiempo actual:
                 // Si llegamos antes de la apertura, esperamos → salimos en apertura
                 currentTime = Math.max(bestArrival, chosenSite.openingTime);
                 // Service time = 0 en estas instancias, así que no sumamos nada

                 // Actualizar distancia restante: restamos la distancia recorrida
                 remainingDist -= bestDistCost;

                 // Actualizar posición actual al sitio recién añadido
                 currentNodeIndex = getSiteIndex(bestSiteIndex);

                 // Indicar que hubo mejora para continuar el bucle
                 improvement = true;

                 System.out.println("    Día " + (dayIndex + 1)
                         + " → añadido: " + chosenSite.id
                         + " | score: " + chosenSite.score
                         + " | tiempo: " + String.format("%.2f", currentTime)
                         + " | dist restante: " + String.format("%.2f", remainingDist));
             }
             // Si bestSiteIndex == -1, no había ningún candidato válido
             // → el bucle termina (improvement sigue en false)
         }

         // Actualizar la distancia total del día en el objeto Day
         // (usando el metodo que ya tenía tu pareja)
         day.updateDistance(instance);

         return day;
     }


    //----------------metodo auxiliar--------------------
    //fait la sumatoire de tous les score de tous les sites visités dans la solution
    private  double computeTotalScore(List<Day> days){ //days  donne la liste de jours de la solution
        double total = 0.0;
        for (Day day : days){
            total+= day.dailyScore;
        }
        return total;
    }

    //retourne l'indice d'un hotel  dans la distMatrix
    private int getHotelIndex(Hotel hotel) {
        ArrayList<Hotel> hotels = instance.getHotels();
        for (int i = 0; i < hotels.size(); i++) {
            if (hotels.get(i) == hotel) return i;
        }
        // Si no se encontró (no debería pasar), devolver 0
        System.err.println("WARN: hotel no encontrado en la lista");
        return 0;
    }

    /*
            * Devuelve el índice de un sitio turístico en la distMatrix.
            * Los sitios turísticos van DESPUÉS de los hoteles en la matriz

     */
    private int getSiteIndex(int siteIndexInList) {
        int numberOfHotels = instance.getHotels().size();
        return numberOfHotels + siteIndexInList;
    }

    public void printSolution() {

        if (bestSolution.isEmpty()) {
            System.out.println("On n'as pas trouver une solution.");
            return;
        }

        System.out.println("\n========== BEST SOLUTION ==========");
        System.out.println("Score total: " + bestTotalScore);
        System.out.println();

        // El hotel de inicio del primer día es siempre el primero del path
        Hotel current = bestHotelPath.get(0);

        for (int d = 0; d < bestSolution.size(); d++) {
            Day day = bestSolution.get(d);

            // Construir la línea del día: HotelInicio Sitio1 Sitio2 ... HotelFin
            StringBuilder sb = new StringBuilder();
            sb.append(current.id);  // nombre del hotel de inicio

            // Añadir cada sitio turístico visitado ese día
            for (TouristicSite site : day.chosenSites) {
                sb.append(" ").append(site.id);
            }

            // Añadir el hotel de fin del día
            sb.append(" ").append(day.chosenHotel.id);

            System.out.println("Day " + (d + 1) + ": " + sb.toString()
                    + "   [dist=" + String.format("%.2f", day.dailyDistance)
                    + ", score=" + day.dailyScore + "]");

            // El hotel de inicio del día siguiente es el hotel de fin de hoy
            current = day.chosenHotel;
        }


        System.out.println("=====================================\n");
    }
}
