package org.rutebanken.helper.jms.batch;

import org.apache.camel.component.jms.JmsEndpoint;
import org.apache.camel.component.jms.MessageListenerContainerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.stereotype.Service;


@Service
public class BatchListenerContainerFactory implements MessageListenerContainerFactory {

    @Value("${rutebanken.jms.batch.size:100}")
    private int batchSize;


    public BatchListenerContainerFactory() {
    }

    public BatchListenerContainerFactory(int batchSize) {
        this.batchSize = batchSize;
    }


    @Override
    public AbstractMessageListenerContainer createMessageListenerContainer(JmsEndpoint jmsEndpoint) {
        return new BatchMessageListenerContainer(batchSize);
    }
}
