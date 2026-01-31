# VIP月票同步测试接口说明

## 概述

为了测试车辆VIP月票同步功能，我们提供了5个测试接口，可以模拟VIP月票同步的各种场景。

## 测试接口列表

### 1. 查询车辆VIP状态

**接口地址：** `GET /api/sync/test/vip/query`

**功能：** 查询指定车牌号的VIP票状态

**参数：**
- `plateNumber` (必填): 车牌号，如 "黑A12345"

**示例：**
```
GET http://localhost:8080/api/sync/test/vip/query?plateNumber=黑A12345
```

**返回示例：**
```json
{
  "success": true,
  "code": 20000,
  "message": "📋 VIP票信息\n共 1 条记录\n\n【记录 1】\n票号: 黑A12345_1736905200000\n VIP类型: 化工西化肥西VIP\n车牌号: 黑A12345\n车主: 测试车主\n状态: 生效中\n有效期: 2026-01-15 00:00:00 ~ 2026-12-31 23:59:59"
}
```

---

### 2. 测试开通新VIP月票（场景1）

**接口地址：** `POST /api/sync/test/vip/add`

**功能：** 测试开通新VIP月票（无现有VIP的情况）

**参数：**
- `plateNumber` (必填): 车牌号，如 "黑A12345"
- `gateNames` (必填): 门名称列表，逗号分隔，如 "化工西门,化肥西门"
- `ownerName` (可选): 车主姓名，默认 "测试车主"
- `startTime` (必填): 开始时间，格式 "yyyy-MM-dd HH:mm:ss"
- `endTime` (必填): 结束时间，格式 "yyyy-MM-dd HH:mm:ss"

**测试场景：**

#### 场景1.1：开通单门VIP
```
POST http://localhost:8080/api/sync/test/vip/add
Content-Type: application/x-www-form-urlencoded

plateNumber=黑A12345
&gateNames=化工西门
&ownerName=张三
&startTime=2026-01-15 00:00:00
&endTime=2026-12-31 23:59:59
```

**预期结果：**
- ✅ 开通VIP月票成功
- VIP类型名称：`化工西VIP`
- 费用：0元

#### 场景1.2：开通多门VIP
```
POST http://localhost:8080/api/sync/test/vip/add
Content-Type: application/x-www-form-urlencoded

plateNumber=黑A23456
&gateNames=化工西门,化肥西门,复合肥南门
&ownerName=李四
&startTime=2026-01-15 00:00:00
&endTime=2026-12-31 23:59:59
```

**预期结果：**
- ✅ 开通VIP月票成功
- VIP类型名称：`化工西化肥西复合肥南VIP`
- 费用：0元

---

### 3. 测试VIP权限变化（场景2）

**接口地址：** `POST /api/sync/test/vip/update-permission`

**功能：** 测试VIP权限变化（先退费，再开通新VIP）

**参数：**
- `plateNumber` (必填): 车牌号
- `newGateNames` (必填): 新的门名称列表，逗号分隔
- `ownerName` (可选): 车主姓名，默认 "测试车主"

**前置条件：** 车辆必须已有生效中的VIP票

**测试场景：**

#### 场景2.1：从单门改为多门
```
# 先开通单门VIP
POST http://localhost:8080/api/sync/test/vip/add
plateNumber=黑A12345&gateNames=化工西门&startTime=2026-01-15 00:00:00&endTime=2026-12-31 23:59:59

# 再更新为多门
POST http://localhost:8080/api/sync/test/vip/update-permission
plateNumber=黑A12345&newGateNames=化工西门,化肥西门
```

**预期结果：**
- ✅ VIP权限变化处理成功
- 旧权限：`化工西VIP`
- 新权限：`化工西门, 化肥西门`
- 操作：先退费，再开通新VIP（0元）

#### 场景2.2：从多门改为单门
```
POST http://localhost:8080/api/sync/test/vip/update-permission
plateNumber=黑A12345&newGateNames=化工西门
```

**预期结果：**
- ✅ VIP权限变化处理成功
- 旧权限：`化工西化肥西VIP`
- 新权限：`化工西门`
- 操作：先退费，再开通新VIP（0元）

---

### 4. 测试VIP时间更新（场景3）

**接口地址：** `POST /api/sync/test/vip/update-time`

**功能：** 测试VIP时间更新（调用续费接口）

