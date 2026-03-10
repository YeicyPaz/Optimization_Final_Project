import java.util.ArrayList;

public class Day { //un voyage par jour

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

    public void setChosenHotel(Hotel chosenHotel) { //pour l'hotel de fin de jour
        this.chosenHotel = chosenHotel;
    }

    public void addChosenSites(TouristicSite site) { //pour ajouter un site visité ce jour la

        this.chosenSites.add(site);
    }
    public void updateDistance(){ //pour recalcule la distance du jour en utilisant getDistMatrix() de la class intance
        double counter = 0.0;

        // à appeler quand on rajoute des sites

        for (TouristicSite site: chosenSites) {
            counter = counter + Instance.getDistMatrix(chosenHotel,site);
        }
        dailyDistance = counter;
    }
}