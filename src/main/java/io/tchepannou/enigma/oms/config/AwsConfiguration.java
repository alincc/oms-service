package io.tchepannou.enigma.oms.config;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("int")
public class AwsConfiguration {
    @Bean
    public AmazonSNS snsClient (){
        return AmazonSNSClient.builder()
                .withCredentials(awsCredentialsProvider())
                .withRegion(Regions.US_EAST_1)
                .build();
    }

    @Bean
    AWSCredentialsProvider awsCredentialsProvider(){
        return new SystemPropertiesCredentialsProvider();
    }
}
