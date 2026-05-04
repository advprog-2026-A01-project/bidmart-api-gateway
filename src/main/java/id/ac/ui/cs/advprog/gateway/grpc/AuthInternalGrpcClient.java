package id.ac.ui.cs.advprog.gateway.grpc;

import id.ac.ui.cs.advprog.bidmart.auth.grpc.AuthInternalServiceGrpc;
import id.ac.ui.cs.advprog.bidmart.auth.grpc.AuthServiceStatusRequest;
import id.ac.ui.cs.advprog.bidmart.auth.grpc.AuthServiceStatusResponse;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthInternalGrpcClient implements DisposableBean {

    private static final String CALLER_SERVICE = "bidmart-api-gateway";
    private static final long DEADLINE_SECONDS = 2L;

    private final ManagedChannel channel;
    private final AuthInternalServiceGrpc.AuthInternalServiceBlockingStub blockingStub;

    public AuthInternalGrpcClient(
            @Value("${services.auth.grpc.host}") final String host,
            @Value("${services.auth.grpc.port}") final int port
    ) {
        this.channel = NettyChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = AuthInternalServiceGrpc.newBlockingStub(channel);
    }

    public AuthServiceStatusResponse getAuthServiceStatus() {
        final AuthServiceStatusRequest request = AuthServiceStatusRequest.newBuilder()
                .setCallerService(CALLER_SERVICE)
                .build();

        return blockingStub
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                .getAuthServiceStatus(request);
    }

    @Override
    public void destroy() throws InterruptedException {
        channel.shutdown();
        if (!channel.awaitTermination(DEADLINE_SECONDS, TimeUnit.SECONDS)) {
            channel.shutdownNow();
        }
    }
}
