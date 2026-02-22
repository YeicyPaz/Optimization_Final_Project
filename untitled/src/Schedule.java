import java.util.*;
public class Schedule {

    ArrayList<Day> schedule = new ArrayList<>();
    int daysNb = Instance.d;


    /*
    faire récursivement
    regarder tout les chemins possibles d'hotels (prenant en compte seulement les hotels)
    dans un arbre on construit les différentes possibilités
    en effacant celle qui épuise la distance de parcours avant d'arriver à l'hotel final
    matrices avec les différents chemins d'hotels (qui respecte la distance)
     */
    public Schedule(){
        for (int d = 0; d < daysNb; d++) {
            schedule.add(new Day()); //Initialisation of nbDays days in the schedule
        }
    }

    public void generateHotelSkeletons(int dayIndex, Hotel currentStartHotel) {
        //base case - stop condition, we fill up every day 0 to daysNb
        if(dayIndex == daysNb) {
            // we verify that the last hotel is the fixed hotel
            //if(condition){
            System.out.println("Valid Squeletton found");
            // save this schedule
            //}
            return;
        }

    }
}
