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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Service
public class HubotPostService {
    private static final Logger log = LoggerFactory.getLogger(HubotPostService.class);

    private final String hubotEndpoint;

    @Autowired
    public HubotPostService(@Value("${helper.hubot.endpoint:http://hubot/hubot/say/}") String hubotEndpoint) {
        this.hubotEndpoint = hubotEndpoint;
    }

    public boolean publish(String messageText, String source) {
        HubotMessage message = new HubotMessage(messageText, source);
        return publish(message);
    }

    public boolean publish(HubotMessage hubotMessage) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            String json = new ObjectMapper().writeValueAsString(hubotMessage);
            log.debug("Will try to send the following to hubot: " + json);

            HttpPost httpPost = new HttpPost(hubotEndpoint);
            httpPost.setEntity(new StringEntity(json));
            httpPost.addHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
                HttpEntity entity = response.getEntity();
                log.info("Entity as response from hubot: " + EntityUtils.toString(entity));
                EntityUtils.consume(entity);
            }

            return true;
        } catch (IOException e) {
            log.error("Could not parse object", e);
            return false;
        }
    }

    public static class HubotMessage {

        private final String message;
        private final String source;
        private final String icon;

        public HubotMessage(String message, String source) {
            this.message= message;
            this.source = source;
            this.icon = "";
        }

        public HubotMessage(String message, String source, String icon) {
            this.message = message;
            this.source = source;
            this.icon = icon;
        }

        public String getMessage() {
            return message;
        }

        public String getIcon() {
            return icon;
        }

        public String getSource() {
            return source;
        }
    }
}
