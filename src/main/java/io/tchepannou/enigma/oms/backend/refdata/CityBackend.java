package io.tchepannou.enigma.oms.backend.refdata;

import com.google.common.base.Joiner;
import io.tchepannou.core.logger.Loggable;
import io.tchepannou.core.rest.RestClient;
import io.tchepannou.enigma.refdata.client.dto.CityDto;
import io.tchepannou.enigma.refdata.client.rr.CityListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@ConfigurationProperties("enigma.service.refdata.city")
public class CityBackend {
    @Autowired
    private RestClient rest;

    private String url;

    @Loggable("RefData.City.search")
    public List<CityDto> search(Collection<Integer> ids){
        return search(ids, rest);
    }

    public List<CityDto> search(Collection<Integer> ids, RestClient rest){
        final String uri = url + "?ids=" + Joiner.on(",").join(ids) + "&limit=" + Integer.MAX_VALUE;
        return rest.get(uri, CityListResponse.class).getBody().getCities();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
