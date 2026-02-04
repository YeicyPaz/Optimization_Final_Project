import java.io.*;
import java.util.*;

public class Instance {

    static int n;          // Vértices
    static int h;          // extra Hotels
    static int d;          // Days
    static double tMax;
    static double[] tD;



    static ArrayList<Hotel> hotels;
    static ArrayList<TouristicSite> touristicSites;
    public static void readFile(String filePath) throws IOException {
        hotels = new ArrayList<>();
        touristicSites = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line;

        //premier line

        if ((line = br.readLine()) != null) {
            line = line.trim();

            String[] parts =line.split("\\s+");

            n = Integer.parseInt(parts[0]); //nombre des nodes(vertices)
            h = Integer.parseInt(parts[1]); //les hotels du voyage sans l'hotel de debut et fin qui sont fixes
            d = Integer.parseInt(parts[2]); // jours de voyage(numbers of trips)

            System.out.println("Nodes: " + n + ", Hotels : " + h + ", Days: " + d);

        }

      //tmax
        if ((line = br.readLine()) != null){
            tMax = Double.parseDouble(line.trim());
        }

     //troisieme line

        if ((line = br.readLine()) != null){
            line=line.trim();
            String[] parts =line.split("\\s+");

            tD = new double[d];

            for (int i = 0; i < d; i++) {
                tD[i] = Double.parseDouble(parts[i]);
            }


        }



        while ((line = br.readLine()) != null) {
            line = line.trim();

            if (line.startsWith("H")) {

                double hotelX = Double.parseDouble(line.split("\\s+")[1].trim());
                double hotelY = Double.parseDouble(line.split("\\s+")[2].trim());
                double hotelClosingT = Double.parseDouble(line.split("\\s+")[6].trim());


                hotels.add(new Hotel(new Point(hotelX, hotelY), hotelClosingT));

            }
            else if (line.startsWith("C")) {

                double touristSiteX = Double.parseDouble(line.split("\\s+")[1].trim());
                double touristSiteY = Double.parseDouble(line.split("\\s+")[2].trim());
                int touristSiteScore = Integer.parseInt(line.split("\\s+")[3].trim());
                double touristSiteOpeningT = Double.parseDouble(line.split("\\s+")[5].trim());
                double touristSiteClosingT = Double.parseDouble(line.split("\\s+")[6].trim());

                touristicSites.add(new TouristicSite(new Point(touristSiteX, touristSiteY), touristSiteScore, touristSiteOpeningT,touristSiteClosingT));

                }

        }


    }

    public static ArrayList<TouristicSite> getTouristicSites() {
        return touristicSites;
    }
    public static ArrayList<Hotel> getHotels(){ return hotels;}
}
