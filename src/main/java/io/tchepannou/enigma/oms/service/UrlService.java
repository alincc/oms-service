package io.tchepannou.enigma.oms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
public class UrlService {
    private final static Logger LOGGER = LoggerFactory.getLogger(UrlService.class);

    @Async
    public void asyncNavigateTo(final String url) {
        try {
            navigateTo(url);
        } catch (IOException e){
            LOGGER.error("Unable to navigate to {}", url, e);
        }
    }

    public void navigateTo(final String url) throws IOException {
        HttpURLConnection cnn = null;
        try {
            URL myURL = new URL(url);
            cnn = (HttpURLConnection)myURL.openConnection();
            cnn.connect();
        } finally {
            if (cnn != null){
                cnn.disconnect();
            }
        }
    }
}
