package org.example.loader;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.example.loader.events.DataLoader;
import org.example.loader.events.FailEvent;
import org.example.loader.events.LoaderTask;
import org.example.loader.events.SuccessEvent;
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
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.slf4j.LoggerFactory.getLogger;

public class Loader implements DataLoader {
    public static final long MAX_WAITING_TIME = TimeUnit.SECONDS.toMillis(5);
    private static final Logger log = getLogger(Loader.class);

    private final Executor loaderExecutor;
    private final ScheduledExecutorService taskExecutor;

    private final BlockingQueue<LoaderTask> taskQueue = new PriorityBlockingQueue<>();

    private final Map<String, HttpCookie> cookies;

    @Getter
    private final AtomicLong lastMessageTime = new AtomicLong();

    public Loader() {
        this.loaderExecutor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("loader-task", 0).factory());
        this.taskExecutor = Executors.newScheduledThreadPool(1, Thread.ofVirtual().name("loader-checker", 0).factory());
        cookies = new ConcurrentHashMap<>();
    }

    public LoaderTask addTask(Link link) {
        LoaderTask event = new LoaderTask(link);
        taskQueue.add(event);
        return event;
    }

    @Override
    public void startUp() {
        taskExecutor.scheduleWithFixedDelay(this::runTask, 50L, 50L, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutDown() {
        taskExecutor.shutdownNow();
    }

    public boolean isWorking() {
        return !taskQueue.isEmpty() || !taskExecutor.isShutdown();
    }

    @Override
    public void runTask() {
        LoaderTask loaderTask = taskQueue.poll();
        if (loaderTask == null) {
            long lastMessage = lastMessageTime.get();
            if (lastMessage != 0 && lastMessage < System.currentTimeMillis() - MAX_WAITING_TIME) {
                log.info("Stopping execution cause no more tasks");
                shutDown();
            }
            return;
        }
        lastMessageTime.set(System.currentTimeMillis());
        Link link = loaderTask.getLink();
        initLoader(link);

        CompletableFuture.runAsync(() -> {
            try {
                URL taskUrl = new URI(link.buildUrl()).toURL();
                HttpURLConnection urlConnection = (HttpURLConnection) taskUrl.openConnection();

                initConnection(urlConnection, link);
                urlConnection.setRequestMethod(link.getMethod());

                if (link.getBody() != null) {
                    urlConnection.setDoOutput(true);
                    try (OutputStream os = urlConnection.getOutputStream();
                         OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                        osw.write(link.getBody());
                        osw.flush();
                    }
                }

                log.info("[{}] {}", link.getMethod(), link.buildUrl());
                int responseCode = urlConnection.getResponseCode();
                if (is2xxCode(responseCode)) {
                    handleConnection(urlConnection);
                    InputStream response = urlConnection.getInputStream();

                    SuccessEvent successEvent = new SuccessEvent(this, link, new String(response.readAllBytes(), StandardCharsets.UTF_8));
                    loaderTask.complete(successEvent);
                } else if (is4xxCode(responseCode)) {
                    InputStream response = urlConnection.getErrorStream();

                    FailEvent successEvent = new FailEvent(this, link, new String(response.readAllBytes()), getExceptionForStatus(responseCode));
                    loaderTask.complete(successEvent);
                } else {
                    String responseMessage = urlConnection.getResponseMessage();

                    FailEvent successEvent = new FailEvent(this, link, responseMessage, getExceptionForStatus(responseCode));
                    loaderTask.complete(successEvent);
                }


            } catch (IOException | URISyntaxException e) {
                loaderTask.complete(new FailEvent(this, link, e.getMessage(), e));
            }
        }, loaderExecutor);
    }


    private void initLoader(Link link) {
        link.getCookies().values()
              .forEach(cookie -> cookies.put(cookie.key(), new HttpCookie(cookie.key(), cookie.value())));
    }

    private void initConnection(HttpURLConnection urlConnection, Link link) {
        String cookies = getCookieString();
        if (StringUtils.isNoneBlank(cookies)) {
            urlConnection.setRequestProperty(ModelConstants.COOKIE_HEADER, cookies);
        }
        link.getHeaders().forEach(urlConnection::setRequestProperty);
    }

    private void handleConnection(HttpURLConnection connection) {
        connection.getHeaderFields().forEach((s, values) -> {
            if (StringUtils.equalsIgnoreCase(s, ModelConstants.SET_COOKIE_HEADER)) {
                for (String cookieString : values) {
                    String[] split = cookieString.split("=");
                    String key = split[0];
                    String value = split[1];
                    if (value.endsWith(";")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    this.cookies.put(key, new HttpCookie(key, value));
                }
            }
        });
    }

    private String getCookieString() {
        StringBuilder cookieString = new StringBuilder();
        cookies.values()
              .forEach(cookie -> cookieString.append(cookie.getName()).append("=").append(cookie.getValue()).append(';'));
        return cookieString.toString();
    }

    private static boolean is2xxCode(int responseCode) {
        return responseCode / 100 == 2;
    }

    private static boolean is4xxCode(int responseCode) {
        return responseCode / 100 == 4;
    }

    private Throwable getExceptionForStatus(int statusCode) {
        return switch (statusCode) {
            case 403 -> new ForbiddenException();
            case 401 -> new AuthorizedException();
            default -> new NotFoundException();
        };
    }
}
