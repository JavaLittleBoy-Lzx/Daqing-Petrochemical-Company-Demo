package com.parkingmanage.vo;

import lombok.Data;

import java.util.List;

//权限配置

/**
 * @PROJECT_NAME: parkingmanage
 * @PACKAGE_NAME: com.parkingmanage.vo
 * @NAME: RolePermVo
 * @author:yuli
 * @DATE: 2022/2/25 0:08
 * @description: asd
 */
@Data
public class RolePermVo {
    private Boolean checkAll;
    private Boolean isIndeterminate;
    private Integer id;
    private String title;
    private List<String> checkedList;
    private List<Sub> subs;
    @Data
    public static class Sub {
        private Integer id;
        private String title;
        private String index;
    }
}
