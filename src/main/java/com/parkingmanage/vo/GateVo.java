package com.parkingmanage.vo;

import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

/**
 * @program: ParkManage
 * @description: 开关闸接口中的接收参数
 * @author: lzx
 * @create: 2023-11-14 10:08
 **/
@Data
public class GateVo {
     private String parkCode;
     private String channelCode;
    private Integer opType;
    private String operator;
//    private Date operateTime;
}
