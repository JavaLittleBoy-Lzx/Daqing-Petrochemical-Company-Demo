package com.parkingmanage.service.oracle;

import com.parkingmanage.dto.oracle.VehicleValidInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

/**
 * Oracle查询服务
 * 提供Oracle数据库的查询功能
 */
@Slf4j
@Service
public class OracleQueryService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 常见的日期时间格式
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    /**
     * 安全地从ResultSet中获取LocalDateTime
     * 先尝试作为Timestamp获取，失败则作为String解析
     * 
     * @param rs ResultSet
     * @param columnName 列名
     * @return LocalDateTime或null
     */
    private LocalDateTime safeGetDateTime(java.sql.ResultSet rs, String columnName) {
        try {
            // 首先尝试作为Timestamp获取
            Timestamp timestamp = rs.getTimestamp(columnName);
            if (timestamp != null) {
                log.debug("字段[{}]作为Timestamp获取成功: {}", columnName, timestamp);
                return timestamp.toLocalDateTime();
            } else {
                log.debug("字段[{}]的Timestamp值为null", columnName);
            }
        } catch (Exception e) {
            log.debug("字段[{}]无法作为Timestamp获取: {}", columnName, e.getMessage());
            // Timestamp转换失败，尝试作为字符串解析
            try {
                String dateStr = rs.getString(columnName);
                log.debug("字段[{}]作为String获取: [{}]", columnName, dateStr);
                
                if (dateStr != null && !dateStr.trim().isEmpty()) {
                    dateStr = dateStr.trim();
                    
                    // 尝试多种日期格式
                    for (DateTimeFormatter formatter : DATE_FORMATTERS) {
                        try {
                            LocalDateTime result = LocalDateTime.parse(dateStr, formatter);
                            log.debug("字段[{}]使用格式[{}]解析成功: {}", columnName, formatter, result);
                            return result;
                        } catch (DateTimeParseException ignored) {
                            // 继续尝试下一个格式
                        }
                    }
                    
                    log.warn("无法解析日期时间字段 {}: {}", columnName, dateStr);
                }
            } catch (Exception ex) {
                log.warn("获取日期时间字段 {} 失败: {}", columnName, ex.getMessage());
            }
        }
        return null;
    }

    /**
     * 根据车牌号码查询车辆有效信息
     * 从视图 aentranceguard.view_autovalidinfo 查询
     * 
     * @param plateNumber 车牌号码
     * @return 车辆有效信息列表
     */
    public List<VehicleValidInfoDTO> queryVehicleByPlateNumber(String plateNumber) {
        log.info("========== 开始查询车辆有效信息 ==========");
        log.info("车牌号码: {}", plateNumber);
        
        if (plateNumber == null || plateNumber.trim().isEmpty()) {
            log.warn("车牌号码为空");
            return Collections.emptyList();
        }
        
        try {
            // 先测试视图是否可访问，查询总记录数
            try {
                Long totalCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM aentranceguard.view_autovalidinfo", Long.class);
                log.info("视图总记录数: {}", totalCount);
                
                // 查询前10条记录的车牌号，用于调试
                List<String> samplePlates = jdbcTemplate.queryForList(
                    "SELECT CPHM FROM aentranceguard.view_autovalidinfo WHERE ROWNUM <= 10", String.class);
                log.info("视图中的示例车牌号: {}", samplePlates);
                
                // 详细输出第一个车牌号的信息（用于调试）
                if (!samplePlates.isEmpty()) {
                    String firstPlate = samplePlates.get(0);
                    log.info("第一个车牌号详细信息: 内容=[{}], 长度={}, 去空格后=[{}], 去空格后长度={}", 
                            firstPlate, 
                            firstPlate != null ? firstPlate.length() : 0,
                            firstPlate != null ? firstPlate.trim() : "null",
                            firstPlate != null ? firstPlate.trim().length() : 0);
                }
            } catch (Exception e) {
                log.error("查询视图总记录数失败: {}", e.getMessage());
            }
            
            // 从视图 aentranceguard.view_autovalidinfo 查询车辆数据
            // 使用LIKE模糊查询，将汉字替换为%通配符
            String sql = "SELECT RECORDNOL, RECORDNO, KH, CPHM, HPYS, HPYSNAME, CLZL, CLZLNAME, " +
                        "CLLX, CLLXNAME, PPXH, DWMCNAME, JSYXM, CODE, CQDM, CQDMNAME, " +
                        "KYXQKSSJ, KYXQJSSJ, DQZT, DQZTNAME, ISCHECK, ISCHECKNAME, CZSJ " +
                        "FROM aentranceguard.view_autovalidinfo " +
                        "WHERE CPHM LIKE ? " +  // 使用LIKE模糊查询
                        "ORDER BY CZSJ DESC";
            
            // 将车牌号中的汉字替换为%，只保留数字和字母
            // 例如：黑E06568 -> %E06568
            String likePattern = "%" + plateNumber.trim().replaceAll("[\\u4e00-\\u9fa5]", "");
            
            log.info("执行SQL查询: 车牌号={}", plateNumber);
            log.info("SQL语句: {}", sql);
            log.info("LIKE查询参数（去除汉字）: [{}]", likePattern);
            
            List<VehicleValidInfoDTO> vehicles = jdbcTemplate.query(sql, ps -> {
                ps.setString(1, likePattern);  // 使用LIKE模糊查询参数
            }, (rs, rowNum) -> {
                VehicleValidInfoDTO vehicle = new VehicleValidInfoDTO();
                
                // 记录号
                vehicle.setRecordnoL(rs.getString("RECORDNOL"));
                vehicle.setRecordno(rs.getString("RECORDNO"));
                
                // 基本信息
                vehicle.setCardNo(rs.getString("KH"));
                vehicle.setPlateNumber(rs.getString("CPHM"));
                vehicle.setPlateColorCode(rs.getString("HPYS"));
                vehicle.setPlateColorName(rs.getString("HPYSNAME"));
                
                // 车辆类型信息
                vehicle.setVehicleCategoryCode(rs.getString("CLZL"));
                vehicle.setVehicleCategoryName(rs.getString("CLZLNAME"));
                vehicle.setVehicleTypeCode(rs.getString("CLLX"));
                vehicle.setVehicleTypeName(rs.getString("CLLXNAME"));
                vehicle.setBrandModel(rs.getString("PPXH"));
                
                // 单位和驾驶员信息
                vehicle.setCompanyName(rs.getString("DWMCNAME"));
                vehicle.setDriverName(rs.getString("JSYXM"));
                vehicle.setCode(rs.getString("CODE"));
                
                // 厂区信息
                vehicle.setAreaCode(rs.getString("CQDM"));
                vehicle.setAreaName(rs.getString("CQDMNAME"));
                
                // 有效期 - 使用安全的日期时间获取方法
                LocalDateTime validStartTime = safeGetDateTime(rs, "KYXQKSSJ");
                if (validStartTime != null) {
                    vehicle.setValidStartTime(validStartTime);
                }
                LocalDateTime validEndTime = safeGetDateTime(rs, "KYXQJSSJ");
                if (validEndTime != null) {
                    vehicle.setValidEndTime(validEndTime);
                }
                
                // 状态信息
                vehicle.setStatusCode(rs.getString("DQZT"));
                vehicle.setStatusName(rs.getString("DQZTNAME"));
                vehicle.setIsCheck(rs.getString("ISCHECK"));
                vehicle.setCheckName(rs.getString("ISCHECKNAME"));
                
                // 操作时间
                vehicle.setOperateTime(rs.getString("CZSJ"));
                
                log.debug("查询到车辆: 车牌={}, 驾驶员={}, 单位={}, 厂区={}, 状态={}", 
                        vehicle.getPlateNumber(), vehicle.getDriverName(), 
                        vehicle.getCompanyName(), vehicle.getAreaName(), 
                        vehicle.getStatusName());
                
                return vehicle;
            });
            
            log.info("查询到 {} 条车辆记录", vehicles.size());
            log.info("========== 车辆有效信息查询完成 ==========");
            
            return vehicles;
            
        } catch (DataAccessException e) {
            log.error("查询车辆有效信息失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 查询所有车辆有效信息（用于测试，限制返回前N条）
     * 
     * @param limit 返回记录数限制
     * @return 车辆有效信息列表
     */
    public List<VehicleValidInfoDTO> queryAllVehicles(int limit) {
        log.info("========== 开始查询所有车辆有效信息（限制{}条）==========", limit);
        
        try {
            // 查询前N条记录（注意：Oracle中ROWNUM要在ORDER BY之前）
            String sql = "SELECT * FROM (" +
                        "SELECT RECORDNOL, RECORDNO, KH, CPHM, HPYS, HPYSNAME, CLZL, CLZLNAME, " +
                        "CLLX, CLLXNAME, PPXH, DWMCNAME, JSYXM, CODE, CQDM, CQDMNAME, " +
                        "KYXQKSSJ, KYXQJSSJ, DQZT, DQZTNAME, ISCHECK, ISCHECKNAME, CZSJ " +
                        "FROM aentranceguard.view_autovalidinfo " +
                        "ORDER BY CZSJ DESC" +
                        ") WHERE ROWNUM <= ?";
            
            log.info("执行SQL查询: 限制{}条记录", limit);
            
            List<VehicleValidInfoDTO> vehicles = jdbcTemplate.query(sql, ps -> {
                ps.setInt(1, limit);
            }, (rs, rowNum) -> {
                VehicleValidInfoDTO vehicle = new VehicleValidInfoDTO();
                
                // 记录号
                vehicle.setRecordnoL(rs.getString("RECORDNOL"));
                vehicle.setRecordno(rs.getString("RECORDNO"));
                
                // 基本信息
                vehicle.setCardNo(rs.getString("KH"));
                vehicle.setPlateNumber(rs.getString("CPHM"));
                vehicle.setPlateColorCode(rs.getString("HPYS"));
                vehicle.setPlateColorName(rs.getString("HPYSNAME"));
                
                // 车辆类型信息
                vehicle.setVehicleCategoryCode(rs.getString("CLZL"));
                vehicle.setVehicleCategoryName(rs.getString("CLZLNAME"));
                vehicle.setVehicleTypeCode(rs.getString("CLLX"));
                vehicle.setVehicleTypeName(rs.getString("CLLXNAME"));
                vehicle.setBrandModel(rs.getString("PPXH"));
                
                // 单位和驾驶员信息
                vehicle.setCompanyName(rs.getString("DWMCNAME"));
                vehicle.setDriverName(rs.getString("JSYXM"));
                vehicle.setCode(rs.getString("CODE"));
                
                // 厂区信息
                vehicle.setAreaCode(rs.getString("CQDM"));
                vehicle.setAreaName(rs.getString("CQDMNAME"));
                
                // 有效期 - 使用安全的日期时间获取方法
                LocalDateTime validStartTime = safeGetDateTime(rs, "KYXQKSSJ");
                if (validStartTime != null) {
                    vehicle.setValidStartTime(validStartTime);
                }
                LocalDateTime validEndTime = safeGetDateTime(rs, "KYXQJSSJ");
                if (validEndTime != null) {
                    vehicle.setValidEndTime(validEndTime);
                }
                
                // 状态信息
                vehicle.setStatusCode(rs.getString("DQZT"));
                vehicle.setStatusName(rs.getString("DQZTNAME"));
                vehicle.setIsCheck(rs.getString("ISCHECK"));
                vehicle.setCheckName(rs.getString("ISCHECKNAME"));
                
                // 操作时间
                vehicle.setOperateTime(rs.getString("CZSJ"));
                
                log.debug("查询到车辆: 车牌={}, 驾驶员={}, 单位={}, 厂区={}, 状态={}", 
                        vehicle.getPlateNumber(), vehicle.getDriverName(), 
                        vehicle.getCompanyName(), vehicle.getAreaName(), 
                        vehicle.getStatusName());
                
                return vehicle;
            });
            
            log.info("查询到 {} 条车辆记录", vehicles.size());
            log.info("========== 所有车辆有效信息查询完成 ==========");
            
            return vehicles;
            
        } catch (DataAccessException e) {
            log.error("查询所有车辆有效信息失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
