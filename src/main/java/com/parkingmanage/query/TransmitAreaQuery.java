package com.parkingmanage.query;

import lombok.Data;

@Data
public class TransmitAreaQuery {
    private String province;
    private String city;
    private String district;
    private String community;
    private String building;
    private String openid;
    private Integer units;
    private Integer floor;
}
