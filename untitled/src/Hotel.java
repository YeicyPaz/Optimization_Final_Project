public class Hotel extends Site{


    public Hotel(Point coordinates, double closing, String id){
        this.coordinates = coordinates;
        this.openingTime = 0;
        this.closingTime = closing;
        this.isVisited = false;
        this.id = id;
    }




}
