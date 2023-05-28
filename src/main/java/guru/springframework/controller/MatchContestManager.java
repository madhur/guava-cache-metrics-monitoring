package guru.springframework.controller;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MatchContestManager {

    private Long startingMatchId = 1L;

    public MatchListDO createAndPersistAllMatchesJson(Integer sportsType) {

        MatchListDO matchListDO = new MatchListDO();
        List<Long> matchList = new ArrayList<>();
        for( long i= startingMatchId.intValue();i<startingMatchId + 10;++i) {
            matchList.add(i);
        }
        matchListDO.setMatchId(matchList);
        // Simulate sleep of 300ms
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return  matchListDO;

    }


    @Scheduled(fixedRate = 1000)
    public void run() {
        startingMatchId++;
    }
}
