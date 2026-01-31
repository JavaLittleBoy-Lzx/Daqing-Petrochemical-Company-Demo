# 车辆照片URL前缀添加说明

## 问题描述

车辆进出场记录写入Oracle数据库时，照片URL（ZPURL字段）缺少前缀，导致照片路径不完整。

**问题示例：**
```
ZPURL: /vems/picpath/2026/01/15/0000000000/2/car/mhTBaLGlGKIV2XCp_2_87_20260115094632_E723BP.jpg
```

**期望结果：**
```
ZPURL: http://11.114.34.28:8092/vems/picpath/2026/01/15/0000000000/2/car/mhTBaLGlGKIV2XCp_2_87_20260115094632_E723BP.jpg
```

## 修复方案

### 1. 添加车辆照片URL前缀常量

在 `OracleRecordWriteService.java` 中添加常量：

```java
// 车辆照片URL前缀
private static final String VEHICLE_PHOTO_PREFIX = "http://11.114.34.28:8092/vems";
```

### 2. 修改车辆进场记录写入逻辑

**修改前：**
```java
jdbcTemplate.update(sql,
    recordNo,
    klx,
    carLicenseNumber,
    gateCode.getAreaCode(),
    gateCode.getGateCode(),
    enterTime,
    carLicenseNumber,
    inOperatorName,
    "1",
    fxlb,
    hpys,
    enterCarFullPicture  // 直接使用原始路径
);
```

**修改后：**
```java
// 处理照片URL（添加前缀）
String photoUrl = null;
if (enterCarFullPicture != null && !enterCarFullPicture.trim().isEmpty()) {
    photoUrl = VEHICLE_PHOTO_PREFIX + enterCarFullPicture;
}

jdbcTemplate.update(sql,
    recordNo,
    klx,
    carLicenseNumber,
    gateCode.getAreaCode(),
    gateCode.getGateCode(),
    enterTime,
    carLicenseNumber,
    inOperatorName,
    "1",
    fxlb,
    hpys,
    photoUrl  // 使用带前缀的完整URL
);
```

### 3. 修改车辆离场记录写入逻辑

同样的处理方式应用于离场记录：

```java
// 处理照片URL（添加前缀）
String photoUrl = null;
if (leaveCarFullPicture != null && !leaveCarFullPicture.trim().isEmpty()) {
    photoUrl = VEHICLE_PHOTO_PREFIX + leaveCarFullPicture;
}

jdbcTemplate.update(sql,
    recordNo,
    klx,
    carLicenseNumber,
    gateCode.getAreaCode(),
    gateCode.getGateCode(),
    leaveTime,
    carLicenseNumber,
    outOperatorName,
    "2",
    fxlb,
    hpys,
    photoUrl  // 使用带前缀的完整URL
);
```

## 对比说明

### 人员照片URL处理（已有）
```java
private static final String PERSON_PHOTO_PREFIX = "http://11.114.34.25:8888";

// 处理照片URL（添加前缀）
String photoUrl = null;
if (recPhoto != null && !recPhoto.trim().isEmpty()) {
    photoUrl = PERSON_PHOTO_PREFIX + recPhoto;
}
```

### 车辆照片URL处理（新增）
```java
private static final String VEHICLE_PHOTO_PREFIX = "http://11.114.34.28:8092/vems";

// 处理照片URL（添加前缀）
String photoUrl = null;
if (enterCarFullPicture != null && !enterCarFullPicture.trim().isEmpty()) {
    photoUrl = VEHICLE_PHOTO_PREFIX + enterCarFullPicture;
}
```

## 影响范围

**修改文件：**
- `OracleRecordWriteService.java`

**修改方法：**
- `writeVehicleInRecord()` - 车辆进场记录写入
- `writeVehicleOutRecord()` - 车辆离场记录写入

**影响功能：**
- 所有车辆进出场记录的照片URL
- Oracle数据库中AUTOINOUTAKEINFO表的ZPURL字段

## 修复后效果

✅ 车辆进场记录照片URL完整：`http://11.114.34.28:8092/vems/picpath/...`  
✅ 车辆离场记录照片URL完整：`http://11.114.34.28:8092/vems/picpath/...`  
✅ 与人员记录照片URL处理方式保持一致  
✅ 照片可以通过完整URL直接访问

## 测试建议

1. 重启应用服务
2. 触发车辆进出场记录推送
3. 查询Oracle数据库中的ZPURL字段
4. 确认照片URL包含完整前缀
5. 测试照片URL是否可以正常访问

## 修改时间

2026-01-15
