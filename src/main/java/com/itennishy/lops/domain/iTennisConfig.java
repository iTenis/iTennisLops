package com.itennishy.lops.domain;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@RefreshScope
@Data
@Component
@ConfigurationProperties(prefix = "itennis")
public class iTennisConfig {

    private Map<String, String> pxeServer;

    private List<Map<String,String>> partitions;

    private Map<String, String> installOS;

    private Map<String, String> dhcpServer;

    private List<String> packages;

}
