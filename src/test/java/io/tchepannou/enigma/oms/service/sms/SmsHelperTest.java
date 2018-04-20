package io.tchepannou.enigma.oms.service.sms;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SmsHelperTest {
    @Test
    public void formatPhone() throws Exception {
        assertThat(SmsHelper.formatPhone("(514) 758-01-00")).isEqualTo("+15147580100");
    }

    @Test
    public void formatPhoneNull() throws Exception {
        assertThat(SmsHelper.formatPhone(null)).isNull();
    }

    @Test
    public void formatMessage() throws Exception {
        assertThat(SmsHelper.formatMessage("Hello world")).isEqualTo("Hello world");
    }

    @Test
    public void formatMessageNull() throws Exception {
        assertThat(SmsHelper.formatMessage(null)).isNull();
    }

    @Test
    public void formatMessageLong() throws Exception {
        assertThat(SmsHelper.formatMessage("12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"))
                .isEqualTo("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
    }
}
