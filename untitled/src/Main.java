//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.


import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        String doc = "untitled/instances/Inst6.txt";
        Instance.readFile(doc);
        Schedule schedule1 = new Schedule();
        schedule1.chooseHotelPath();
        System.out.println(schedule1.bestHotelPath);
        for (Hotel hotel :schedule1.bestHotelPath
             ) {
            System.out.println(hotel.id);
        }

    }
}