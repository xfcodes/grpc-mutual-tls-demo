package demo.tls.server;

import io.grpc.Server;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

/**
 * Description
 *
 * @author maoyunfei
 * @date 2018-11-22
 */
public class HelloWorldServer {
    private static final Logger logger = Logger.getLogger(HelloWorldServer.class.getName());

    private Server server;

    private void start() throws IOException {
        server = NettyServerBuilder.forAddress(new InetSocketAddress("localhost", 50051))
                .addService(new GreeterImpl())
                .sslContext(getSslContextBuilder().build())
                .build()
                .start();
        logger.info("Server started, listening on 50051");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                HelloWorldServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private SslContextBuilder getSslContextBuilder() {
        String filepath = System.getProperty("user.dir") + "/";
        String certChainFilePath = filepath + "sslcert/server.crt";
        String privateKeyFilePath = filepath + "sslcert/server.pem";
        String trustCertCollectionFilePath = filepath + "sslcert/ca.crt";

        /*ClassLoader classLoader = HelloWorldServer.class.getClassLoader();
        File keyCertChainFile=new File(classLoader.getResource(certChainFilePath).getFile());
        File keyFile= new File(classLoader.getResource(privateKeyFilePath).getFile());
        File trustCertCollectionFile=new File(classLoader.getResource(trustCertCollectionFilePath).getFile());*/
        File keyCertChainFile=new File(certChainFilePath);
        File keyFile= new File(privateKeyFilePath);
        File trustCertCollectionFile=new File(trustCertCollectionFilePath);

        SslContextBuilder sslClientContextBuilder = SslContextBuilder.forServer(keyCertChainFile,keyFile);
        sslClientContextBuilder.trustManager(trustCertCollectionFile);
        sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE);

        return GrpcSslContexts.configure(sslClientContextBuilder,
                SslProvider.OPENSSL);
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        HelloWorldServer server = new HelloWorldServer();
        server.start();
        server.blockUntilShutdown();
    }
}
