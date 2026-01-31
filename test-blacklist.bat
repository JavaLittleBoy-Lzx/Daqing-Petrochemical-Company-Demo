@echo off
chcp 65001 >nul
echo ========================================
echo 车辆黑名单同步测试脚本
echo ========================================
echo.

set BASE_URL=http://localhost:8080/api/sync/test/blacklist
set PLATE=黑A99999

echo [1] 查询初始状态（应该不存在）
curl -X GET "%BASE_URL%/query?plateNumber=%PLATE%"
echo.
echo.
pause

echo [2] 添加新黑名单（单门，临时）
curl -X POST "%BASE_URL%/add?plateNumber=%PLATE%&gateNames=化工西门&ownerName=测试用户&startTime=2026-01-15 00:00:00&endTime=2026-06-30 23:59:59"
echo.
echo.
pause

echo [3] 查询状态（应该存在）
curl -X GET "%BASE_URL%/query?plateNumber=%PLATE%"
echo.
echo.
pause

echo [4] 更新权限（改为多门）
curl -X POST "%BASE_URL%/update-permission?plateNumber=%PLATE%&newGateNames=化工西门,化肥西门&ownerName=测试用户"
echo.
echo.
pause

echo [5] 查询状态（权限应该已更新）
curl -X GET "%BASE_URL%/query?plateNumber=%PLATE%"
echo.
echo.
pause

echo [6] 更新时间（延长有效期）
curl -X POST "%BASE_URL%/update-time?plateNumber=%PLATE%&newStartTime=2026-01-15 00:00:00&newEndTime=2026-12-31 23:59:59&ownerName=测试用户"
echo.
echo.
pause

echo [7] 查询状态（时间应该已更新）
curl -X GET "%BASE_URL%/query?plateNumber=%PLATE%"
echo.
echo.
pause

echo [8] 删除黑名单
curl -X POST "%BASE_URL%/delete?plateNumber=%PLATE%"
echo.
echo.
pause

echo [9] 查询状态（应该不存在）
curl -X GET "%BASE_URL%/query?plateNumber=%PLATE%"
echo.
echo.

echo ========================================
echo 测试完成！
echo ========================================
pause
