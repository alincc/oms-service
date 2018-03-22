package io.tchepannou.enigma.oms.backend.ferari;

import io.tchepannou.core.logger.Loggable;
import io.tchepannou.core.rest.RestClient;
import io.tchepannou.core.rest.exception.HttpStatusException;
import io.tchepannou.enigma.ferari.client.dto.ProductDto;
import io.tchepannou.enigma.ferari.client.rr.SearchProductRequest;
import io.tchepannou.enigma.ferari.client.rr.SearchProductResponse;
import io.tchepannou.enigma.oms.client.OMSErrorCode;
import io.tchepannou.enigma.oms.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Component
@ConfigurationProperties("enigma.service.ferari.product")
public class ProductBackend {
    @Autowired
    private RestClient rest;

    private String url;

    @Loggable("Ferari.Product.findById")
    public ProductDto findById(final Integer id){
        List<ProductDto> products = search(Arrays.asList(id));
        if (products.isEmpty()){
            throw new NotFoundException(OMSErrorCode.PRODUCT_NOT_FOUND);
        }
        return products.get(0);
    }

    @Loggable("Ferari.Product.search")
    public List<ProductDto> search(final Collection<Integer> ids){
        try {

            final SearchProductRequest request = new SearchProductRequest();
            request.setIds(ids);
            return rest.post(url + "/search", request, SearchProductResponse.class).getBody().getProducts();

        } catch (HttpStatusException e){
            throw new FerrariException("Unable to find product", e);
        }
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
