//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import java.io.*;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        String doc = "untitled/instances/Inst1.txt";
        Instance.readFile(doc);
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
        for (Day day: schedule1.schedule
             ) {
            System.out.println("Daily distance: "+day.dailyDistance);
            System.out.println("Daily score: "+day.dailyScore);

        }
        System.out.println("score total = "+schedule1.totalScore(schedule1.schedule));
    }
}