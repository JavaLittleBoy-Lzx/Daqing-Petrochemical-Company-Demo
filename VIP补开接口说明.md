# VIP补开接口说明

## 功能概述

为指定车牌补开"请停车检查(化工西化肥西复合肥南)"VIP月票。

## 业务场景

针对以下8个车牌号码:
```
黑E627KW, 黑E7105L, 黑E8891S, 黑EF50377, 黑EFF1939, 黑EJ3363, 黑EP2999, 黑M2D002
```

这些车牌存在以下问题:
1. 有"停用tcjc"类型的VIP记录(已退款状态)
2. 缺少"请停车检查(化工西化肥西复合肥南)"类型的VIP记录
3. 退款记录在有效期内但没有生效中的记录

## 处理流程

### 1. 查询VIP记录
- 调用 `GET_VIP_CAR` 接口查询车牌的所有VIP记录
- 查找VIP类型为"停用tcjc"的记录

### 2. 提取有效期
- 从"停用tcjc"记录中提取开始时间和结束时间
- 格式: `yyyy-MM-dd HH:mm:ss`

### 3. 开通新VIP
- 调用 `OPEN_VIP_TICKET` 接口
- VIP类型名称: `请停车检查（化工西化肥西复合肥南）`
- 有效期: 与"停用tcjc"的有效期保持一致
- 车主信息: 使用原VIP的车主信息

## API接口

### 接口地址
```
POST /api/vip/fix/supplement-vip
```

### 请求参数
无需传参,车牌号列表已硬编码在接口中:
```java
String plateNumbersStr = "黑E627KW,黑E7105L,黑E8891S,黑EF50377,黑EFF1939,黑EJ3363,黑EP2999,黑M2D002";
```

### 响应示例

#### 成功响应
```json
{
  "code": 200,
  "message": "补开完成",
  "data": {
    "total": 8,
    "successCount": 8,
    "failedCount": 0,
    "skipCount": 0,
    "startTime": "2026-01-26 15:30:00",
    "endTime": "2026-01-26 15:30:15",
    "details": [
      "补开成功: 车牌=黑E627KW, VIP类型=请停车检查（化工西化肥西复合肥南）, 有效期=2026-01-01 00:00:00 ~ 2026-12-31 23:59:59",
      "补开成功: 车牌=黑E7105L, VIP类型=请停车检查（化工西化肥西复合肥南）, 有效期=2026-01-01 00:00:00 ~ 2026-12-31 23:59:59",
      ...
    ],
    "errors": []
  }
}
```

#### 失败响应
```json
{
  "code": 500,
  "message": "补开失败",
  "data": {
    "total": 8,
    "successCount": 0,
    "failedCount": 5,
    "skipCount": 3,
    "errors": [
      "车牌 黑E627KW 未找到'停用tcjc'类型的VIP",
      "补开VIP失败: 车牌=黑E7105L, VIP类型=请停车检查（化工西化肥西复合肥南）",
      ...
    ]
  }
}
```

## 核心代码

### Service层 - VipSupplementService

```java
@Service
public class VipSupplementService {
    
    /**
     * 为指定车牌列表补开VIP月票
     */
    public SupplementResult supplementVipForPlates(List<String> plateNumbers) {
        // 1. 遍历车牌列表
        for (String plateNumber : plateNumbers) {
            // 2. 查询VIP记录
            List<VipTicketInfo> vips = akeVipService.getVipTicket(plateNumber, null, null);
            
            // 3. 查找"停用tcjc"类型的VIP
            VipTicketInfo sourceVip = findSourceVip(vips);
            
            // 4. 补开VIP
            supplementSingleVip(plateNumber, sourceVip, result);
        }
        return result;
    }
    
    /**
     * 构建开通VIP请求
     */
    private OpenVipTicketRequest buildOpenVipTicketRequest(VipTicketInfo sourceVip) {
        OpenVipTicketRequest request = new OpenVipTicketRequest();
        
        // VIP类型
        request.setVipTypeName("请停车检查（化工西化肥西复合肥南）");
        
        // 车主信息(使用源VIP的信息)
        request.setCarOwner(sourceVip.getCarOwner());
        request.setTelphone(sourceVip.getTelphone());
        
        // 有效期(使用源VIP的有效期)
        timePeriod.setStartTime(sourceVip.getStartTime());
        timePeriod.setEndTime(sourceVip.getEndTime());
        
        return request;
    }
}
```

