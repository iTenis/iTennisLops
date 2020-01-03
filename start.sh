#!/usr/bin/env bash
#
##停止容器
#docker stop $(docker ps -a | grep "Exited" | awk '{print $1 }') &> /dev/null
#echo "停止容器..."
##删除容器
#docker rm $(docker ps -a | grep "Exited" | awk '{print $1 }') &> /dev/null
#echo "删除容器..."
##删除镜像
#docker rmi $(docker images | grep "none" | awk '{print $3}') &> /dev/null
#echo "删除镜像..."

#删除web的docker容器和镜像
docker rmi $(docker ps -a | grep itennis-web | awk '{print $1}') &> /dev/null
docker rmi $(docker images | grep itennis-lops_web | awk '{print $3}') &> /dev/null

#删除web的docker容器和镜像
docker rmi $(docker ps -a | grep itennis-lops_api | awk '{print $1}') &> /dev/null
docker rmi $(docker images | grep itennis-lops_api | awk '{print $3}') &> /dev/null

echo "前台启动服务..."
docker-compose up

#后台启动服务，并指定yml文件
#echo "后台启动服务..."
#docker-compose -f docker-compose.yml up -d