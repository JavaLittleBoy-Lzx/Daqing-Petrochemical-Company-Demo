package com.parkingmanage.query;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 @ClassName YardInfoQuery
 @Description TODO
 @Author 李子雄
 @Date 2023/7/11 8:49:29
 @Version 1.0
*/
@Data
public class YardInfoQuery {
    private String yardCode;
    private String yardName;
//    private String entrancePassage;
}