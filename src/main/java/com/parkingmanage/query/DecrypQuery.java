package com.parkingmanage.query;

import lombok.Data;

@Data
public class DecrypQuery {
    private String sessionID;
    private String encryptedData;
    private String iv;
}