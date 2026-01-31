package com.parkingmanage.service.well;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.parkingmanage.common.HttpClientUtil;
import com.parkingmanage.dto.well.WellUserInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 威尔用户信息查询服务
 */
@Slf4j
@Service
public class WellUserInfoService {

    @Value("${well.api.base-url}")
    private String wellApiBaseUrl;

    /**
     * 根据学工号查询用户信息
     * 
     * @param userNo 学工号
     * @return 用户信息，如果查询失败返回null
     */
    public WellUserInfoResponse getUserInfo(String userNo) {
        if (userNo == null || userNo.trim().isEmpty()) {
            log.warn("学工号为空，无法查询用户信息");
            return null;
        }

        try {
            String url = wellApiBaseUrl + "/api-general/api-general/open-user/getUserInfo?userNo=" + userNo;
            log.debug("查询威尔用户信息: userNo={}, url={}", userNo, url);

            String response = HttpClientUtil.doGet(url);
            if (response == null || response.trim().isEmpty()) {
                log.warn("威尔用户信息查询响应为空: userNo={}", userNo);
                return null;
            }

            JSONObject jsonResponse = JSON.parseObject(response);
            Integer code = jsonResponse.getInteger("code");
            
            if (code == null || code != 0) {
                log.warn("威尔用户信息查询失败: userNo={}, code={}, msg={}", 
                        userNo, code, jsonResponse.getString("msg"));
                return null;
            }

            JSONObject data = jsonResponse.getJSONObject("data");
            if (data == null) {
                log.warn("威尔用户信息查询无数据: userNo={}", userNo);
                return null;
            }

            WellUserInfoResponse userInfo = new WellUserInfoResponse();
            userInfo.setUserNo(data.getString("userNo"));
            userInfo.setUserName(data.getString("userName"));
            userInfo.setUserIdentity(data.getString("userIdentity"));
            userInfo.setDeptName(data.getString("deptName"));
            userInfo.setCardNo(data.getString("cardNo"));
            userInfo.setSex(data.getString("sex"));
            userInfo.setPhone(data.getString("phone"));

            log.debug("威尔用户信息查询成功: userNo={}, userName={}, userIdentity={}", 
                    userNo, userInfo.getUserName(), userInfo.getUserIdentity());
            
            return userInfo;

        } catch (Exception e) {
            log.error("威尔用户信息查询异常: userNo={}, error={}", userNo, e.getMessage(), e);
            return null;
        }
    }
}
