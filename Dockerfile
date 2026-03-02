# Stage 1: Build the mod
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build
COPY sharkengine/ ./

RUN chmod +x gradlew && ./gradlew --no-daemon build -x test

# Stage 2: Run Fabric server with the mod
FROM eclipse-temurin:21-jre

WORKDIR /server

# Fabric server installer (loader 0.17.0, installer 1.0.1, MC 1.21.1)
ADD https://meta.fabricmc.net/v2/versions/loader/1.21.1/0.17.0/1.0.1/server/jar fabric-server.jar

# Create mods directory and copy built mod
RUN mkdir -p mods world

# Copy mod JAR from builder (exclude sources jar)
COPY --from=builder /build/build/libs/sharkengine-*.jar mods/

# Download Fabric API matching the mod's required version
ADD https://cdn.modrinth.com/data/P7dR8mSH/versions/biIRIp2X/fabric-api-0.114.0%2B1.21.1.jar mods/fabric-api.jar

# Server config
COPY server/server.properties .
RUN echo "eula=true" > eula.txt

EXPOSE 25565

# JVM flags optimized for containerized Minecraft
CMD ["java", \
     "-Xms1G", "-Xmx2G", \
     "-XX:+UseG1GC", \
     "-XX:+ParallelRefProcEnabled", \
     "-XX:MaxGCPauseMillis=200", \
     "-XX:+UnlockExperimentalVMOptions", \
     "-XX:+DisableExplicitGC", \
     "-XX:G1HeapRegionSize=8M", \
     "-XX:G1NewSizePercent=30", \
     "-XX:G1MaxNewSizePercent=40", \
     "-XX:G1MixedGCLiveThresholdPercent=90", \
     "-XX:G1RSetUpdatingPauseTimePercent=5", \
     "-jar", "fabric-server.jar", "nogui"]
