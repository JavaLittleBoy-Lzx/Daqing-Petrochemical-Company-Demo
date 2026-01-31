package com.parkingmanage.query;

import lombok.Data;

@Data
public class CommunityVisitorQuery {
    private String province;
    private String city;
    private String district;
    private String community;
    private String building;
    private Integer units;
    private Integer floor;
    private Integer roomnumber;
}