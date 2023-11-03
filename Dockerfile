FROM openjdk:17-jdk
COPY ./*SNAPSHOT.jar /app.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=common,deploy", "-jar", "/app.jar"]