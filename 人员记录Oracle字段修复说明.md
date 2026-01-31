# 人员记录Oracle字段修复说明

## 修复时间
2026-01-15

## 问题描述
在写入威尔门禁人员记录到Oracle数据库时，出现以下错误：
```
ORA-00904: "ZJHM": invalid identifier
ORA-00904: "BXH": invalid identifier
```

原因：SQL语句中使用了Oracle数据库表 `PERSONINOUTAKEINFO` 中不存在的字段。

## Oracle数据库实际字段

### 人员表：PENTRANCEGUARD.PERSONINOUTAKEINFO

| 字段名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| RECORDNO | VARCHAR2(14) | 是 | 记录号 |
| KLX | VARCHAR2(10) | 是 | 卡类型 |
| XM | VARCHAR2(20) | 是 | 姓名 |
| YXM | VARCHAR2(10) | 否 | 音序码 |
| RYLX | VARCHAR2(20) | 是 | 人员类型 |
| RYID | VARCHAR2(20) | 是 | 人员ID |
| DWMC | VARCHAR2(200) | 否 | 单位名称 |
| JCCBZ | VARCHAR2(10) | 是 | 进出标志 |
| JCDM | VARCHAR2(50) | 是 | 大门编码 |
| JCSJ | VARCHAR2(20) | 是 | 进出时间 |
| KMFS | VARCHAR2(10) | 否 | 开门方式 |
| CQ | VARCHAR2(10) | 是 | 厂区 |
| XB | VARCHAR2(2) | 否 | 性别 |
| JCTD | VARCHAR2(20) | 否 | 进出通道 |
| CLBZ | VARCHAR2(2) | 是 | 处理标志 |
| ZPURL | VARCHAR2(500) | 否 | 照片URL |

**注意：表中没有 ZJHM（证件号码）和 BXH（保险号）字段**

## 修复内容

### 1. 移除不存在的字段
- 从SQL INSERT语句中移除了 `ZJHM`（证件号码）字段
- 从SQL INSERT语句中移除了 `BXH`（保险号）字段

### 2. 添加必需字段
- 添加了 `RYID`（人员ID）字段，使用威尔门禁的工号（userNo）作为人员ID
- 添加了 `DWMC`（单位名称）字段，暂时设置为null
- 添加了 `CLBZ`（处理标志）字段，默认设置为"0"（未处理）

### 3. 移除不必要的代码
- 移除了调用 `wellUserInfoService.getUserInfo()` 查询身份证号的代码
- 移除了使用卡号作为证件号码的备用逻辑
- 修复了日志中使用未定义变量 `userIdentity` 的错误

## 修改后的SQL语句

```sql
INSERT INTO PENTRANCEGUARD.PERSONINOUTAKEINFO 
(RECORDNO, KLX, XM, YXM, RYLX, RYID, DWMC, JCCBZ, JCDM, JCSJ, KMFS, CQ, XB, JCTD, CLBZ, ZPURL) 
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
```

## 字段映射关系

| Oracle字段 | 威尔门禁字段 | 默认值 | 说明 |
|-----------|-------------|--------|------|
| RECORDNO | - | 自动生成 | 14位时间戳 |
| KLX | - | "D" | 卡类型（临时卡） |
| XM | userName | - | 姓名 |
| YXM | - | null | 音序码 |
| RYLX | - | "5" | 人员类型（外来人员） |
| RYID | userNo | - | 人员ID（使用工号） |
| DWMC | - | null | 单位名称 |
| JCCBZ | recDic | - | 进出标志（0→1进，1→2出） |
| JCDM | doorName | - | 大门编码（通过映射获取） |
| JCSJ | recTime | - | 进出时间 |
| KMFS | authMode | - | 开门方式（101→199人脸） |
| CQ | doorName | - | 厂区编码（通过映射获取） |
| XB | - | null | 性别 |
| JCTD | - | null | 进出通道 |
| CLBZ | - | "0" | 处理标志（未处理） |
| ZPURL | recPhoto | - | 照片URL（添加前缀） |

## 测试建议

1. 重启应用后，观察日志确认不再出现 `ORA-00904` 错误
2. 检查人员进出记录是否能正常写入Oracle数据库
3. 验证写入的数据字段是否完整且正确
4. 确认照片URL前缀是否正确添加

## 相关文件
- `src/main/java/com/parkingmanage/service/oracle/OracleRecordWriteService.java`
