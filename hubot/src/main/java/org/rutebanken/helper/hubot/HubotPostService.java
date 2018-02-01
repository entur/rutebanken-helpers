package org.rutebanken.helper.hubot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;


@Service
public class HubotPostService {
    private static final Logger log = LoggerFactory.getLogger(HubotPostService.class);

    private final String hubotEndpoint;

    @Autowired
    public HubotPostService(String hubotEndpoint) {
        this.hubotEndpoint = hubotEndpoint;
    }

    public boolean publish( String message ) {
        HubotMessage msg = new HubotMessage(message, "MESSAGE", "OK", new Date().toString());

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            String json = new ObjectMapper().writeValueAsString(msg);
            log.debug("Will try to send the following to hubot: "+json);

            HttpPost httpPost = new HttpPost(hubotEndpoint);
            httpPost.setEntity(new StringEntity(json));
            httpPost.addHeader("Content-Type", "application/json");

            try ( CloseableHttpResponse response = httpclient.execute(httpPost) ) {
                HttpEntity entity = response.getEntity();
                log.info("Entity as response from hubot: "+EntityUtils.toString(entity));
                EntityUtils.consume(entity);
            }

            return true;
        } catch (IOException e) {
            log.error("Could not parse object", e);
            return false;
        }
    }

    public static class HubotMessage {
        private final String contents;
        private final String action;
        private final String status;
        private final String date;

        public HubotMessage(String contents, String action, String status, String date) {
            this.contents = contents;
            this.action = action;
            this.status = status;
            this.date = date;
        }

        public String getContents() {
            return contents;
        }

        public String getAction() {
            return action;
        }

        public String getStatus() {
            return status;
        }

        public String getDate() {
            return date;
        }
    }
}
