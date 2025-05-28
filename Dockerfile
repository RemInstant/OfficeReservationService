FROM eclipse-temurin:22-alpine
ARG JAR_FILE=build/libs/*.jar
RUN mkdir /opt/app
COPY ${JAR_FILE} /opt/app/japp.jar
CMD ["java", "-jar", "/opt/app/japp.jar"]