package id.ac.ui.cs.advprog.gateway.controller;

import id.ac.ui.cs.advprog.gateway.grpc.AuthGrpcStatusDto;
import id.ac.ui.cs.advprog.gateway.grpc.AuthInternalGrpcClient;
import io.grpc.StatusRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/gateway/internal/auth-grpc")
public class AuthGrpcDiagnosticsController {

    private final AuthInternalGrpcClient authInternalGrpcClient;

    public AuthGrpcDiagnosticsController(final AuthInternalGrpcClient authInternalGrpcClient) {
        this.authInternalGrpcClient = authInternalGrpcClient;
    }

    @GetMapping("/status")
    public Mono<AuthGrpcStatusDto> status() {
        return Mono.fromCallable(authInternalGrpcClient::getAuthServiceStatus)
                .map(AuthGrpcStatusDto::from)
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(StatusRuntimeException.class, AuthGrpcDiagnosticsController::toBadGateway);
    }

    private static ResponseStatusException toBadGateway(final StatusRuntimeException exception) {
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Auth gRPC service is unavailable: " + exception.getStatus().getCode(),
                exception
        );
    }
}
