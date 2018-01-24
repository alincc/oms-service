package io.tchepannou.enigma.oms.backend.profile;

import io.tchepannou.core.logger.Loggable;
import io.tchepannou.core.rest.RestClient;
import io.tchepannou.core.rest.exception.HttpException;
import io.tchepannou.enigma.profile.client.dto.MerchantDto;
import io.tchepannou.enigma.profile.client.rr.SearchMerchantRequest;
import io.tchepannou.enigma.profile.client.rr.SearchMerchantResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
@ConfigurationProperties("enigma.service.profile.merchant")
public class MerchantBackend {
    @Autowired
    private RestClient rest;

    private String url;


    @Loggable("Profile.Merchant.search")
    public List<MerchantDto> search(final Collection<Integer> merchantIds){
        final SearchMerchantRequest request = new SearchMerchantRequest();
        request.setIds(new ArrayList<>(merchantIds));
        try {

            return rest.post(url + "/search", request, SearchMerchantResponse.class).getBody().getMerchants();

        } catch (HttpException e){
            throw new ProfileException(e);
        }
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
