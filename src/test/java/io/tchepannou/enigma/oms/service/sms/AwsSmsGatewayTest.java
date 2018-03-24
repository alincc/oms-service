package io.tchepannou.enigma.oms.service.sms;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AwsSmsGatewayTest {
    @Mock
    private AmazonSNS sns;

    @InjectMocks
    private AwsSmsGateway gateway;

    @Test
    public void send() throws Exception {
        PublishResult expected = mock(PublishResult.class);
        when(expected.getMessageId()).thenReturn("this-is-message-id");
        when (sns.publish(any())).thenReturn(expected);

        // When
        String result = gateway.send("Ray", "(514)544-11 11", "Hello world");

        // Then
        assertThat(result).isEqualTo("this-is-message-id");

        ArgumentCaptor<PublishRequest> request = ArgumentCaptor.forClass(PublishRequest.class);
        verify(sns).publish(request.capture());
        assertThat(request.getValue().getMessage()).isEqualTo("Hello world");
        assertThat(request.getValue().getPhoneNumber()).isEqualTo("+15145441111");
        assertThat(request.getValue().getMessageAttributes().get("AWS.SNS.SMS.SenderID").getStringValue()).isEqualTo("Ray");
        assertThat(request.getValue().getMessageAttributes().get("AWS.SNS.SMS.SMSType").getStringValue()).isEqualTo("Transactional");
    }

    @Test
    public void sendWithNoSender() throws Exception {
        PublishResult expected = mock(PublishResult.class);
        when(expected.getMessageId()).thenReturn("this-is-message-id");
        when (sns.publish(any())).thenReturn(expected);

        // When
        String result = gateway.send(null, "(514)544-11 11", "Hello world");

        // Then
        assertThat(result).isEqualTo("this-is-message-id");

        ArgumentCaptor<PublishRequest> req = ArgumentCaptor.forClass(PublishRequest.class);
        verify(sns).publish(req.capture());
        assertThat(req.getValue().getMessage()).isEqualTo("Hello world");
        assertThat(req.getValue().getPhoneNumber()).isEqualTo("+15145441111");
        assertThat(req.getValue().getMessageAttributes().containsKey("AWS.SNS.SMS.SenderID")).isFalse();
        assertThat(req.getValue().getMessageAttributes().get("AWS.SNS.SMS.SMSType").getStringValue()).isEqualTo("Transactional");
    }

}
