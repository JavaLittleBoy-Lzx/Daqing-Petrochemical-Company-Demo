package com.parkingmanage.dto.well;

import lombok.Data;

/**
 * 威尔人脸请求DTO
 * 对应接口：/api-face/api-face/open-face/batch/insert
 */
@Data
public class WellFaceRequest {
    
    /** 人员工号 */
    private String userNo;
    
    /** 人脸照片url（与base64二选一） */
    private String photoUrl;
    
    /** 人脸照片Base64码（与url二选一） */
    private String photoCodeStr;
}