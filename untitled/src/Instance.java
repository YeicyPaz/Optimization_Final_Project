import java.io.*;
import java.util.*;

public class Instance {

    int n;
    int h;
    int d;
    int tMax;
    int tD;

    ArrayList<Day> days;

    static ArrayList<Hotel> hotels;
    static ArrayList<TouristicSite> touristicSites;
    public static void readFile(String filePath) throws IOException {
        hotels = new ArrayList<>();
        touristicSites = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line;

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
}
