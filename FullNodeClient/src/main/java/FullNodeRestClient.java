import blockchain.BlockChainSecurityHelper;
import exception.*;
import pojo.*;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.client.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;




//hash algorithm is always SHA256
//signature algorithm is always SHA256withECDSA

public class FullNodeRestClient {

    static{
        HttpsURLConnection.setDefaultHostnameVerifier(
                (hostname, sslSession) -> {
                    return true;
                }
        );
    };

    private Client client;
    private WebTarget base;
    private String token=null;

    public FullNodeRestClient(InetAddress ipAddress, X509Certificate apiServerCert) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException, URISyntaxException {

        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(null);
        trustStore.setCertificateEntry("DIP1-SIGNING", apiServerCert);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X509");
        trustManagerFactory.init(trustStore);

        client = ClientBuilder.newClient();
        client.getSslContext().init(null, trustManagerFactory.getTrustManagers(),null);
        base =client.target(new URI("https://"+ipAddress.getHostAddress())).path("api");
    }

    //for testing
    public String getToken()
    {
        return token;
    }

    /*
     *  If successful, secure token
     */

    public void login(String name, char[] password) throws InvalidUserInfo {
        WebTarget webTarget = base.path("user/login");

        Invocation.Builder invocationBuilder =  webTarget.request(MediaType.TEXT_PLAIN);

        UserInfoPojo userInfoPojo=new UserInfoPojo();
        userInfoPojo.setUsername(name);
        userInfoPojo.setPassword(password);

        Response response = invocationBuilder.post(Entity.json(userInfoPojo));

        if(response.getStatus()== Response.Status.UNAUTHORIZED.getStatusCode())
            throw new InvalidUserInfo();

        String result =response.readEntity(String.class);
        response.close();
        token=result;

    }

    /*
     *return status code(1 byte) + transaction id(32 byte) (if successful):
     *
     * status code:
     * 0: successful
     * 1: being processed
     * 2: already exists or invalid
     *
     * transaction id:
     * hash of the transaction
     *
     *
     *
     * To note, signature = signature( GeneralHelper.longToBytes(getTimestamp()) | encryptedRecord | medicalOrgIdentifier ), "|" means concatenate
     *  And, simply calling getEncoded returns DER encoded data.
     */

    public byte[] addTransaction(long timeStamp,byte[] encryptedRecord,boolean isSinatureDEREncoded,byte[] patientSignature,byte[] patientIdentifier) throws UnAuthorized, NotFound, BadRequest, ServerError {
        WebTarget webTarget = base.path("record/add-transaction");

        Invocation.Builder invocationBuilder =  webTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION,token);

        TransactionPojo transactionPojo=new TransactionPojo(timeStamp,encryptedRecord,isSinatureDEREncoded,patientSignature,patientIdentifier);
        Response response = invocationBuilder.put(Entity.json(transactionPojo));

        checkException(response);

        byte[] result =response.readEntity(ByteArrayWrapper.class).getContent();
        response.close();

        return result;
    }


    /*
     * return list of short information of the patient's records
     * (each short info includes timestamp, medical org name and location of the record in the blockchain )
     */
    public  RecordShortInfoPojo[] getRecordShortInfoList(ECPublicKey patientPublicKey) throws UnAuthorized, NotFound, BadRequest, ServerError {
        WebTarget webTarget = base.path("record/get-record-short-info-list");

        Invocation.Builder invocationBuilder =  webTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION,token);

        ByteArrayWrapper byteArrayWrapper = new ByteArrayWrapper(BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(patientPublicKey));
        Response response = invocationBuilder.post(Entity.json(byteArrayWrapper));
        checkException(response);
        RecordShortInfoPojo[] result =response.readEntity(RecordShortInfoPojo[].class);


        response.close();

        return result;
    }

    /*
     * return list of the requested records' contents
     */
    public  RecordContentPojo[] getRecordShortInfoList(ArrayList<LocationPojo> locationPojos) throws UnAuthorized, NotFound, BadRequest, ServerError {
        WebTarget webTarget = base.path("record/get-record-short-info-list");

        Invocation.Builder invocationBuilder =  webTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION,token);

        Response response = invocationBuilder.post(Entity.json(locationPojos));

        checkException(response);

        RecordContentPojo[] result =response.readEntity(RecordContentPojo[].class);
        response.close();

        return result;
    }

    /*
     * return list of short information of the patient
     * (each short info includes timestamp, and location of the patient information in the blockchain )
     */
    public  PatientShortInfoPojo[] getPatientShortInfoList(ECPublicKey patientPublicKey) throws UnAuthorized, NotFound, BadRequest, ServerError {
        WebTarget webTarget = base.path("patient/get-patient-short-info-list");

        Invocation.Builder invocationBuilder =  webTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION,token);

        ByteArrayWrapper byteArrayWrapper = new ByteArrayWrapper(BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(patientPublicKey));
        Response response = invocationBuilder.post(Entity.json(byteArrayWrapper));
        checkException(response);
        PatientShortInfoPojo[] result =response.readEntity(PatientShortInfoPojo[].class);


        response.close();

        return result;
    }

    /*
     * return list of the requested patient information contents
     */
    public  PatientInfoContentPojo[] getPatientInfoContentsList(ArrayList<LocationPojo> locationPojos) throws UnAuthorized, NotFound, BadRequest, ServerError {
        WebTarget webTarget = base.path("patient/get-patient-info-contents-list");

        Invocation.Builder invocationBuilder =  webTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION,token);

        Response response = invocationBuilder.post(Entity.json(locationPojos));
        checkException(response);
        PatientInfoContentPojo[] result =response.readEntity(PatientInfoContentPojo[].class);


        response.close();

        return result;
    }


    /*
     * return this medical organization's identifier
     */
    public  byte[] getMedicalOrgIdentifier() throws UnAuthorized, NotFound, BadRequest, ServerError {
        WebTarget webTarget = base.path("medical-org/get-identifier");

        Invocation.Builder invocationBuilder =  webTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION,token);

        Response response = invocationBuilder.get();

        checkException(response);

        byte[] result =response.readEntity(ByteArrayWrapper.class).getContent();

        response.close();

        return result;
    }


    private void checkException(Response response) throws UnAuthorized, BadRequest, NotFound, ServerError {
        if(response.getStatus()== Response.Status.UNAUTHORIZED.getStatusCode())
            throw new UnAuthorized("Unauthorized request.");
        else if(response.getStatus()== Response.Status.BAD_REQUEST.getStatusCode())
            throw new BadRequest();
        else if(response.getStatus()== Response.Status.NOT_FOUND.getStatusCode())
            throw new NotFound();
        else if(response.getStatus()== Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
            throw new ServerError();
    }

}
