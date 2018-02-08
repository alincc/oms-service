package io.tchepannou.enigma.oms.backend.profile;

import io.tchepannou.core.logger.Loggable;
import io.tchepannou.core.rest.RestClient;
import io.tchepannou.core.rest.exception.HttpException;
import io.tchepannou.enigma.oms.client.OMSErrorCode;
import io.tchepannou.enigma.oms.exception.NotFoundException;
import io.tchepannou.enigma.profile.client.dto.MerchantDto;
import io.tchepannou.enigma.profile.client.rr.SearchMerchantRequest;
import io.tchepannou.enigma.profile.client.rr.SearchMerchantResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Component
@ConfigurationProperties("enigma.service.profile.merchant")
public class MerchantBackend {
    @Autowired
    private RestClient rest;

    private String url;


    @Loggable("Profile.Merchant.findById")
    public MerchantDto findById(Integer id){
        return findById(id, rest);
    }

    public MerchantDto findById(Integer id, final RestClient rest){
        final List<MerchantDto> merchants = search(Collections.singletonList(id), rest);
        if (merchants.isEmpty()){
            throw new NotFoundException(OMSErrorCode.MERCHANT_NOT_FOUND);
        }
        return merchants.stream().filter(m -> id.equals(m.getId())).findFirst().get();
    }

    @Loggable("Profile.Merchant.search")
    public List<MerchantDto> search(final Collection<Integer> merchantIds){
        return search(merchantIds, rest);
    }
    public List<MerchantDto> search(final Collection<Integer> merchantIds, final RestClient rest){
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
