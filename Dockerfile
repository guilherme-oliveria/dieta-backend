# Imagem da API (BE-32a). Build: docker build -t dietamais-api .
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --no-daemon --version
COPY src src
RUN ./gradlew --no-daemon bootJar -x test \
	&& cp "$(ls build/libs/*.jar | grep -v -- '-plain' | head -n 1)" /app.jar

FROM eclipse-temurin:25-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /app.jar app.jar
# Arquivos gerados (PDFs, logo) quando não há S3: diretório do volume `arquivos` do compose (BE-28)
RUN mkdir -p /arquivos && chown app:app /arquivos
USER app
ENV SPRING_PROFILES_ACTIVE=prod \
	JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
