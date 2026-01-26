//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import java.io.*;


import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        //normalement si tu as la même structure de dossier que moi ça devrait marcher
        String doc = "untitled/instances/Inst1.txt";
        Instance.readFile(doc);

        Instance.getTouristicSites().forEach(site -> {

            System.out.println("x: "+ site.coordinates.x + " y: " + site.coordinates.y);
            System.out.println("score: "+ site.score);
        });

    }
}