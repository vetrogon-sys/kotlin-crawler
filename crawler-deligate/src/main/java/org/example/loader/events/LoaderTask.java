package org.example.loader.events;

import lombok.Getter;
import org.example.loader.Link;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LoaderTask extends CompletableFuture<LoaderEvent> implements Comparable<LoaderTask> {

    @Getter
    private final Link link;

    public LoaderTask(Link link) {
        this.link = link;
    }

    private Consumer<SuccessEvent> successEventConsumer;
    private Consumer<FailEvent> failEventConsumer;

    public LoaderTask onSuccess(Consumer<SuccessEvent> seConsumer) {
        successEventConsumer = seConsumer;
        return this;
    }

    public LoaderTask onFail(Consumer<FailEvent> feConsumer) {
        failEventConsumer = feConsumer;
        return this;
    }

    @Override
    public boolean complete(LoaderEvent loaderEvent) {
        if (successEventConsumer != null && loaderEvent instanceof SuccessEvent se) {
            successEventConsumer.accept(se);
        } else if (failEventConsumer != null && loaderEvent instanceof FailEvent fe) {
            failEventConsumer.accept(fe);
        }
        return super.complete(loaderEvent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LoaderTask that)) {
            return false;
        }

        return Objects.equals(link, that.link);
    }

    @Override
    public int hashCode() {
        return link != null ? link.hashCode() : 0;
    }

    @Override
    public int compareTo(LoaderTask o) {
        return Integer.compare(hashCode(), o.hashCode());
    }
}
