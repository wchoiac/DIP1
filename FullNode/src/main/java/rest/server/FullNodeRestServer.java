package rest.server;

import config.Configuration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class FullNodeRestServer {

    private static FullNodeRestServer runningServer = null;

    private FullNodeAPIResolver fullNodeAPIResolver;
    private Logger apiLogger=Logger.getLogger(FullNodeRestServer.class.getName());
    private HttpServer httpServer;
    private int port;
    private SSLContext context;

    public static FullNodeRestServer create(KeyStore keyStore, char[] password, int port, FullNodeAPIResolver fullNodeAPIResolver, String logFileName) throws Exception {
        return new FullNodeRestServer(keyStore, password,port,fullNodeAPIResolver, logFileName);
    }

    public static FullNodeRestServer getRunningServer()
    {
        return runningServer;
    }

    public FullNodeAPIResolver getAPIResolver()
    {
        return fullNodeAPIResolver;
    }

    public Logger getAPILogger()
    {
        return apiLogger;
    }

    public void start() throws Exception {
        if(runningServer==null)
        {
            runningServer = this;
            httpServer.start();
        }
        else if(runningServer==this) {
            return;
        }
        else{
            throw new Exception("A server is already running, please shutdown the running server to start this server");
        }
    }

    public void shutdown() throws Exception {
        if(runningServer==this) {
            httpServer.shutdown();
            runningServer = null;
        }
        else
        {
            throw new Exception("This server is not running");
        }
    }

    private FullNodeRestServer(KeyStore keyStore, char[] password, int port, FullNodeAPIResolver fullNodeAPIResolver, String logFileName) throws IOException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, URISyntaxException {

        this.context=createSSLSocketContext(keyStore,password);
        this.port=port;
        this.fullNodeAPIResolver=fullNodeAPIResolver;

        ResourceConfig resourceConfig =new ResourceConfig();
        resourceConfig.packages("rest.server.resource");


        httpServer = GrizzlyHttpServerFactory.createHttpServer(new URI("https://0.0.0.0:" + port+"/api"),
                resourceConfig,
                true,
                new SSLEngineConfigurator(context).setClientMode(false).setNeedClientAuth(false).setEnabledCipherSuites(Configuration.API_TLS_CIPHER_SUITE));

        FileHandler apiLogFileHandler = new FileHandler(logFileName, true);
        SimpleFormatter formatter = new SimpleFormatter();
        try {
            apiLogger.addHandler(apiLogFileHandler);
            apiLogger.setUseParentHandlers(false);
            apiLogFileHandler.setFormatter(formatter);

        } catch (SecurityException e) {
            e.printStackTrace();
        }

    }

    private SSLContext createSSLSocketContext(KeyStore keyStore, char[] password) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyManagementException {

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, password);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init((KeyStore)null);

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

        return sslContext;
    }

}
