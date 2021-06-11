FROM gradle:6.2.2-jdk11 as builder
#USER root
COPY . .
RUN gradle --no-daemon build

FROM gcr.io/distroless/java:11
ENV JAVA_TOOL_OPTIONS -XX:+ExitOnOutOfMemoryError
COPY --from=builder /home/gradle/build/deps/external/*.jar /data/
COPY --from=builder /home/gradle/build/deps/fint/*.jar /data/
COPY --from=builder /home/gradle/build/libs/fint-events-collector-*.jar /data/fint-events-collector.jar
CMD ["/data/fint-events-collector.jar"]
