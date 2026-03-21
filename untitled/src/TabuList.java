import java.util.LinkedList;
import java.util.Queue;

public class TabuList {
    private Queue<String> tabuList;
    private int maxSize;

    public TabuList(int size) {
        this.tabuList = new LinkedList<>();
        this.maxSize = size;
    }

    public void add(String move) {
        if (tabuList.size() >= maxSize) {
            tabuList.poll(); // Retirer le mouvement le plus ancien
        }
        tabuList.add(move);
        System.out.println("Ajout du mouvement à la liste tabou: " + move);
    }

    public boolean contains(String move) {
        System.out.println("Vérification si le mouvement est tabou: " + move + " -> " + tabuList.contains(move));
        return tabuList.contains(move);
    }
}
