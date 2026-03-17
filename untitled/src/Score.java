public class Score {
    /* Classe score pour pouvoir avoir une liste d'objet tuple
     * ex: [(Hotel1, Score), (Hotel2, Score), ... ]
     * ou  [(Hotel1, Score1, Score2), (Hotel2, Score1, Score2), ... ]
     */
    Site site;
    Double score1;
    Double score2;
    Double score3;
    Double score4;
    public Score(Site site, Double score1){
        this.site = site;
        this.score1 = score1;
    }
    public Score(Site site, Double score1,Double score2,Double score3, Double score4){
        this.site = site;
        this.score1 = score1;
        this.score2 = score2;
        this.score3 = score3;
        this.score4 = score4;

    }
}
