package io.tchepannou.enigma.oms.backend.refdata;

import io.tchepannou.core.logger.Loggable;
import io.tchepannou.core.rest.RestClient;
import io.tchepannou.core.rest.exception.HttpException;
import io.tchepannou.enigma.refdata.client.dto.SiteDto;
import io.tchepannou.enigma.refdata.client.rr.SiteResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("enigma.service.refdata.site")
public class SiteBackend {
    @Autowired
    private RestClient rest;

    private String url;

    @Loggable("RefData.Site.findById")
    public SiteDto findById(Integer id){
        return findById(id, rest);
    }

    public SiteDto findById(Integer id, RestClient rest){
        try {
            return rest.get(url + "/" + id, SiteResponse.class).getBody().getSite();
        } catch (HttpException e){
            throw new RefDataException(e);
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
