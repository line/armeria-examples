package example.armeria.server.auth;

import java.util.concurrent.CompletableFuture;
import static com.linecorp.armeria.common.HttpHeaderNames.AUTHORIZATION;


import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.server.*;
import com.linecorp.armeria.server.auth.AuthService;
import com.linecorp.armeria.server.auth.AuthServiceBuilder;
import com.linecorp.armeria.server.auth.Authorizer;
import com.linecorp.armeria.server.logging.LoggingService;


public class Main {
    public static void main(String[] args) throws Exception {
        ServerBuilder sb = Server.builder();
        sb.http(8089);
        //The Hello Word services
        sb.service("/", new AbstractHttpService() {
            @Override
            protected HttpResponse doGet(ServiceRequestContext ctx, HttpRequest req) {
                return HttpResponse.of("Hello,word");
            }
        });

        AuthServiceBuilder authServiceBuilder= AuthService.builder();
        authServiceBuilder.add(new MyAuthHandler());
        AuthService authService =authServiceBuilder.build(new AbstractHttpService() {
            @Override
            protected HttpResponse doGet(ServiceRequestContext ctx, HttpRequest req) {
                String name = ctx.pathParam("name");
                return HttpResponse.of("Hello, %s!", name);
            }
        });
        sb.service("/greet/{name}",authService);


        final HttpService service = new AbstractHttpService() {
            @Override
            protected HttpResponse doGet(ServiceRequestContext ctx, HttpRequest req) {
                return HttpResponse.of(HttpStatus.OK);
            }
        };
        // Auth with arbitrary authorizer
        final Authorizer<HttpRequest> authorizer = (ctx, req) ->
                CompletableFuture.supplyAsync(
                        () -> "token".equals(req.headers().get(AUTHORIZATION)));
        sb.service("/welcom",
                service.decorate(AuthService.newDecorator(authorizer))
                        .decorate(LoggingService.newDecorator()));

        Server server = sb.build();
        CompletableFuture<Void> future = server.start();
        future.join();
    }
}
