package com.parkingmanage.vo;

import lombok.Data;

/**
 * @author MLH
 * @since 2022-09-08
 */
@Data
public class AreaResult {
    private String province;
    private String city;
    private String district;
    private String community;
    private String building;
    private String units;
    private String floor;
    private String roomnumber;
}
