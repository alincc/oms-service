package io.tchepannou.enigma.oms.service;

import io.tchepannou.core.rest.CurrentRequestProvider;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

public class CurrentRequestProviderImpl implements CurrentRequestProvider {
    @Autowired
    private HttpServletRequest request;

    @Override
    public HttpServletRequest get() {
        return request;
    }
}
