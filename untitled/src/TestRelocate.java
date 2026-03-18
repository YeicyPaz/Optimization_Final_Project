import java.io.IOException;
import java.util.List;

public class TestRelocate {
    public static void main(String[] args) throws IOException {

        String doc = "untitled/instances/Inst1.txt";
        Instance.readFile(doc);
        Schedule schedule = new Schedule();
        schedule.chooseHotelPath();

    }
}
