package org.example;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IpDetails {
    private String ip;
    private String continent;
    private String country;
    private String region;
    private String city;
    private String latitude;
    private String longitude;
    private boolean success;
}
