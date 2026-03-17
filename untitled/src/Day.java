import java.util.ArrayList;

public class Day { //un voyage par jour

    int index;
    Hotel chosenHotel; //the chosen hotel for the night
    ArrayList<TouristicSite> chosenSites; // the chosen sites to visit in the day
    double dailyDistance;
    double dailyScore;

    /*
    Journée -> array list des sites et hotels + distance parcourus

     */
    public Day(int index){
        this.index = index;
        this.chosenSites = new ArrayList<TouristicSite>();
        dailyDistance = 0.0;
        dailyScore = 0.0;
    }
    public void removeSite(int siteInx){
        TouristicSite site = chosenSites.get(siteInx);
        chosenSites.remove(site);
    }
    public void removeSite(TouristicSite site){
        if(chosenSites.contains(site)){
            chosenSites.remove(site);
        }else {
            System.out.println("Can't remove the site, it is not on the list");
        }
    }

    public void setChosenHotel(Hotel chosenHotel) { //pour l'hotel de fin de jour
        this.chosenHotel = chosenHotel;
    }

    public void addChosenSite(TouristicSite site) { //pour ajouter un site visité ce jour la
        this.chosenSites.add(site);
    }
    public void updateDistanceAndScore(){ //pour recalcule la distance du jour en utilisant getDistMatrix() de la class intance
        double counter = 0.0;
        double counterScore = 0.0;


        dailyDistance = counter;
    }

    public Hotel getChosenHotel() {
        return chosenHotel;
    }

    public ArrayList<TouristicSite> getChosenSites() {
        return chosenSites;
    }

    public double getDailyDistance() {
        double counter = 0.0;
        /*for (int i = 0; i < chosenSites.size(); i++) {
            if(i == 0){
                System.out.println("index day "+this.index+"l'hotel choisi " +this.chosenHotel.id);
                counter += Instance.getDistMatrix(Instance.hotels.get(index),chosenSites.get(0));
            } else if (i == chosenSites.size() - 1) {
                counter += Instance.getDistMatrix(chosenSites.get(i),Instance.hotels.get(index));
            } else{
                counter += Instance.getDistMatrix(chosenSites.get(i-1), chosenSites.get(i));
            }

        }*/return counter;

    }


    public double getDailyScore() {
        return dailyScore;
    }

    public void setDailyDistance(double dailyDistance) {
        this.dailyDistance = dailyDistance;
    }

    public void setDailyScore(double dailyScore) {
        this.dailyScore = dailyScore;
    }

    public void sitesToString() {
        for (Site site: chosenSites
             ) {
            System.out.println(site.id);
        }
    }
}
