package org.example.loader.events;

import org.example.loader.Link;

import java.util.function.Consumer;

public interface DataLoader {

    LoaderTask addTask(Link link);

    boolean isSuitable(String value);

    void startUp();

    void shutDown();

    boolean runTask(LoaderTask loaderTask);

    boolean isDone();

}
