package io.tchepannou.enigma.oms.service.sms;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

public class AwsSmsGateway implements SmsGateway {
    @Autowired
    private AmazonSNS sns;

    @Override
    public String send(final String phone, final String message) {
        final Map<String, MessageAttributeValue> attrs = new HashMap<>();
        final PublishResult result = sns.publish(new PublishRequest()
                .withMessage(message)
                .withPhoneNumber(formatPhone(phone))
                .withMessageAttributes(attrs));
        return result.getMessageId();
    }

    private String formatPhone(String phone){
        StringBuilder sb = new StringBuilder();
        for (int i=0  ; i<phone.length() ; i++){
            char ch = phone.charAt(i);
            if (Character.isDigit(ch)){
                sb.append(ch);
            }
        }
        return "+1" + sb.toString();
    }
}
