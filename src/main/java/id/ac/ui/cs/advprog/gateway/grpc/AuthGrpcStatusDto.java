package id.ac.ui.cs.advprog.gateway.grpc;

import id.ac.ui.cs.advprog.bidmart.auth.grpc.AuthServiceStatusResponse;

public record AuthGrpcStatusDto(
        boolean healthy,
        String serviceName,
        String issuer,
        String jwksPath,
        String message
) {

    public static AuthGrpcStatusDto from(final AuthServiceStatusResponse response) {
        return new AuthGrpcStatusDto(
                response.getHealthy(),
                response.getServiceName(),
                response.getIssuer(),
                response.getJwksPath(),
                response.getMessage()
        );
    }
}
