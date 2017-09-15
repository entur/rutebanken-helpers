/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 */

package org.rutebanken.helper.jms.batch;

import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQObjectMessage;

import javax.jms.JMSException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ActiveMQObjectMessage wrapping a list of JMS messages.
 */
public class JmsBatchMessage extends ActiveMQObjectMessage implements Serializable {

	private List<ActiveMQMessage> messages;

	public JmsBatchMessage(List<ActiveMQMessage> messages) {
		this.messages = messages;
		messages.get(0).copy(this);
	}


	@Override
	public void setObject(Serializable newObject) throws JMSException {
		super.setObject(newObject);
	}

	@Override
	public Serializable getObject() throws JMSException {
		return new ArrayList<>(messages);
	}


}
