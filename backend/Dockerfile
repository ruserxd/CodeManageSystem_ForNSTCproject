# 把 Maven, 程式碼, 工具建立 jar
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 只先放「建置工具」和「套件清單」
COPY mvnw pom.xml ./
COPY .mvn ./.mvn
RUN ./mvnw dependency:go-offline -B

# 放程式碼
COPY src ./src
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:21-jre

WORKDIR /app
RUN useradd -m appuser && mkdir -p ./src/cloneCode ./src/log && chown -R appuser ./src/cloneCode ./src/log

COPY --from=build /workspace/target/CodeManageSystem-0.0.1-SNAPSHOT.jar ./app.jar
EXPOSE 8080

USER appuser
CMD ["java", "-jar", "app.jar"]
