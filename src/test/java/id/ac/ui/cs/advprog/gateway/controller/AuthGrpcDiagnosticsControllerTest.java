package id.ac.ui.cs.advprog.gateway.controller;

import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.bidmart.auth.grpc.AuthServiceStatusResponse;
import id.ac.ui.cs.advprog.gateway.grpc.AuthInternalGrpcClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class AuthGrpcDiagnosticsControllerTest {

    private static final String AUTH_GRPC_STATUS_PATH = "/api/gateway/internal/auth-grpc/status";
    private static final String SERVICE_NAME = "bidmart-auth-service";
    private static final String ISSUER = "http://localhost:8081";
    private static final String JWKS_PATH = "/.well-known/jwks.json";

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AuthInternalGrpcClient authInternalGrpcClient;

    @Test
    void statusReturnsAuthServiceStatusWithoutJwt() {
        final AuthServiceStatusResponse response = AuthServiceStatusResponse.newBuilder()
                .setHealthy(true)
                .setServiceName(SERVICE_NAME)
                .setIssuer(ISSUER)
                .setJwksPath(JWKS_PATH)
                .setMessage("gRPC auth internal service reachable by bidmart-api-gateway")
                .build();

        when(authInternalGrpcClient.getAuthServiceStatus()).thenReturn(response);

        webTestClient.get()
                .uri(AUTH_GRPC_STATUS_PATH)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.healthy").isEqualTo(true)
                .jsonPath("$.serviceName").isEqualTo(SERVICE_NAME)
                .jsonPath("$.issuer").isEqualTo(ISSUER)
                .jsonPath("$.jwksPath").isEqualTo(JWKS_PATH)
                .jsonPath("$.message").isEqualTo("gRPC auth internal service reachable by bidmart-api-gateway");
    }

    @Test
    void statusMapsGrpcFailureToBadGateway() {
        when(authInternalGrpcClient.getAuthServiceStatus())
                .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        webTestClient.get()
                .uri(AUTH_GRPC_STATUS_PATH)
                .exchange()
                .expectStatus().isEqualTo(502);
    }
}
