#!/usr/bin/env bash
# 백엔드(bootRun/gradle)와 vite/esbuild 전부 종료
pkill -9 -f "bootRun" 2>/dev/null
pkill -9 -f "GradleWrapperMain" 2>/dev/null
pkill -9 -f "org.gradle" 2>/dev/null
for port in 8080 5173 5174; do
  pids=$(ss -ltnp 2>/dev/null | grep ":$port " | grep -oE 'pid=[0-9]+' | cut -d= -f2)
  for p in $pids; do kill -9 "$p" 2>/dev/null; done
done
pkill -9 -f "vite" 2>/dev/null
pkill -9 -f "esbuild" 2>/dev/null
sleep 2
echo "--- remaining 8080/5173 ---"
ss -ltn 2>/dev/null | grep -E ":8080 |:5173 " || echo "all clear"
