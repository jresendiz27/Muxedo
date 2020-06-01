FROM openjdk:8-alpine
LABEL mainteiner=jresendiz27@gmail.com
RUN mkdir /app
WORKDIR /app
COPY build/libs/Muxedo-all.jar /app/muxedo-all.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/muxedo-all.jar"]
