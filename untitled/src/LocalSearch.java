public class LocalSearch {

    public double applyBestImprovement(Schedule schedule){
        double bestGain = 0.0;
        TouristicSite bestSite1 = null;
        TouristicSite bestSite2 = null;

        for (Day day : schedule.travelDays) {
            for (int i = 0; i < day.chosenSites.size(); i++) {

            }
        }

    }

    /*@Override
    public double applyBestImprovement(VRPSolution solution) {
        double bestGain = 0.0;
        Node bestNode1 = null;
        Node bestNode2 = null;

        for (Route r1 : solution.routes) {
            for (Node n1 = r1.startDepot.next; n1 != r1.endDepot; n1 = n1.next) {
                for (Route r2 : solution.routes) {
                    for (Node n2 = r2.startDepot.next; n2 != r2.endDepot; n2 = n2.next) {

                        if (n1.id >= n2.id) continue;

                        if (!check(n1, n2, solution)) continue;

                        double gain = evaluate(n1, n2, solution);

                        //gain négatif = amélioration
                        if (gain < bestGain) {
                            bestGain = gain;
                            bestNode1 = n1;
                            bestNode2 = n2;
                        }
                    }
                }
            }
        }

        if (bestGain < 0.0 && bestNode1 != null && bestNode2 != null) {
            apply(bestNode1, bestNode2, solution);
            return bestGain;
        }

        return 0.0;
    }
*/


}
