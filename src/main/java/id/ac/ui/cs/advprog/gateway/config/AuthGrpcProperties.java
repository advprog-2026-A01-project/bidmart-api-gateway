package id.ac.ui.cs.advprog.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services.auth.grpc")
public record AuthGrpcProperties(String host, int port) {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 9091;

    public AuthGrpcProperties(final String host, final int port) {
        this.host = normalizeHost(host);
        this.port = normalizePort(port);
    }

    private static String normalizeHost(final String rawHost) {
        if (rawHost == null || rawHost.isBlank()) {
            return DEFAULT_HOST;
        }
        return rawHost;
    }

    private static int normalizePort(final int rawPort) {
        if (rawPort <= 0) {
            return DEFAULT_PORT;
        }
        return rawPort;
    }
}
