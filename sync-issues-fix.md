# 数据同步问题修复说明

## 问题1：威尔人员同步 remark 字段显示"注销"

### 问题描述
在调用威尔批量新增人员信息时，`remark` 字段中标注的是"注销"。

### 原因分析
`remark` 字段直接来源于 Oracle 数据库中的 **DQZTNAME（当前状态名称）** 字段。

**数据流程：**
1. **Oracle 数据源**：从 `pentranceguard.view_facedowninfo` 视图查询人员数据时，读取 `DQZTNAME` 字段
2. **字段映射**：在 `OracleDataService.java` 中：
   ```java
   String dqztName = rs.getString("DQZTNAME");
   person.setRemark(dqztName);
   ```
3. **传递到威尔**：在 `DataSyncServiceImpl.java` 中：
   ```java
   request.setRemark(person.getRemark());
   ```

**DQZTNAME 的含义：**
- 这是 Oracle 系统中人员的"当前状态名称"
- 可能的值包括：正常、注销、停用等
- 当人员在 Oracle 系统中被标记为"注销"状态时，这个值就会是"注销"

### 解决方案
这是正常的业务逻辑，`remark` 字段用于记录人员在 Oracle 系统中的原始状态信息。如果需要修改：
- **方案1**：过滤掉注销状态的人员不同步到威尔
- **方案2**：将 remark 字段改为其他内容（如固定值"Oracle同步"）

---

## 问题2：车辆数据查询时间字段类型转换错误

### 问题描述
```
java.sql.SQLException: 请求的转换无效
at oracle.jdbc.driver.CharCommonAccessor.getTimestamp(CharCommonAccessor.java:447)
Caused by: java.lang.IllegalArgumentException: Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]
```

### 原因分析
Oracle 视图 `aentranceguard.view_autovalidinfo` 中的时间字段 `KYXQKSSJ`（有效期开始时间）和 `KYXQJSSJ`（有效期结束时间）是 **VARCHAR2 类型**，但代码使用 `rs.getTimestamp()` 尝试读取，导致类型转换失败。

### 修复内容

#### 1. 修改 `OracleDataService.java` - 车辆数据查询
**位置**：`getLatestVehicleData()` 方法

**修改前：**
```java
// 有效期（使用实际字段名：KYXQKSSJ, KYXQJSSJ）
Timestamp kyxqkssj = rs.getTimestamp("KYXQKSSJ");
if (kyxqkssj != null) {
    vehicle.setValidStartTime(kyxqkssj.toLocalDateTime());
}
Timestamp kyxqjssj = rs.getTimestamp("KYXQJSSJ");
if (kyxqjssj != null) {
    vehicle.setValidEndTime(kyxqjssj.toLocalDateTime());
}
```

**修改后：**
```java
// 有效期（KYXQKSSJ, KYXQJSSJ 是VARCHAR2类型，需要字符串解析）
String kyxqkssjStr = rs.getString("KYXQKSSJ");
if (kyxqkssjStr != null && !kyxqkssjStr.trim().isEmpty()) {
    vehicle.setValidStartTime(parseDateTime(kyxqkssjStr));
}
String kyxqjssjStr = rs.getString("KYXQJSSJ");
if (kyxqjssjStr != null && !kyxqjssjStr.trim().isEmpty()) {
    vehicle.setValidEndTime(parseDateTime(kyxqjssjStr));
}
```

#### 2. 修改 `OracleDataService.java` - 人员数据查询
**位置**：`getLatestPersonData()` 方法

**修改前：**
```java
// 有效期（使用实际字段名：KYXQKSSJ, KYXQJSSJ - 注意是K开头不是R开头）
Timestamp kyxqkssj = rs.getTimestamp("KYXQKSSJ");
if (kyxqkssj != null) {
    person.setValidStartTime(kyxqkssj.toLocalDateTime());
}
Timestamp kyxqjssj = rs.getTimestamp("KYXQJSSJ");
if (kyxqjssj != null) {
    person.setValidEndTime(kyxqjssj.toLocalDateTime());
}
```

**修改后：**
```java
// 有效期（KYXQKSSJ, KYXQJSSJ 是VARCHAR2类型，需要字符串解析）
String kyxqkssjStr = rs.getString("KYXQKSSJ");
if (kyxqkssjStr != null && !kyxqkssjStr.trim().isEmpty()) {
    person.setValidStartTime(parseDateTime(kyxqkssjStr));
}
String kyxqjssjStr = rs.getString("KYXQJSSJ");
if (kyxqjssjStr != null && !kyxqjssjStr.trim().isEmpty()) {
    person.setValidEndTime(parseDateTime(kyxqjssjStr));
}
```

#### 3. 新增 `parseDateTime()` 方法
**位置**：`OracleDataService.java` 类末尾

```java
/**
 * 解析VARCHAR2格式的日期时间字符串为LocalDateTime
 * 支持多种格式：
 *   - yyyy-MM-dd HH:mm:ss
 *   - yyyy/MM/dd HH:mm:ss
 *   - yyyyMMddHHmmss
 *   - yyyy-MM-dd
 * 
 * @param dateTimeStr 日期时间字符串
 * @return LocalDateTime对象，解析失败返回null
 */
private LocalDateTime parseDateTime(String dateTimeStr) {
    if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
        return null;
    }
    
    String str = dateTimeStr.trim();
    
    // 尝试多种格式解析
    java.time.format.DateTimeFormatter[] formatters = {
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
        java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
    };
    
    for (java.time.format.DateTimeFormatter formatter : formatters) {
        try {
            return LocalDateTime.parse(str, formatter);
        } catch (Exception ignored) {
            // 尝试下一个格式
        }
    }
    
    // 尝试只有日期的格式
    java.time.format.DateTimeFormatter[] dateOnlyFormatters = {
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
    };
    
    for (java.time.format.DateTimeFormatter formatter : dateOnlyFormatters) {
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(str, formatter);
            return date.atStartOfDay();
        } catch (Exception ignored) {
            // 尝试下一个格式
        }
    }
    
    log.warn("无法解析日期时间字符串: {}", dateTimeStr);
    return null;
}
```

### 修复效果
- 支持 VARCHAR2 类型的时间字段解析
- 兼容多种日期时间格式
- 解析失败时返回 null 并记录警告日志，不会中断同步流程

---

## 编译验证
```bash
mvn clean compile -DskipTests
```
编译成功，无错误。

---

## 修复时间
2026-01-16 08:45
