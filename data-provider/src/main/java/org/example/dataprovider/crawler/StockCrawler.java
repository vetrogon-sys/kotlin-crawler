package org.example.dataprovider.crawler;

import org.example.crawler.DataCrawler;
import org.example.loader.Loader;
import org.example.loader.events.SuccessEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class StockCrawler extends DataCrawler {

    private Map<String, Stock> currentStocks = new ConcurrentHashMap<>();

    public StockCrawler(Loader loader) {
        super(loader);
    }

    @Override
    protected void handleSuccessStart(SuccessEvent successEvent) {

    }

    protected void putStock(Stock stock) {
        currentStocks.put(stock.getId(), stock);
    }

    @Override
    public void onComplete() {
        super.onComplete();

        //
    }
}
