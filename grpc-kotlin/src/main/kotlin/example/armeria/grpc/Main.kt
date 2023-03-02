package example.armeria.grpc

import com.linecorp.armeria.common.grpc.GrpcSerializationFormats
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.ServerBuilder
import com.linecorp.armeria.server.docs.DocService
import com.linecorp.armeria.server.docs.DocServiceFilter
import com.linecorp.armeria.server.grpc.GrpcService
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.reflection.v1alpha.ServerReflectionGrpc
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("example.armeria.grpc.MainKt")

fun main() {
    val server = newServer(8080, 8443)
    server.closeOnJvmShutdown()
    server.start().join()
    logger.info(
        "Server has been started. Serving DocService at http://127.0.0.1:{}/docs",
        server.activeLocalPort()
    )
}

private fun newServer(httpPort: Int, httpsPort: Int): Server {
    val sb = Server.builder()
    sb.http(httpPort)
        .https(httpsPort)
        .tlsSelfSigned()
    configureServices(sb)
    return sb.build()
}

fun configureServices(sb: ServerBuilder) {
    val exampleRequest = Hello.HelloRequest.newBuilder().setName("Armeria").build()
    val grpcService = GrpcService.builder()
        .addService(HelloServiceImpl()) // See https://github.com/grpc/grpc-java/blob/master/documentation/server-reflection-tutorial.md
        .addService(ProtoReflectionService.newInstance())
        .supportedSerializationFormats(GrpcSerializationFormats.values())
        .enableUnframedRequests(true) // You can set useBlockingTaskExecutor(true) in order to execute all gRPC
        // methods in the blockingTaskExecutor thread pool.
        // .useBlockingTaskExecutor(true)
        .build()
    sb.service(grpcService)
        .service(
            "prefix:/prefix",
            grpcService
        ) // You can access the documentation service at http://127.0.0.1:8080/docs.
        // See https://armeria.dev/docs/server-docservice for more information.
        .serviceUnder(
            "/docs",
            DocService.builder()
                .exampleRequests(
                    HelloServiceGrpc.SERVICE_NAME,
                    "Hello", exampleRequest
                )
                .exampleRequests(
                    HelloServiceGrpc.SERVICE_NAME,
                    "LazyHello", exampleRequest
                )
                .exampleRequests(
                    HelloServiceGrpc.SERVICE_NAME,
                    "BlockingHello", exampleRequest
                )
                .exclude(
                    DocServiceFilter.ofServiceName(
                        ServerReflectionGrpc.SERVICE_NAME
                    )
                )
                .build()
        )
}
