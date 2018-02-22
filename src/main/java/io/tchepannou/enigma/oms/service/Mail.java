package io.tchepannou.enigma.oms.service;

import lombok.Getter;
import lombok.Setter;

import java.util.Locale;
import java.util.Map;

@Getter
@Setter
public class Mail {
    private String from;
    private String to;
    private String subject;
    private String content;
    private Map<String, Object> model;
    private String template;
    private Locale locale = Locale.getDefault();
}
