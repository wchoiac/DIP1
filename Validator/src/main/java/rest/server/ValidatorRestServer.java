package rest.server;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;


import config.Configuration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.server.ResourceConfig;


import javax.net.ssl.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class ValidatorRestServer {

    private static ValidatorRestServer runningServer = null;

    private ValidatorAPIResolver validatorAPIResolver;
    private Logger apiLogger=Logger.getLogger(ValidatorRestServer.class.getName());
    private HttpServer httpServer;
    private int port;
    private SSLContext context;

//test main
    public static void main(String[] args) throws Exception {

        File file = new File("apiKeyStore.keystore");
        if(!file.exists()) {
            System.out.println("Please use initializer before running node");
            return;
        }
        KeyStore keyStore= KeyStore.getInstance("JKS");
        char[] password;
        Scanner sc = new Scanner(System.in);
        Console console = System.console();
        while(true) {
            try {
                if (console != null) {
                    password = console.readPassword("Please enter keystore password: ");
                } else {
                    System.out.print("Please enter keystore password: ");
                    password = sc.next().toCharArray();

                }
                keyStore.load(new FileInputStream(file), password);
                break;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                System.out.println("Wrong Password");
                continue;
            }
        }
        //Validator validator = Validator.create(keyStore,"1234".toCharArray(),9999);  //## for debug
        ValidatorRestServer server =ValidatorRestServer.create(keyStore,password,Configuration.API_SERVER_PORT, null, Configuration.API_LOG_FILENAME); //## for debug
        server.start();

        System.out.println("press key to close the server");
        sc.next();
        server.shutdown();
        sc.close();

    }

    public static ValidatorRestServer create(KeyStore keyStore, char[] password, int port, ValidatorAPIResolver validatorAPIResolver, String logFileName) throws Exception {
        return new ValidatorRestServer(keyStore, password,port,validatorAPIResolver,logFileName);
    }

    public static ValidatorRestServer getRunningServer()
    {
        return runningServer;
    }

    public ValidatorAPIResolver getAPIResolver()
    {
        return validatorAPIResolver;
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

    private ValidatorRestServer(KeyStore keyStore, char[] password, int port, ValidatorAPIResolver validatorAPIResolver, String logFileName) throws IOException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, URISyntaxException {

        this.context=createSSLSocketContext(keyStore,password);
        this.port=port;
        this.validatorAPIResolver=validatorAPIResolver;

        ResourceConfig resourceConfig =new ResourceConfig();
        resourceConfig.packages("rest.server.resource");


        httpServer = GrizzlyHttpServerFactory.createHttpServer(new URI("https://0.0.0.0:" + port+"/api"),
                resourceConfig,
                true,
                new SSLEngineConfigurator(context).setClientMode(false)
                        .setNeedClientAuth(false)
                        .setEnabledCipherSuites(Configuration.API_TLS_CIPHER_SUITE));

        StaticHttpHandler staticHttpHandler = new StaticHttpHandler("www/static/");
        httpServer.getServerConfiguration().addHttpHandler(staticHttpHandler, "/");

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
