package example.armeria.grpc

import example.armeria.grpc.Hello.HelloReply
import example.armeria.grpc.HelloServiceGrpcKt.HelloServiceCoroutineImplBase
import io.grpc.Status
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

class HelloServiceImpl : HelloServiceCoroutineImplBase() {
    /**
     * Sends a [HelloReply] immediately when receiving a request.
     */
    override suspend fun hello(request: Hello.HelloRequest): HelloReply =
        if (request.name.isEmpty()) {
            throw Status.FAILED_PRECONDITION.withDescription("Name cannot be empty").asRuntimeException()
        } else {
            buildReply(toMessage(request.name))
        }

    /**
     * Sends a [HelloReply] 3 seconds after receiving a request.
     */
    override suspend fun lazyHello(request: Hello.HelloRequest): HelloReply {
        // You can use the event loop for scheduling a task.
        delay(3_000)
        return buildReply(toMessage(request.name))
    }

    /**
     * Sends a [HelloReply] using `blockingTaskExecutor`.
     *
     * @see [Blocking
     * service implementation](https://armeria.dev/docs/server-grpc.blocking-service-implementation)
     */
    override suspend fun blockingHello(request: Hello.HelloRequest): HelloReply {
        // Unlike upstream gRPC-Java, Armeria does not run service logic in a separate thread pool by default.
        // Therefore, this method will run in the event loop, which means that you can suffer the performance
        // degradation if you call a blocking API in this method. In this case, you have the following options:
        //
        // 1. Call a blocking API in the IO dispatcher provided by Coroutines.
        // 2. Set GrpcServiceBuilder.useBlockingTaskExecutor(true) when building your GrpcService.
        // 3. Call a blocking API in the separate dispatcher you manage.
        //
        // In this example, we chose the option 1:
        return withContext(Dispatchers.IO) {
            try {
                // Simulate a blocking API call.
                Thread.sleep(3_000)
            } catch (ignored: Exception) {
                // Do nothing.
            }
            buildReply(toMessage(request.name))
        }
    }

    /**
     * Sends 5 [HelloReply] responses when receiving a request.
     *
     * @see .lazyHello
     */
    override fun lotsOfReplies(request: Hello.HelloRequest): Flow<HelloReply> =
        // You can also write this code without Reactor like 'lazyHello' example.
        generateSequence(0) { it + 1 }
            .asFlow()
            .onEach { delay(1_000) }
            .take(5)
            // You can make your Flux/Mono publish the signals in the RequestContext-aware executor.
            .map { index -> buildReply("Hello, ${request.name}! (sequence: ${index + 1})") }

    /**
     * Sends a [HelloReply] when a request has been completed with multiple [HelloRequest]s.
     */
    override suspend fun lotsOfGreetings(requests: Flow<Hello.HelloRequest>): HelloReply {
        val names = requests
            .map { it.name }
            .toList()

        return buildReply(toMessage(names.joinToString()))
    }

    /**
     * Sends a [HelloReply] when each [HelloRequest] is received. The response will be completed
     * when the request is completed.
     */
    override fun bidiHello(requests: Flow<Hello.HelloRequest>): Flow<HelloReply> =
        requests
            .map { buildReply(toMessage(it.name)) }

    companion object {
        fun toMessage(name: String): String {
            return "Hello, $name!"
        }

        private fun buildReply(message: Any): HelloReply {
            return HelloReply.newBuilder().setMessage(message.toString()).build()
        }
    }
}
