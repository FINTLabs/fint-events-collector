FROM gradle:8.9-jdk21 as builder
#USER root
COPY . .
RUN gradle --no-daemon build

FROM gcr.io/distroless/java21
ENV JAVA_TOOL_OPTIONS -XX:+ExitOnOutOfMemoryError
COPY --from=builder /home/gradle/build/libs/fint-events-collector*.jar /data/fint-events-collector.jar
CMD ["/data/fint-events-collector.jar"]
