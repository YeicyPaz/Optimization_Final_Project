import java.util.*;
public class Planning {


    ArrayList<Day> planning = new ArrayList<>();
    int nbJours = Instance.d;
    Hotel firstHotel = Instance.hotels.get(0);
    Hotel lastHotel = Instance.hotels.get(nbJours); // à vérifier

    /*
    faire une fonction check planning qui vérifie que dans chaque jour on ne dépasse pas la distance de trajet max par jour
    vérifier que on visite le site touristique pendant l'horaire d'ouverture (après l'ouveture et avant la fermeture)
    ??
     */
    /*
    le temps = distance parcouru

    changer ArrayList<ArrayList<Site>> planning = new ArrayList<>();
    en ArrayList<Journée> planning = new ArrayList<>();
    Journée -> array list des sites et hotels + distance parcourus
                booleen siteVisité?
     */

    /*
    faire récursivement
    regarder tout les chemins possibles d'hotels (prenant en compte seulement les hotels)
    dans un arbre on construit les différentes possibilités
    en effacant celle qui épuise la distance de parcours avant d'arriver à l'hotel final
    matrices avec les différents chemins d'hotels (qui respecte la distance)
     */
    public void createPlanning(){
        for (int d = 0; d < nbJours; d++) {
            planning.add(new Day());
        }
        // ajouter le premier hotel et dernier en tant que

    }

}
