# ---------- build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
# Windows checkouts can leave CRLF line endings, which break the script on Linux
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# resolve dependencies first so rebuilds are fast
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app \
 && mkdir -p /data/storage && chown -R app:app /data

COPY --from=build /workspace/build/libs/*-SNAPSHOT.jar app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=Asia/Kolkata", "-jar", "app.jar"]