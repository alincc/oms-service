package io.tchepannou.enigma.oms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
        int statusCode = -1;
        try {

            // Connect
            cnn = (HttpURLConnection)new URL(url).openConnection();
            cnn.setRequestMethod("GET");
            statusCode = cnn.getResponseCode();

            LOGGER.info("GET {} - {}", url, statusCode);

            // Get Content
            BufferedReader in = new BufferedReader(new InputStreamReader(cnn.getInputStream()));
            try {
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

            } finally{
                in.close();
            }

        } catch (Exception e) {

            LOGGER.info("GET {} - {}", url, statusCode, e);

            throw e;
        } finally {

            if (cnn != null){
                cnn.disconnect();
            }

        }
    }
}