### Controller层 - VipFixController

```java
@RestController
@RequestMapping("/api/vip/fix")
public class VipFixController {
    
    @Autowired
    private VipSupplementService vipSupplementService;
    
    @PostMapping("/supplement-vip")
    @ApiOperation("为指定车牌补开VIP月票")
    public R supplementVipForPlates() {
        String plateNumbersStr = "黑E627KW,黑E7105L,黑E8891S,黑EF50377,黑EFF1939,黑EJ3363,黑EP2999,黑M2D002";
        
        // 解析车牌号
        String[] plates = plateNumbersStr.split("[,，;；\\s]+");
        List<String> plateList = Arrays.asList(plates);
        
        // 调用补开服务
        SupplementResult result = vipSupplementService.supplementVipForPlates(plateList);
        
        return R.ok()
                .message("补开完成")
                .data("total", result.getTotalPlateCount())
                .data("successCount", result.getSuccessCount())
                .data("failedCount", result.getFailedCount())
                .data("details", result.getDetails())
                .data("errors", result.getErrors());
    }
}
```

## 测试步骤

### 1. 启动应用
```bash
mvn spring-boot:run
```

### 2. 调用接口
使用Postman或curl调用接口:

```bash
curl -X POST http://localhost:8080/api/vip/fix/supplement-vip
```

### 3. 查看日志
观察控制台日志,确认处理结果:
```
========== 开始为车牌列表补开VIP月票，车牌数量: 8 ==========
处理车牌: 黑E627KW
车牌 黑E627KW 查询到 2 条VIP记录
找到源VIP: 车牌=黑E627KW, 类型=停用tcjc, 有效期=2026-01-01 00:00:00 ~ 2026-12-31 23:59:59
开始补开VIP: 车牌=黑E627KW, 源类型=停用tcjc, 目标类型=请停车检查（化工西化肥西复合肥南）
补开成功: 车牌=黑E627KW, VIP类型=请停车检查（化工西化肥西复合肥南）
...
========== 车牌列表VIP补开完成 ==========
VIP补开完成 - 处理车牌: 8个, 成功: 8个, 失败: 0个, 跳过: 0个
```

### 4. 验证结果
调用查询接口验证VIP是否补开成功:
```bash
curl -X POST http://localhost:8080/api/ake/vip/query \
  -H "Content-Type: application/json" \
  -d '{"carNo": "黑E627KW"}'
```

预期结果: 应该能看到"请停车检查(化工西化肥西复合肥南)"类型的VIP记录

## 注意事项

1. **有效期一致性**: 新开通的VIP有效期与"停用tcjc"的有效期完全一致
2. **车主信息**: 使用原VIP的车主姓名和电话
3. **不退费**: 此接口只补开新VIP,不会退费现有的VIP
4. **幂等性**: 如果已存在目标VIP类型,接口会继续执行(可能导致重复)
5. **错误处理**: 如果某个车牌处理失败,不影响其他车牌的处理

## 相关接口

- `GET_VIP_CAR`: 查询VIP记录
- `OPEN_VIP_TICKET`: 开通VIP月票
- `REFUND_VIP_TICKET`: VIP退费(本接口不使用)

## 文件清单

1. **Service层**
   - `src/main/java/com/parkingmanage/service/sync/VipSupplementService.java`

2. **Controller层**
   - `src/main/java/com/parkingmanage/controller/VipFixController.java` (新增方法)

3. **依赖服务**
   - `src/main/java/com/parkingmanage/service/ake/AkeVipService.java`

## 更新日志

- 2026-01-26: 创建VIP补开接口,支持8个指定车牌的VIP补开功能
