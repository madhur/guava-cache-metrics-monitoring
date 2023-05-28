package in.co.madhur.controller;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.GuavaCacheMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/test")
public class GuavaTest {


    private static final Logger logger = LoggerFactory.getLogger(GuavaTest.class);
    public static final int RELOAD_EXECUTOR_SIZE = 2;

    private LoadingCache<Integer, MatchListDO> sportsTypeVsMatchesLocalCache;

    private ListeningExecutorService allMatchDetailsReloadExecService;

    @Autowired
    private MatchContestManager matchContestManagementUtil;

    @Autowired
    private MeterRegistry meterRegistry;

    @PostConstruct
    public void init() {
        allMatchDetailsReloadExecService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(RELOAD_EXECUTOR_SIZE));
        initializeAllMatchesCache();
    }

    private void initializeAllMatchesCache() {
        try {
            sportsTypeVsMatchesLocalCache = CacheBuilder.newBuilder()
                    .expireAfterWrite(2, TimeUnit.SECONDS)
                    .refreshAfterWrite(1,
                            TimeUnit.SECONDS)
                    .maximumSize(5)
                    .recordStats()
                    .build(new CacheLoader<>() {
                        @Override
                        public MatchListDO load(Integer sportsType) {
                            try {
                                MatchListDO matchListDO = matchContestManagementUtil.createAndPersistAllMatchesJson(
                                        sportsType);
                                System.out.println("Loading");
                                return matchListDO;
                            } catch (Exception e) {
                                logger.error(
                                        "Exception in initializeMatchesCache cache loader for sportsType:"
                                                + sportsType, e);
                                return null;
                            }
                        }

                        @Override
                        public ListenableFuture<MatchListDO> reload(Integer sportsType, MatchListDO oldValue) {
                            return allMatchDetailsReloadExecService.submit(() -> {
                                try {
                                    MatchListDO matchListDO = matchContestManagementUtil.createAndPersistAllMatchesJson(
                                            sportsType);
                                    System.out.println("Reloading");
                                    return matchListDO;
                                } catch (Exception e) {
                                    logger.error(
                                            "Exception in initializeMatchesCache cache loader for sportsType:"
                                                    + sportsType, e);
                                    return null;
                                }
                            });
                        }
                    });


            GuavaCacheMetrics.monitor(meterRegistry, sportsTypeVsMatchesLocalCache, "sportsTypeVsMatchesLocalCache");
        } catch (Exception e) {
            logger.error(" Error while initializing local cache for matches. due to {}");
            e.printStackTrace();
        }
    }

    @GetMapping("/matches/{sportsType}")
    public ResponseEntity<MatchListDO> getMatches(@PathVariable(value = "sportsType") Integer sportsType) {

        try {
            MatchListDO matchListDO =  sportsTypeVsMatchesLocalCache.get(sportsType);
            return new ResponseEntity<>(matchListDO, HttpStatus.ACCEPTED);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }


}
