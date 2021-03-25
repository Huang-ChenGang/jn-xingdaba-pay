FROM hub.c.163.com/cloudndp/library/openjdk:jdk-11

MAINTAINER hcg hcggang@163.com

ADD xingdaba-pay-application/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone