package io.tchepannou.enigma.oms.service.mq.notification;

import io.tchepannou.enigma.oms.service.mq.NotificationConsumerTestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@Sql({"classpath:/sql/clean.sql", "classpath:/sql/MerchantConsumer.sql"})
@ActiveProfiles(profiles = {"stub"})
public class MerchantConsumerIT extends NotificationConsumerTestBase {
    @Autowired
    private MerchantConsumer consumer;

    @Test
    public void shouldSendEmailToMerchants() throws Exception {
        // Given
        consumer.consume(100);

        // When
        mail.waitForIncomingEmail(5000, 1);
        final MimeMessage[] msgs = mail.getReceivedMessages();

        // Then
        assertThat(msgs).hasSize(2);
        assertThat(Arrays.asList(msgs[0].getRecipients(Message.RecipientType.TO))).containsExactly(new InternetAddress("merchant1000@gmail.com"));
        assertThat(Arrays.asList(msgs[0].getSubject())).contains("[Enigma-Voyages] Travel Confirmation - Order #100");

        assertThat(Arrays.asList(msgs[1].getRecipients(Message.RecipientType.TO))).containsExactly(new InternetAddress("merchant1001@gmail.com"));
        assertThat(Arrays.asList(msgs[1].getSubject())).contains("[Enigma-Voyages] Travel Confirmation - Order #100");
    }

    @Test
    public void shouldNotSendEmailToMerchantForNewOrder() throws Exception {
        // Given
        consumer.consume(101);

        // When
        mail.waitForIncomingEmail(5000, 1);
        final MimeMessage[] msgs = mail.getReceivedMessages();

        // Then
        assertThat(msgs).hasSize(0);
    }

    @Test
    public void shouldNotSendEmailToMerchantForPendingOrder() throws Exception {
        // Given
        consumer.consume(102);

        // When
        mail.waitForIncomingEmail(5000, 1);
        final MimeMessage[] msgs = mail.getReceivedMessages();

        // Then
        assertThat(msgs).hasSize(0);
    }

    @Test
    public void shouldNotSendEmailToMerchantForCancelledOrder() throws Exception {
        // Given
        consumer.consume(103);

        // When
        mail.waitForIncomingEmail(5000, 1);
        final MimeMessage[] msgs = mail.getReceivedMessages();

        // Then
        assertThat(msgs).hasSize(0);
    }
}
