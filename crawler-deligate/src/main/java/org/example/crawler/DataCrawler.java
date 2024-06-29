package org.example.crawler;

import org.example.loader.Link;
import org.example.loader.Loader;
import org.example.loader.events.SuccessEvent;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class DataCrawler implements Runnable {
    private static final Logger log = getLogger(DataCrawler.class);
    private long startScanTime;
    private final Loader loader;

    public DataCrawler(Loader loader) {
        this.loader = loader;
    }

    protected abstract void handleSuccessStart(SuccessEvent successEvent);

    public void beforeStart() {
        startScanTime = System.currentTimeMillis();
        log.info("Start scanning data {}", startScanTime);
    }

    public void onComplete() {
        loader.shutDown();
        log.info("End scan, running time {}ms", System.currentTimeMillis() - startScanTime);
    }

    public boolean isSuitable(String value) {
        return loader.isSuitable(value);
    }

    public Link createStartLink() {
        return Link.EMPTY_URL;
    }

    @Override
    public void run() {
        beforeStart();

        loader.addTask(createStartLink())
              .onSuccess(this::handleSuccessStart);

        loader.startUp();

        while (true) {
            if (!loader.isWorking()) {
                break;
            }
        }

        onComplete();
    }

    public long maxUnitWorkingTime() {
        return TimeUnit.MINUTES.toMillis(1);
    }

}
