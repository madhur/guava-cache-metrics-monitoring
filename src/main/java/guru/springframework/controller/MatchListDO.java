package guru.springframework.controller;


import java.util.List;

public class MatchListDO {

    private List<Long> matchId;

    public List<Long> getMatchId() {
        return matchId;
    }

    public void setMatchId(List<Long> matchId) {
        this.matchId = matchId;
    }

    @Override
    public String toString() {
        return "MatchListDO{" +
                "matchId=" + matchId +
                '}';
    }
}
