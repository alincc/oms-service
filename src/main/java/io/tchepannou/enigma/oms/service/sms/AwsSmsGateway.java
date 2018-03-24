package io.tchepannou.enigma.oms.service.sms;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

public class AwsSmsGateway implements SmsGateway {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSmsGateway.class);

    @Autowired
    private AmazonSNS sns;

    @Override
    public String send(final String sender, final String phone, final String message) {
        final Map<String, MessageAttributeValue> attrs = attributes(sender);
        final PublishResult result = sns.publish(new PublishRequest()
                .withMessage(message)
                .withPhoneNumber(formatPhone(phone))
                .withMessageAttributes(attrs));
        return result.getMessageId();
    }

    private String formatPhone(String phone) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < phone.length(); i++) {
            char ch = phone.charAt(i);
            if (Character.isDigit(ch)) {
                sb.append(ch);
            }
        }
        return "+1" + sb.toString();
    }

    private Map<String, MessageAttributeValue> attributes(final String sender) {
        Map<String, MessageAttributeValue> smsAttributes = new HashMap<>();

        if (sender != null) {
            smsAttributes.put("AWS.SNS.SMS.SenderID", new MessageAttributeValue()
                    .withStringValue(sender)
                    .withDataType("String"));
        }

        smsAttributes.put("AWS.SNS.SMS.SMSType", new MessageAttributeValue()
                .withStringValue("Transactional") //Sets the type to promotional.
                .withDataType("String"));

        return smsAttributes;
    }
}
