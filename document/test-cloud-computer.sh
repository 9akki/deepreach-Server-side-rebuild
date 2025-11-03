#!/bin/bash

# 云电脑接口测试脚本

echo "=================================="
echo "云电脑参数查询接口测试"
echo "=================================="

BASE_URL="http://localhost:8080/api"

# 测试用户ID（需要根据实际情况调整）
USER_ID=1

echo ""
echo "1. 测试获取指定用户的云电脑参数"
echo "URL: $BASE_URL/cloud-computer/parameter/$USER_ID"
curl -X GET "$BASE_URL/cloud-computer/parameter/$USER_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-token-here" \
  --connect-timeout 10 \
  --max-time 30

echo ""
echo ""
echo "----------------------------------"
echo ""

echo "2. 测试获取当前用户的云电脑参数"
echo "URL: $BASE_URL/cloud-computer/parameter/current"
curl -X GET "$BASE_URL/cloud-computer/parameter/current" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-token-here" \
  --connect-timeout 10 \
  --max-time 30

echo ""
echo ""
echo "----------------------------------"
echo ""

echo "3. 测试不存在的用户"
echo "URL: $BASE_URL/cloud-computer/parameter/999999"
curl -X GET "$BASE_URL/cloud-computer/parameter/999999" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-token-here" \
  --connect-timeout 10 \
  --max-time 30

echo ""
echo ""
echo "=================================="
echo "测试完成"
echo "=================================="

# 数据库连接测试
echo ""
echo "4. 测试云电脑数据库连接"
mysql -h 206.82.1.18 -P 33900 -u root -p8n2MDPdMb4qpRHYR cloud-computer -e "
SELECT 'Testing cloud computer database connection...' as message;
SHOW TABLES;
"

echo ""
echo "=================================="
echo "数据库测试完成"
echo "=================================="