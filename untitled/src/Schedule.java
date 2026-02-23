import java.util.*;
public class Schedule {

    ArrayList<Day> schedule = new ArrayList<>();
    int daysNb = Instance.d;
    public List<List<Hotel>> validHotelPaths = new ArrayList<>(); // combinations with fixed start and end
    public Schedule(){
        for (int d = 0; d < daysNb; d++) {
            schedule.add(new Day()); // Initialisation of nbDays days in the schedule
        }
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
    private void exploreHotels(List<Hotel> currentPath, Hotel currentHotel, int day, Hotel hotelEnd) {

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
}




