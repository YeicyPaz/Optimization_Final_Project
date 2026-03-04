import java.util.ArrayList;

public class Day {

    Hotel chosenHotel; //the chosen hotel for the night
    ArrayList<TouristicSite> chosenSites; // the chosen sites to visit in the day
    double dailyDistance;
    double dailyScore;

    /*
    Journée -> array list des sites et hotels + distance parcourus

     */
    public Day(){
        this.chosenSites = new ArrayList<TouristicSite>();
        dailyDistance = 0.0;
        dailyScore = 0.0;
    }

    public void setChosenHotel(Hotel chosenHotel) {
        this.chosenHotel = chosenHotel;
    }

    public void addChosenSites(TouristicSite site) {

        this.chosenSites.add(site);
    }
    public void updateDistance(){
        double counter = 0.0;

        // à appeler quand on rajoute des sites

        for (TouristicSite site: chosenSites) {
            counter = counter + Instance.getDistMatrix(chosenHotel,site);
        }
        dailyDistance = counter;
    }
}