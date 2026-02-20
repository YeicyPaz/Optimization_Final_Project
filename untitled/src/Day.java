import java.util.ArrayList;

public class Day {

    Hotel chosenHotel; //the chosen hotel for the night
    ArrayList<TouristicSite> chosenSites; // the chosen sites to visit in the day
    double distance;

    /*
    Journée -> array list des sites et hotels + distance parcourus

     */


    public void setChosenHotel(Hotel chosenHotel) {
        this.chosenHotel = chosenHotel;
    }

    public void addChosenSites(TouristicSite site) {
        this.chosenSites.add(site);
    }
    public void updateDistance(){
        double counter = 0.0;
        for (TouristicSite site: chosenSites
             ) {
            //counter = counter + MATRICEDISTANCE[chosenHotel,site];
        }
        distance = counter;
    }

}
