package example.armeria.grpc

import com.google.common.base.Stopwatch
import com.linecorp.armeria.client.grpc.GrpcClients
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats
import com.linecorp.armeria.server.ServerBuilder
import com.linecorp.armeria.testing.junit5.server.ServerExtension
import example.armeria.grpc.HelloServiceImpl.Companion.toMessage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions
import org.awaitility.Awaitility
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

internal class HelloServiceTest {
    @Rule
    @JvmField
    val exceptionRule = CoroutineExceptionRule()

    private val scope = CoroutineScope(Dispatchers.Default + Job() + exceptionRule)

    @Test
    fun getReply() {
        val helloService = GrpcClients.newClient(uri(), HelloServiceGrpcKt.HelloServiceCoroutineStub::class.java)
        runBlocking {
            Assertions.assertThat(
                helloService.hello(
                    Hello.HelloRequest.newBuilder().setName("Armeria").build()
                ).message
            )
                .isEqualTo("Hello, Armeria!")
        }
    }

    @Test
    fun getReplyWithDelay() {
        val helloService = GrpcClients.newClient(uri(), HelloServiceGrpcKt.HelloServiceCoroutineStub::class.java)
        runBlocking {
            val result = helloService.lazyHello(Hello.HelloRequest.newBuilder().setName("Armeria").build())
            Assertions.assertThat(result.message).isEqualTo("Hello, Armeria!")
        }
    }

    @Test
    fun getReplyFromServerSideBlockingCall() {
        val helloService = GrpcClients.newClient(uri(), HelloServiceGrpcKt.HelloServiceCoroutineStub::class.java)
        val watch = Stopwatch.createStarted()
        runBlocking {
            Assertions.assertThat(
                helloService.blockingHello(Hello.HelloRequest.newBuilder().setName("Armeria").build())
                    .message
            ).isEqualTo("Hello, Armeria!")
            Assertions.assertThat(watch.elapsed(TimeUnit.SECONDS)).isGreaterThanOrEqualTo(3)
        }
    }

    @Test
    fun getLotsOfReplies() {
        val helloService = helloService()
        runBlocking {
            withTimeout(15.seconds) {
                helloService.lotsOfReplies(
                    Hello.HelloRequest.newBuilder().setName("Armeria").build()
                )
                    .withIndex()
                    .collect { (idx, value) ->
                        Assertions.assertThat(value.message)
                            .isEqualTo("Hello, Armeria! (sequence: ${idx + 1})")
                    }
            }
        }
    }

    @Test
    fun blockForLotsOfReplies() {
        val helloService = helloService()
        val count = runBlocking {
            helloService.lotsOfReplies(
                Hello.HelloRequest.newBuilder().setName("Armeria").build()
            )
                .withIndex()
                .onEach { (idx, value) ->
                    Assertions.assertThat(value.message)
                        .isEqualTo("Hello, Armeria! (sequence: ${idx + 1})")
                }
                .count()
        }
        Assertions.assertThat(count).isEqualTo(5)
    }

    @Test
    fun sendLotsOfGreetings() {
        val helloService = helloService()
        val names = arrayOf("Armeria", "Grpc", "Streaming")

        val greetingFlow = flow<Hello.HelloRequest> {
            for (name in names) {
                emit(Hello.HelloRequest.newBuilder().setName(name).build())
            }
        }
        val reply = runBlocking {
            helloService.lotsOfGreetings(greetingFlow)
        }
        Assertions.assertThat(reply.message)
            .isEqualTo(toMessage(names.joinToString()))
    }

    @Test
    fun bidirectionalHello() {
        val helloService = helloService()
        val names = arrayOf("Armeria", "Grpc", "Streaming")
        val helloChannel = Channel<Hello.HelloRequest>()

        val deferredReceivedCount = scope.async {
            helloService.bidiHello(helloChannel.consumeAsFlow())
                .withIndex()
                .onEach { (idx, value) ->
                    Assertions.assertThat(value.message)
                        .isEqualTo(toMessage(names[idx]))
                }
                .count()
        }


        val received = runBlocking {
            for (name in names) {
                helloChannel.send(Hello.HelloRequest.newBuilder().setName(name).build())
            }
            helloChannel.close()

            withTimeout(5.seconds) {
                deferredReceivedCount.await()
            }
        }
        Assertions.assertThat(received).isEqualTo(names.size)
    }

    companion object {
        @RegisterExtension
        @JvmField
        val server: ServerExtension = object : ServerExtension() {
            override fun configure(sb: ServerBuilder) {
                configureServices(sb)
            }
        }

        private fun helloService(): HelloServiceGrpcKt.HelloServiceCoroutineStub {
            return GrpcClients.newClient(uri(), HelloServiceGrpcKt.HelloServiceCoroutineStub::class.java)
        }

        private fun uri(): String {
            return server.httpUri(GrpcSerializationFormats.PROTO).toString()
        }
    }
}
