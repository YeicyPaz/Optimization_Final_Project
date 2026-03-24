import java.util.ArrayList;
import java.util.List;

public class Day { //un voyage par jour

    int index;
    Hotel chosenHotel; //the chosen hotel for the night
    ArrayList<TouristicSite> chosenSites; // the chosen sites to visit in the day
    ArrayList<Double> departureTimes; // the departure times for each site visited in the day
    ArrayList<Double> arrivalTimes; // the arrival times for each site visited in the day
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
        this.departureTimes = new ArrayList<Double>();
        this.arrivalTimes = new ArrayList<Double>();
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
        return dailyDistance;
    }

    public void updateScoreFromList() {
        double total = 0;
        for (TouristicSite s : this.chosenSites) {
            total += s.score;
        }
        this.dailyScore = total;
    }
    public void refreshStats(int dayIdx, Schedule schedule) {
        // recalcul du Score
        double totalScore = 0;
        for (TouristicSite s : this.chosenSites) {
            totalScore += s.score;
        }
        this.dailyScore = totalScore;

        // recalcul de la Distance (Temps) avec horaires
        Hotel dep = schedule.bestHotelPath.get(dayIdx);
        Hotel arr = schedule.bestHotelPath.get(dayIdx + 1);

        double currentTime = 0;
        Site prev = dep;
        for (TouristicSite current : this.chosenSites) {
            currentTime += Instance.getDistMatrix(prev, current);
            if (currentTime < current.openingTime) {
                currentTime = current.openingTime;
            }
            prev = current;
        }
        currentTime += Instance.getDistMatrix(prev, arr);
        this.dailyDistance = currentTime;
    }

    public void setDailyDistance(double dailyDistance) {
        this.dailyDistance = dailyDistance;
    }

    public void setDailyScore(double dailyScore) {
        this.dailyScore = dailyScore;
    }

    public Site getLastSite() {
        if (chosenSites.isEmpty()) {
            return chosenHotel; // Si aucun site n'a été visité, retourner l'hôtel de départ
        }
        return chosenSites.get(chosenSites.size() - 1); // Retourner le dernier site visité
    }

    public Site getLastSiteOrHotel(Hotel hotelDep) {
        if (this.chosenSites == null || this.chosenSites.isEmpty()) {
            return hotelDep;
        }
        return this.chosenSites.get(this.chosenSites.size() - 1);
    }

    //-------------------VNS------------------------
    //on fait la clonation de la lista pour eviter de modifier la list original et que le resultat soit pire
    public Day cloneDay() {
        Day clonedDay = new Day(this.index);
        clonedDay.setDailyDistance(this.dailyDistance);
        clonedDay.setDailyScore(this.dailyScore);
        clonedDay.setChosenHotel(this.chosenHotel);

        // on fait les copies des list des sites, mais on maintient les reference des object originales
        // les propietés d'un site ne changent pas pendant son execution
        clonedDay.chosenSites = new ArrayList<>(this.chosenSites);
        return clonedDay;
    }

}
