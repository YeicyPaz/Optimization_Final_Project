//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import java.io.*;


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

        System.out.println("--Distance test--");
        Day day1 = new Day();
        System.out.println("day1 distance before update :" + day1.dailyDistance);
        day1.setChosenHotel(Instance.hotels.get(0));
        day1.addChosenSites(Instance.touristicSites.get(0));
        day1.updateDistance();
        System.out.println("day1 distance after update :"+ day1.dailyDistance);

        System.out.println("last hotel"+ Instance.getLastHotel()+ "coordonate X"+ Instance.getLastHotel().getCoordinates().getX()+ " Y: "+Instance.getLastHotel().getCoordinates().getY());
        for (Hotel hotel: Instance.hotels
             ) {
            System.out.println("hotel"+ hotel + " point X " +hotel.getCoordinates().getX() + " point y "+hotel.getCoordinates().getY());
        }
        System.out.println("first hotel"+ Instance.getFirstHotel()+ "coordonate X"+ Instance.getFirstHotel().getCoordinates().getX()+ " Y: "+Instance.getFirstHotel().getCoordinates().getY());

    }
}