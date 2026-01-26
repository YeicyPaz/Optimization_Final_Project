public class TouristicSite extends Site{
    int score;
    public TouristicSite(Point coordinates,int score, double opening, double closing){
        this.coordinates = coordinates;
        this.score = score;
        this.openingTime = opening;
        this.closingTime = closing;
    }


}
