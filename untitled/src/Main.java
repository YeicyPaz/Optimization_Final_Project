//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.


import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        //normalement si tu as la même structure de dossier que moi ça devrait marcher
        String doc = "untitled/instances/Inst1.txt";
        Instance.readFile(doc);

        Instance.getHotels().forEach(site -> {

            System.out.println("x: "+ site.coordinates.x + " y: " + site.coordinates.y);
            //System.out.println("score: "+ site.score);

        });

        System.out.println("Distance max total: " + Instance.tMax + ", Distance par jour: "+ java.util.Arrays.toString(Instance.tD));
        System.out.println("-----------------------------------------------");
        System.out.println("---Distance test---");

        Day day1 = new Day();
        System.out.println("day1 distance before update: " + day1.dailyDistance);
        day1.setChosenHotel(Instance.hotels.get(0));
        day1.addChosenSites(Instance.touristicSites.get(0));
        day1.updateDistance(instance);
        System.out.println("day1 distance after update: " + day1.dailyDistance);
        day1.setChosenHotel(Instance.hotels.get(0));
        day1.addChosenSites(Instance.touristicSites.get(0));
        day1.updateDistance(instance);
        System.out.println("day1 distance 2eme update: " + day1.dailyDistance);


        /*for (int i = 0; i < Instance.tD[0]; i++ ){
            if(i<Instance.tD[0]){
                System.out.println("day1 distance : " + day1.dailyDistance);
                day1.setChosenHotel(Instance.hotels.get(0));
                day1.addChosenSites(Instance.touristicSites.get(0));
                day1.updateDistance();

            }
        }
        */

        System.out.println("-------------------------------------------------");
        System.out.println("last hotel point X: " + Instance.getLastHotel().getCoordinates().getX()+ " Y: "+Instance.getLastHotel().getCoordinates().getY());


        System.out.println("-------------------------------------------------");
        System.out.println("---Tree skeleton + pruning test---");
        Schedule schedule1 = new Schedule();
        schedule1.generateHotelPaths();
        // j'ai ajouté un ID dans les sites comme ça on peut affiché H0 H1 H2
        for (int i = 0; i < schedule1.validHotelPaths.size(); i++) {
            System.out.print("path number " + i + ": ");
            for (int j = 0; j < Instance.d+1 ; j++) {
                System.out.print(schedule1.validHotelPaths.get(i).get(j).id+" ");
            }
            System.out.println();
        }
        /* To retrieve the list of hotel object (pour Yeicy)
        for (List<Hotel> validHotelPaths: schedule1.validHotelPaths
             ) {
            System.out.println(validHotelPaths);
        }
         */
    }
}