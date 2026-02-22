import java.util.ArrayList;
import java.util.HashSet;

public class Checker {

    public static boolean check(Solution solution) {
        if (solution.days.size() != Instance.d) {
            System.out.println("ERROR: the solution doesn't have the correct number days of days");
            return false;
        }

        // Je veux m'assurer que ne se répète pas des endroits visités. Il faut que le voyageur les visit qu'une fois max et peûvent ne pas être visité
        HashSet<Integer> visitedsites = new HashSet<>();
        double calculatedTotalScore =0;

        //il commence a parcourir jour par jour
        for (int dayIndex =0; dayIndex < solution.days.size(); dayIndex ++){

            ArrayList<Site> currentRoute = solution.days.get((dayIndex));

            //valider que le jour ne soit pas vide, minimun l'hotel de debut et l'hotel du fin
            if(currentRoute.size() < 2){
                System.out.println("ERROR: the days "+ dayIndex+ "has less of 2 stops");
                return false;
            }
            double currentTime =0; //l'horlogue commence en 0 chaque jour
            double currentDistance =0; //counteur de kilometre commence en 0

            //parcurir CHAQUE SITE de la rute de la JOURNÉE
            for (int i=0; i < currentRoute.size() -1; i++ ){
                Site start = currentRoute.get(i);
                Site end = currentRoute.get(i+1);

                //calcule les distance entre le point actual et le suivant
                double dist = distance(start,end);
                currentDistance += dist;

                //le temp de voyage c'est egal a la distance
                double arrivalTime = currentTime + dist;
            }

        }

        return true;
    }
}
