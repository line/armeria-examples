package example.armeria.server.auth;

import static com.linecorp.armeria.common.HttpHeaderNames.AUTHORIZATION;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.auth.Authorizer;

/**
 * An example of an {@link Authorizer}.
 *
 * <p>Note that the implementation in this class is purely for a demonstration purpose.
 * You should perform proper authorization in a real world application.
 */
final class MyAuthHandler implements Authorizer<HttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(MyAuthHandler.class);

    /**
     * Checks whether the request has right permission to access the service. In this example, a {@code AUTHORIZATION}
     * header is used to hold the information. If a {@code AUTHORIZATION} key exists in the header,
     * the request is treated as authenticated.
     */
    @Override
    public CompletionStage<Boolean> authorize(ServiceRequestContext ctx, HttpRequest req) {
        return CompletableFuture.supplyAsync(
                () -> "token".equals(req.headers().get(AUTHORIZATION)));
    }
}

