#!/usr/bin/env bash
kill -9 `ps -ef | grep java | grep iTennisLops | awk '{print $2}'`
if [ $?=0 ];then
    echo "服务清除成功！"
else
    echo "服务清除失败！"
fi
rm -rf jobs