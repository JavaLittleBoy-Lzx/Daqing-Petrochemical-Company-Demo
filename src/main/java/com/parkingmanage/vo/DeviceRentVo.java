package com.parkingmanage.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 @PROJECT_NAME: parkingmanage
 @PACKAGE_NAME: com.parkingmanage.vo
 @NAME: DeviceRentVo
 @author:yuli
 @DATE: 2022/3/26 19:16
 @description: 2.租赁  租赁了多少台，多少租金
*/
@Data
public class DeviceRentVo {
    List<String> typeName;
    List<Integer> rentalNum;
    List<BigDecimal> allAccentRent;
    List<Integer> maintainNum;

}
