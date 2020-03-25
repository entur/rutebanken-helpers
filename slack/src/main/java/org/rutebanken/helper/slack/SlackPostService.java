package org.rutebanken.helper.slack;

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
public class SlackPostService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SlackPostService.class);

    private final String slackEndpoint;

    @Autowired
    public SlackPostService(@Value("${helper.slack.endpoint}") String slackEndpoint) {
        this.slackEndpoint = slackEndpoint;
    }

    public boolean publish(String messageText) {
        SlackPayload payload = new SlackPayload(messageText);
        return publish(payload);
    }

    public boolean publish(SlackPayload payload) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            String json = new ObjectMapper().writeValueAsString(payload);
            LOGGER.debug("Will try to send the following to slack: {}", json);

            HttpPost httpPost = new HttpPost(slackEndpoint);
            httpPost.setEntity(new StringEntity(json));
            httpPost.addHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
                HttpEntity entity = response.getEntity();
                if(LOGGER.isInfoEnabled()) {
                    LOGGER.info("Entity as response from hubot: {}", EntityUtils.toString(entity));
                }
                EntityUtils.consume(entity);
            }

            return true;
        } catch (IOException e) {
            LOGGER.error("Could not parse object", e);
            return false;
        }
    }

    public static class SlackPayload {

        private final String text;

        public SlackPayload(String text) {
            this.text= text;
        }

        public String getText() {
            return text;
        }

    }
}
