FROM openjdk:21-jdk-slim

LABEL authors="ruserxd"

# 常數
ENV APP_DIR=/app

# 建立目錄
RUN mkdir -p ${APP_DIR}/config

# 設定工作目錄
WORKDIR ${APP_DIR}

# 複製 jar 檔案 
COPY ./target/CodeManageSystem-0.0.1-SNAPSHOT.jar ${APP_DIR}/app.jar
COPY ./src/main/resources/application.properties ${APP_DIR}/config/

# 端口
EXPOSE 8080

# 執行 jar 檔案
CMD ["java", "-Dspring.config.location=config/", "-jar", "app.jar", "--spring.config.name=application"]