**参数：**
- `plateNumber` (必填): 车牌号
- `newStartTime` (必填): 新的开始时间，格式 "yyyy-MM-dd HH:mm:ss"
- `newEndTime` (必填): 新的结束时间，格式 "yyyy-MM-dd HH:mm:ss"
- `ownerName` (可选): 车主姓名，默认 "测试车主"

**前置条件：** 车辆必须已有生效中的VIP票

**测试场景：**

#### 场景3.1：延长有效期
```
# 先开通VIP
POST http://localhost:8080/api/sync/test/vip/add
plateNumber=黑A12345&gateNames=化工西门&startTime=2026-01-15 00:00:00&endTime=2026-06-30 23:59:59

# 延长有效期到年底
POST http://localhost:8080/api/sync/test/vip/update-time
plateNumber=黑A12345&newStartTime=2026-01-15 00:00:00&newEndTime=2026-12-31 23:59:59
```

**预期结果：**
- ✅ VIP时间更新成功
- 旧时间：`2026-01-15 00:00:00 ~ 2026-06-30 23:59:59`
- 新时间：`2026-01-15 00:00:00 ~ 2026-12-31 23:59:59`
- 操作：调用续费接口

#### 场景3.2：缩短有效期
```
POST http://localhost:8080/api/sync/test/vip/update-time
plateNumber=黑A12345&newStartTime=2026-01-15 00:00:00&newEndTime=2026-03-31 23:59:59
```

**预期结果：**
- ✅ VIP时间更新成功
- 操作：调用续费接口

---

### 5. 测试退费VIP（场景4）

**接口地址：** `POST /api/sync/test/vip/refund`

**功能：** 测试退费VIP（模拟注销状态 DQZT=D）

**参数：**
- `plateNumber` (必填): 车牌号

**前置条件：** 车辆必须已有生效中的VIP票

**测试场景：**

#### 场景4.1：退费VIP
```
# 先开通VIP
POST http://localhost:8080/api/sync/test/vip/add
plateNumber=黑A12345&gateNames=化工西门&startTime=2026-01-15 00:00:00&endTime=2026-12-31 23:59:59

# 退费VIP
POST http://localhost:8080/api/sync/test/vip/refund
plateNumber=黑A12345
```

**预期结果：**
- ✅ 退费VIP成功
- 操作：检测到注销状态(DQZT=D)，退费VIP（0元）

---

## 完整测试流程示例

### 测试流程1：完整的生命周期

```bash
# 1. 查询初始状态（应该不存在）
GET /api/sync/test/vip/query?plateNumber=黑A88888

# 2. 开通VIP月票（单门）
POST /api/sync/test/vip/add
plateNumber=黑A88888&gateNames=化工西门&ownerName=测试用户&startTime=2026-01-15 00:00:00&endTime=2026-06-30 23:59:59

# 3. 查询状态（应该存在）
GET /api/sync/test/vip/query?plateNumber=黑A88888

# 4. 更新权限（改为多门）
POST /api/sync/test/vip/update-permission
plateNumber=黑A88888&newGateNames=化工西门,化肥西门

# 5. 查询状态（权限应该已更新）
GET /api/sync/test/vip/query?plateNumber=黑A88888

# 6. 更新时间（延长有效期）
POST /api/sync/test/vip/update-time
plateNumber=黑A88888&newStartTime=2026-01-15 00:00:00&newEndTime=2026-12-31 23:59:59

# 7. 查询状态（时间应该已更新）
GET /api/sync/test/vip/query?plateNumber=黑A88888

# 8. 退费VIP
POST /api/sync/test/vip/refund
plateNumber=黑A88888

# 9. 查询状态（应该不存在或状态为已退费）
GET /api/sync/test/vip/query?plateNumber=黑A88888
```

---

## 注意事项

1. **测试环境：** 这些接口仅用于测试，不应在生产环境使用
2. **车牌号格式：** 建议使用测试车牌号（如 黑A88888），避免影响真实数据
3. **时间格式：** 所有时间参数必须使用格式 `yyyy-MM-dd HH:mm:ss`
4. **门名称：** 必须使用Oracle数据库中的完整门名称（如 "化工西门"），系统会自动转换为ake简化名称
5. **权限合并：** 多个门名称会自动合并为一个VIP类型名称
6. **费用：** 所有VIP操作（开通/退费/续费）都是0元

---

## 支持的门名称列表

根据设计文档，系统支持以下门名称：

