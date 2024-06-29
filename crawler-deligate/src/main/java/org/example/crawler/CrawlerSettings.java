package org.example.crawler;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CrawlerSettings {

    int unitCount() default 1;

}
