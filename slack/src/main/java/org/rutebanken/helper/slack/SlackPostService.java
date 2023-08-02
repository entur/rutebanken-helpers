package org.rutebanken.helper.slack;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SlackPostService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SlackPostService.class);

    private final String slackEndpoint;
    private final ObjectMapper objectMapper;

    @Autowired
    public SlackPostService(@Value("${helper.slack.endpoint}") String slackEndpoint) {
        this.slackEndpoint = slackEndpoint;
        objectMapper = new ObjectMapper();
    }

    public boolean publish(String messageText) {
        SlackPayload payload = new SlackPayload(messageText);
        return publish(payload);
    }

    public boolean publish(SlackPayload payload) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            String json = objectMapper.writeValueAsString(payload);
            LOGGER.debug("Will try to send the following to slack: {}", json);
            HttpPost httpPost = new HttpPost(slackEndpoint);
            httpPost.setEntity(new StringEntity(json));
            httpPost.addHeader("Content-Type", "application/json");
            httpclient.execute(httpPost, response -> {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Entity as response from hubot: {}", EntityUtils.toString(response.getEntity()));
                }
                return null;
            });
            return true;
        } catch (Exception e) {
            LOGGER.error("Could not publish message", e);
            return false;
        }
    }

    public record SlackPayload(String text) {}
}