1. 炼油南门
2. 炼油南一
3. 炼油西一
4. 炼油西门
5. 炼油东门
6. 化肥西门
7. 化工西门
8. 化工北门
9. 化工东门
10. 化三南门
11. 复合肥南门
12. 机修厂西一
13. 二罐区正门

**门名称映射规则：**
- Oracle完整名称 → ake简化名称
- 化工西门 → 化工西
- 化肥西门 → 化肥西
- 复合肥南门 → 复合肥南
- 其他门名称去除"门"字后缀

---

## VIP类型名称格式

**格式：** `简化名称1+简化名称2+...+VIP`

**示例：**
- 单门：`化工西VIP`
- 双门：`化工西化肥西VIP`
- 三门：`化工西化肥西复合肥南VIP`

---

## VIP同步流程说明

### 场景1：开通新VIP（无现有VIP）

```
1. 查询ake系统是否有生效中的VIP票
2. 如果没有，直接调用OPEN_VIP_TICKET接口开通（0元）
3. VIP类型名称 = 合并后的门权限 + "VIP"
```

### 场景2：权限变化（有现有VIP，权限不同）

```
1. 查询ake系统的VIP票
2. 提取Oracle权限集合和ake权限集合
3. 比较权限是否相同
4. 如果不同：
   a. 调用REFUND_VIP_TICKET接口退费（0元）
   b. 调用OPEN_VIP_TICKET接口开通新VIP（0元，使用新权限）
```

### 场景3：时间更新（有现有VIP，权限相同，时间不同）

```
1. 查询ake系统的VIP票
2. 提取Oracle权限集合和ake权限集合
3. 比较权限是否相同
4. 如果相同：
   a. 比较时间是否相同
   b. 如果时间不同：调用RENEW_VIP_TICKET接口续费
   c. 如果时间相同：跳过
```

### 场景4：注销状态（DQZT=D）

```
1. 检查DQZT状态
2. 如果是"D"（注销）：
   a. 调用REFUND_VIP_TICKET接口退费（0元）
   b. 结束处理
```

---

## 故障排查

### 问题1：接口返回 "测试服务未启用"

**原因：** VehicleBlacklistTestService 未被Spring容器加载

**解决方案：** 
1. 检查服务类上有 `@Service` 注解
2. 重启应用程序
3. 检查Spring扫描路径

### 问题2：开通VIP失败

**可能原因：**
1. 车辆已有生效中的VIP票（使用查询接口确认）
2. AKE接口连接失败（检查配置文件中的ake.api.base-url）
3. 门名称格式错误（必须使用完整门名称）

**解决方案：**
1. 先查询状态，如果存在则先退费
2. 检查网络连接和配置
3. 使用支持的门名称列表中的名称

### 问题3：权限变化或时间更新失败

**可能原因：**
1. 车辆不存在生效中的VIP票
2. 接口调用失败

**解决方案：**
1. 先查询状态确认VIP存在
2. 检查日志查看具体错误信息

### 问题4：curl命令中文乱码

**原因：** Windows命令行编码问题

**解决：**
1. 使用测试脚本（已设置UTF-8编码）
2. 或在命令行执行：`chcp 65001`
3. 或使用Postman

---

## VIP vs 黑名单对比

| 特性 | VIP月票 | 黑名单 |
|------|---------|--------|
| 类型名称格式 | `门名称+VIP` | `请停车检查（门名称）` |
| 权限变化处理 | 先退费，再开通 | 先删除，再添加 |
| 时间更新处理 | 调用续费接口 | 先删除，再添加 |
| 注销状态处理 | 退费（0元） | 删除 |
| 永久类型 | 不支持 | 支持 |
| 费用 | 0元 | N/A |

---

## 下一步

测试成功后，可以：

1. ✅ 查看日志文件，确认同步逻辑正确
2. ✅ 在AKE系统中验证VIP数据
3. ✅ 启用自动同步任务
4. ✅ 从Oracle数据库同步真实数据

---

## 相关文档

- 📄 `黑名单同步测试接口说明.md` - 黑名单测试接口文档
- 📄 `VIP月票测试接口-Postman集合.json` - Postman测试集合
- 📄 `test-vip.bat` - 自动化测试脚本
- 📄 `.kiro/specs/daqing-petrochemical-sync/design.md` - 设计文档

---

**文档版本：** 1.0  
**创建日期：** 2026-01-15  
**作者：** Kiro AI Assistant
