package model;

import java.util.List;

public class RankingResult {

    private List<Player> race;
    private List<Player> ranking;

    public RankingResult(List<Player> race, List<Player> ranking) {
        this.race = race;
        this.ranking = ranking;
    }

    public List<Player> getRace() {
        return race;
    }

    public List<Player> getRanking() {
        return ranking;
    }
}
