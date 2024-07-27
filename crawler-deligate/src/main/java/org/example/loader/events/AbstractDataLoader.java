package org.example.loader.events;

import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.loader.Link;
import org.example.loader.Loader;
import org.example.loader.exceptions.AuthorizedException;
import org.example.loader.exceptions.ForbiddenException;
import org.example.loader.exceptions.NotFoundException;
import org.example.model.ModelConstants;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class AbstractDataLoader implements DataLoader {

    public static final long MAX_WAITING_TIME = TimeUnit.SECONDS.toMillis(5);
    private static final Logger log = getLogger(Loader.class);

    protected final Executor loaderExecutor;
    protected final ScheduledExecutorService taskExecutor;

    protected final long pauseRequests;
    protected final int limitRequest;

    protected final BlockingQueue<LoaderTask> taskQueue = new PriorityBlockingQueue<>();

    protected final Map<String, HttpCookie> cookies;

    protected final Set<String> notSuitableValues = ConcurrentHashMap.newKeySet();

    @Getter
    protected final AtomicLong lastMessageTime = new AtomicLong();

    public AbstractDataLoader(long pauseRequest, int limitRequest) {
        this.loaderExecutor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("loader-", 0).factory());
        this.taskExecutor = Executors.newScheduledThreadPool(1, Thread.ofVirtual().name("loader-checker", 0).factory());
        cookies = new ConcurrentHashMap<>();
        this.pauseRequests = pauseRequest == 0L ? 1L : pauseRequest;
        this.limitRequest = limitRequest;
    }

    @Override
    public LoaderTask addTask(Link link) {
        LoaderTask event = new LoaderTask(link);
        taskQueue.add(event);
        return event;
    }

    @Override
    public boolean isSuitable(String value) {
        if (notSuitableValues.contains(value)) {
            return false;
        }
        return notSuitableValues.add(value);
    }

    @Override
    public void startUp() {
        taskExecutor.scheduleWithFixedDelay(this::runTaskGroup, 500L, pauseRequests, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutDown() {
        taskExecutor.shutdownNow();
        if (!taskQueue.isEmpty()) {
            taskQueue.forEach(loaderTask -> log.info("CANCEL [{}] {}", loaderTask.getLink().getMethod(), loaderTask.getLink().buildUrl()));
        }
    }

    @Override
    public boolean isDone() {
        return !taskQueue.isEmpty() || !taskExecutor.isShutdown();
    }

    public void runTaskGroup() {
        for (int i = 0; i < limitRequest; i++) {
            LoaderTask loaderTask = taskQueue.poll();
            if (loaderTask == null) {
                long lastMessage = lastMessageTime.get();
                if (lastMessage != 0 && lastMessage < System.currentTimeMillis() - MAX_WAITING_TIME) {
                    log.info("Stopping execution cause no more tasks");
                    shutDown();
                }
                break;
            }
            lastMessageTime.set(System.currentTimeMillis());
            if (!runTask(loaderTask)) {
                break;
            }
        }
    }

    protected void initLoader(Link link) {
        link.getCookies().values()
              .forEach(cookie -> cookies.put(cookie.key(), new HttpCookie(cookie.key(), cookie.value())));
    }

    protected void initConnection(HttpURLConnection urlConnection, Link link) {
        String cookies = getCookieString();
        if (StringUtils.isNoneBlank(cookies)) {
            urlConnection.setRequestProperty(ModelConstants.COOKIE_HEADER, cookies);
        }
        link.getHeaders().forEach(urlConnection::setRequestProperty);
    }

    protected static void logLink(Link link) {
        log.info("[{}] {}", link.getMethod(), link.buildUrl());
    }

    protected String getCookieString() {
        StringBuilder cookieString = new StringBuilder();
        cookies.values()
              .forEach(cookie -> cookieString.append(cookie.getName()).append("=").append(cookie.getValue()).append(';'));
        return cookieString.toString();
    }

    protected static boolean is2xxCode(int responseCode) {
        return responseCode / 100 == 2;
    }

    protected static boolean is4xxCode(int responseCode) {
        return responseCode / 100 == 4;
    }

    protected Throwable getExceptionForStatus(int statusCode) {
        return switch (statusCode) {
            case 403 -> new ForbiddenException();
            case 401 -> new AuthorizedException();
            default -> new NotFoundException();
        };
    }

}
