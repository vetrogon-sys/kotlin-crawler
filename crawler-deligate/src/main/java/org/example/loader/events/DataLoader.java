package org.example.loader.events;

public interface DataLoader {

    void startUp();

    void shutDown();

    boolean runTask();

}
