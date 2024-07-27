package org.example.crawler;

import org.example.loader.HttpClientLoader;
import org.example.loader.Loader;
import org.example.loader.events.DataLoader;
import org.slf4j.Logger;
import sun.reflect.ReflectionFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.slf4j.LoggerFactory.getLogger;

public final class CrawlerUtils {
    private static final Logger log = getLogger(CrawlerUtils.class);

    static final ExecutorService crawlerExecutor;

    static {
        crawlerExecutor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("crawler-", 0).factory());
    }

    public static void runCrawler(Class<? extends DataCrawler> crawlerClass) {
        Constructor<?> crawlerConstructor;
        try {
            crawlerConstructor = crawlerClass.getDeclaredConstructor(DataLoader.class);
        } catch (NoSuchMethodException e) {
            log.error("Cannot find constructor for {}", crawlerClass);
            return;
        }

        CrawlerSettings crawlerSettings = crawlerClass.getAnnotation(CrawlerSettings.class);
        if (crawlerSettings == null) {
            log.error("Cannot find @CrawlerSettings annotation in {}", crawlerClass);
            return;
        }

        List<DataCrawler> units = new ArrayList<>();
        try {

            DataLoader loader = new HttpClientLoader(crawlerSettings.pauseRequest(), crawlerSettings.limitRequest());
            List<CompletableFuture<?>> crawlerFeatures = new ArrayList<>();
            long maxUnitWorkingTime = 0;
            for (int i = 0; i < crawlerSettings.unitCount(); i++) {
                DataCrawler crawler = (DataCrawler) crawlerConstructor.newInstance(loader);

                crawlerFeatures.add(CompletableFuture.runAsync(crawler, crawlerExecutor));
                units.add(crawler);

                if (maxUnitWorkingTime == 0) {
                    maxUnitWorkingTime = crawler.maxUnitWorkingTime();
                }
            }

            if (maxUnitWorkingTime == 0) {
                maxUnitWorkingTime = TimeUnit.MINUTES.toMillis(5);
            }
            for (CompletableFuture<?> crawlerFuture : crawlerFeatures) {
                crawlerFuture.get(maxUnitWorkingTime, TimeUnit.MILLISECONDS);
            }

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log.error("Via try to create crawler", e);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            units.forEach(DataCrawler::onComplete);
            log.error("Cancel cause {}", e.getMessage());
        }
    }
}
