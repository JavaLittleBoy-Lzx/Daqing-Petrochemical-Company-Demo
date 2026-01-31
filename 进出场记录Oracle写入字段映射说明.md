# 进出场记录Oracle写入字段映射说明

## 问题描述

车辆出场记录写入Oracle数据库时报错：
```
ORA-01400: 无法将 NULL 插入 ("AENTRANCEGUARD"."AUTOINOUTAKEINFO"."KLX")
```

## 原因分析

Oracle数据库表 `AENTRANCEGUARD.AUTOINOUTAKEINFO` 的 `KLX`（卡类型）字段设置为NOT NULL约束，但在写入车辆进出场记录时没有提供该字段的值。

## 字段映射对比

### 人员表 PERSONINOUTAKEINFO（正常）
- 包含 `KLX` 字段，默认值为 "D"（临时卡）
- 包含 `RYLX` 字段，默认值为 "5"（外来人员）

### 车辆表 AUTOINOUTAKEINFO（缺失）
- ❌ 缺少 `KLX` 字段
- ❌ 缺少 `RYLX` 字段（如果需要）

## 修复方案

### 1. 添加KLX字段（卡类型）

车辆记录应该包含卡类型字段：

```java
// 卡类型：默认设置为长期卡A
String klx = "A";
```

### 2. 修改SQL语句

**修改前：**
```sql
INSERT INTO AENTRANCEGUARD.AUTOINOUTAKEINFO 
(RECORDNO, CPHM, CQ, JCCDM, JCCSJ, JCCSBCPHM, FXMWXM, JCCBZ, FXLB, HPYS, ZPURL) 
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
```

**修改后：**
```sql
INSERT INTO AENTRANCEGUARD.AUTOINOUTAKEINFO 
(RECORDNO, KLX, CPHM, CQ, JCCDM, JCCSJ, JCCSBCPHM, FXMWXM, JCCBZ, FXLB, HPYS, ZPURL) 
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
```

### 3. 字段说明

| 字段名 | 说明 | 默认值 | 备注 |
|--------|------|--------|------|
| KLX | 卡类型 | A | A=长期卡 |
| RYLX | 人员类型 | - | 车辆表不需要此字段 |

## 影响范围

需要修改以下两个方法：
1. `writeVehicleInRecord()` - 车辆进场记录写入
2. `writeVehicleOutRecord()` - 车辆离场记录写入

## 修复后效果

- ✅ 车辆进出场记录可以正常写入Oracle数据库
- ✅ 不再出现 `ORA-01400` 错误
- ✅ 与人员记录处理方式保持一致

## 修复时间

2026-01-15

## 修复状态

✅ **已完成**

已成功修复车辆进出场记录写入Oracle时KLX字段为NULL的问题：

1. ✅ 在 `writeVehicleInRecord()` 方法中添加了KLX字段（默认值"D"）
2. ✅ 在 `writeVehicleOutRecord()` 方法中添加了KLX字段（默认值"D"）
3. ✅ 修改了SQL INSERT语句，包含KLX字段
4. ✅ 编译通过，无语法错误

### 修改内容

**文件：** `OracleRecordWriteService.java`

**进场记录（第88-89行）：**
```java
// 卡类型：默认设置为长期卡A
String klx = "A";
```

**离场记录（第168-169行）：**
```java
// 卡类型：默认设置为长期卡A
String klx = "A";
```

**SQL语句修改：**
- 字段列表增加：`KLX`
- 参数列表增加：`klx` (第2个参数位置)

### 测试建议

1. 重启应用服务
2. 触发车辆进出场记录推送
3. 检查Oracle数据库中的记录是否正常写入
4. 确认KLX字段值为"A"（长期卡）
