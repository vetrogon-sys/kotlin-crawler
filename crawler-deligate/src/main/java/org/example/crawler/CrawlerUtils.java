package org.example.crawler;

import org.slf4j.Logger;
import sun.reflect.ReflectionFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
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
        Constructor<?>[] declaredConstructors = crawlerClass.getDeclaredConstructors();
        Constructor<?> crawlerConstructor = null;
        for (Constructor<?> declaredConstructor : declaredConstructors) {
            Parameter[] parameters = declaredConstructor.getParameters();
            if (parameters.length == 0) {
                crawlerConstructor = declaredConstructor;
                break;
            }
        }

        if (crawlerConstructor == null) {
            log.error("Cannot find constructor for {}", crawlerClass);
            return;
        }

        CrawlerSettings crawlerSettings = crawlerClass.getAnnotation(CrawlerSettings.class);
        if (crawlerSettings == null) {
            log.error("Cannot find @CrawlerSettings annotation in {}", crawlerClass);
            return;
        }

        try {

            for (int i = 0; i < crawlerSettings.unitCount(); i++) {
                DataCrawler crawler = (DataCrawler) crawlerConstructor.newInstance();

                CompletableFuture<Void> crawlerFuture = CompletableFuture.runAsync(crawler, crawlerExecutor);

                crawlerFuture.get(crawler.maxUnitWorkingTime(), TimeUnit.MILLISECONDS);
            }

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log.error("Via try to create crawler", e);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
