import java.util.ArrayList;

public class Solution {
    // Una lista de días. Cada día tiene una lista de sitios (Sitios o Hoteles)
    public ArrayList<ArrayList<Site>> days;
    public double totalScore;

    public Solution() {
        this.days = new ArrayList<>();
        this.totalScore = 0;
    }
}