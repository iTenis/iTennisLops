# 指定基础镜像
FROM maven:3.5.4-jdk-8
# 维护者信息
MAINTAINER xiehuisheng "xiehuisheng@hotmail.com"

RUN echo "-------------------- api环境配置 --------------------"

# 暴露9101端口
EXPOSE 8088
# 设置环境编码UTF-8
ENV LANG C.UTF-8

#指定挂载点
#VOLUME /tmp
#将本地文件或目录复制到由dockerfile构建的镜像中
#ADD maintenance-platform-1.0.jar maintenance-platform-1.0.jar
#设置环境变量
#ENV JAVA_OPTS=""
#提供容器运行的默认命令
#ENTRYPOINT ["java", "-jar", "/maintenance-platform-1.0.jar"]