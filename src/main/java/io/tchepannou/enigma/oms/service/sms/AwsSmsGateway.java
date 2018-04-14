package io.tchepannou.enigma.oms.service.sms;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

public class AwsSmsGateway implements SmsGateway {
    private static final int SENDER_MAX_LEN = 11;
    private static final int MESSAGE_MAX_LEN = 160;

    @Autowired
    private AmazonSNS sns;

    @Override
    public SendSmsResponse send(final SendSmsRequest request) {
        final String sender = request.getSenderId();
        final String message = request.getMessage();
        final String phone = request.getPhone();

        if (Strings.isNullOrEmpty(message)){
            return new SendSmsResponse();
        }

        final Map<String, MessageAttributeValue> attrs = attributes(sender);
        final PublishResult result = sns.publish(new PublishRequest()
                .withMessage(formatMessage(message))
                .withPhoneNumber(formatPhone(phone))
                .withMessageAttributes(attrs));

        final SendSmsResponse response = new SendSmsResponse();
        response.setMessageId(result.getMessageId());
        return response;
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

    private String formatSenderId(String sender) {
        if (sender == null){
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sender.length(); i++) {
            char ch = sender.charAt(i);
            if (Character.isDigit(ch) || Character.isAlphabetic(ch)) {
                sb.append(ch);
            }
        }
        return sb.length() > SENDER_MAX_LEN ? sb.substring(0, SENDER_MAX_LEN) : sb.toString();
    }

    private String formatMessage(String message){
        return message.length() > MESSAGE_MAX_LEN ? message.substring(0, MESSAGE_MAX_LEN) : message;
    }

    private Map<String, MessageAttributeValue> attributes(final String sender) {
        Map<String, MessageAttributeValue> smsAttributes = new HashMap<>();

        if (sender != null) {
            smsAttributes.put("AWS.SNS.SMS.SenderID", new MessageAttributeValue()
                    .withStringValue(formatSenderId(sender))
                    .withDataType("String"));
        }

        smsAttributes.put("AWS.SNS.SMS.SMSType", new MessageAttributeValue()
                .withStringValue("Transactional") //Sets the type to promotional.
                .withDataType("String"));

        return smsAttributes;
    }
}
