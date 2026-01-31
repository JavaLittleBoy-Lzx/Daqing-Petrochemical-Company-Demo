@echo off
chcp 65001 >nul
echo ========================================
echo VIP补开接口测试
echo ========================================
echo.
echo 目标车牌: 黑E627KW,黑E7105L,黑E8891S,黑EF50377,黑EFF1939,黑EJ3363,黑EP2999,黑M2D002
echo.
echo 正在调用接口...
echo.

curl -X POST http://localhost:8080/api/vip/fix/supplement-vip ^
  -H "Content-Type: application/json" ^
  -w "\n\n状态码: %%{http_code}\n" ^
  -s

echo.
echo ========================================
echo 测试完成
echo ========================================
pause
