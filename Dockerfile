FROM eclipse-temurin:11-jdk

# Set the working directory directly inside the container
WORKDIR /app

# Copy all project files into the container
COPY . .

# Ensure the Maven wrapper script has executable permissions
RUN chmod +x mvnw

# Compile the application (downloads all Maven dependencies into the container)
RUN ./mvnw clean compile

# Expose port 8080 to the outside world
EXPOSE 8080

# Command to start the Grizzly server natively when the container runs
CMD ["./mvnw", "exec:java"]