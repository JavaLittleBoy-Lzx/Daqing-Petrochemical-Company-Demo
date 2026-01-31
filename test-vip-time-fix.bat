@echo off
chcp 65001 >nul
echo ========================================
echo VIP时间字段修复验证测试
echo ========================================
echo.
echo 📝 测试说明：
echo 此脚本用于验证VIP查询接口是否能正确显示有效期
echo.
echo 🔧 修复内容：
echo - 修改了AkeVipService.parseVipTicketResponse方法
echo - 现在会从time_period字段解析时间（格式：开始时间~结束时间）
echo - 兼容处理：如果time_period不存在，尝试start_time和end_time
echo.
echo ⚠️  请确保应用已重启后再运行此测试
echo.
pause

set BASE_URL=http://localhost:8080/api/sync/test/vip

echo.
echo ========================================
echo 测试1：查询现有VIP记录（黑EQG165）
echo ========================================
echo 预期结果：应该显示有效期 2026-01-14 00:00:00 ~ 2026-12-21 23:59:59
echo.
curl -X GET "%BASE_URL%/query?plateNumber=黑EQG165"
echo.
echo.
pause

echo.
echo ========================================
echo 测试2：开通新VIP（测试车牌）
echo ========================================
echo 车牌：黑A88888
echo 门：化工西门
echo 有效期：2026-01-15 00:00:00 ~ 2026-06-30 23:59:59
echo.
curl -X POST "%BASE_URL%/add?plateNumber=黑A88888&gateNames=化工西门&ownerName=测试用户&startTime=2026-01-15 00:00:00&endTime=2026-06-30 23:59:59"
echo.
echo.
pause

echo.
echo ========================================
echo 测试3：查询新开通的VIP
echo ========================================
echo 预期结果：应该显示有效期 2026-01-15 00:00:00 ~ 2026-06-30 23:59:59
echo.
curl -X GET "%BASE_URL%/query?plateNumber=黑A88888"
echo.
echo.
pause

echo.
echo ========================================
echo 测试4：更新VIP时间（续费）
echo ========================================
echo 延长有效期到：2026-12-31 23:59:59
echo.
curl -X POST "%BASE_URL%/update-time?plateNumber=黑A88888&newStartTime=2026-01-15 00:00:00&newEndTime=2026-12-31 23:59:59&ownerName=测试用户"
echo.
echo.
pause

echo.
echo ========================================
echo 测试5：再次查询VIP
echo ========================================
echo 预期结果：应该显示更新后的有效期 2026-01-15 00:00:00 ~ 2026-12-31 23:59:59
echo.
curl -X GET "%BASE_URL%/query?plateNumber=黑A88888"
echo.
echo.
pause

echo.
echo ========================================
echo 测试6：清理测试数据（退费VIP）
echo ========================================
curl -X POST "%BASE_URL%/refund?plateNumber=黑A88888"
echo.
echo.
pause

echo.
echo ========================================
echo ✅ 测试完成！
echo ========================================
echo.
echo 📊 验证要点：
echo 1. 所有查询结果中的"有效期"字段应该正确显示时间
echo 2. 不应该再出现 "null ~ null" 的情况
echo 3. 时间格式应该是：yyyy-MM-dd HH:mm:ss ~ yyyy-MM-dd HH:mm:ss
echo.
echo 如果测试通过，说明修复成功！
echo.
pause
