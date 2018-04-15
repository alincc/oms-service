package io.tchepannou.enigma.oms.service.sms;

import com.amazonaws.AmazonClientException;
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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

        final SendSmsRequest req = new SendSmsRequest();
        req.setSenderId("Ray");
        req.setPhone("(514)544-11 11");
        req.setMessage("Hello world");

        // When
        final SendSmsResponse response = gateway.send(req);

        // Then
        assertThat(response.getMessageId()).isEqualTo("this-is-message-id");

        ArgumentCaptor<PublishRequest> request = ArgumentCaptor.forClass(PublishRequest.class);
        verify(sns).publish(request.capture());
        assertThat(request.getValue().getMessage()).isEqualTo("Hello world");
        assertThat(request.getValue().getPhoneNumber()).isEqualTo("+15145441111");
        assertThat(request.getValue().getMessageAttributes().get("AWS.SNS.SMS.SenderID").getStringValue()).isEqualTo("Ray");
        assertThat(request.getValue().getMessageAttributes().get("AWS.SNS.SMS.SMSType").getStringValue()).isEqualTo("Transactional");
    }

    @Test
    public void sendWithNullSender() throws Exception {
        PublishResult expected = mock(PublishResult.class);
        when(expected.getMessageId()).thenReturn("this-is-message-id");
        when (sns.publish(any())).thenReturn(expected);

        final SendSmsRequest request = new SendSmsRequest();
        request.setSenderId(null);
        request.setPhone("(514)544-11 11");
        request.setMessage("Hello world");

        // When
        final SendSmsResponse result = gateway.send(request);

        // Then
        assertThat(result.getMessageId()).isEqualTo("this-is-message-id");

        ArgumentCaptor<PublishRequest> req = ArgumentCaptor.forClass(PublishRequest.class);
        verify(sns).publish(req.capture());
        assertThat(req.getValue().getMessage()).isEqualTo("Hello world");
        assertThat(req.getValue().getPhoneNumber()).isEqualTo("+15145441111");
        assertThat(req.getValue().getMessageAttributes().containsKey("AWS.SNS.SMS.SenderID")).isFalse();
        assertThat(req.getValue().getMessageAttributes().get("AWS.SNS.SMS.SMSType").getStringValue()).isEqualTo("Transactional");
    }

    @Test
    public void sendWithNullMessage() throws Exception {
        // Given
        final SendSmsRequest request = new SendSmsRequest();
        request.setSenderId("foo");
        request.setPhone("(514)544-11 11");
        request.setMessage(null);

        try {
            // When
            gateway.send(request);
            fail();
        } catch (SmsException e){
            verify(sns, never()).publish(any());
        }
    }

    @Test
    public void sendWithEmptyMessage() throws Exception {
        // Given
        final SendSmsRequest request = new SendSmsRequest();
        request.setSenderId("foo");
        request.setPhone("(514)544-11 11");
        request.setMessage("");


        try {
            // When
            gateway.send(request);
            fail();
        } catch (SmsException e){
            verify(sns, never()).publish(any());
        }
    }


    @Test(expected = SmsException.class)
    public void sendAWSException() throws Exception {
        // Given
        final SendSmsRequest request = new SendSmsRequest();
        request.setSenderId("foo");
        request.setPhone("(514)544-11 11");
        request.setMessage("flkfdl");

        AmazonClientException e = new AmazonClientException("failed");
        when(sns.publish(any())).thenThrow(e);

        // When
        gateway.send(request);
    }
}
