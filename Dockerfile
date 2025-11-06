FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY . .
RUN javac -cp ".:postgresql-42.7.8.jar" ChatApp.java
CMD ["java", "-cp", ".:postgresql-42.7.8.jar", "ChatApp"]
