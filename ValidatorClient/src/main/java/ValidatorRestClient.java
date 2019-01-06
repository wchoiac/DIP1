
import exception.*;
import general.security.SecurityHelper;
import general.utility.GeneralHelper;
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
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.Date;

//for testing purpose
public class ValidatorRestClient {

    static {
        HttpsURLConnection.setDefaultHostnameVerifier(
                (hostname, sslSession) -> {
                    return true;
                }
        );
    }

    ;

    private Client client;
    private WebTarget base;
    private String token = null;

    public ValidatorRestClient(InetAddress ipAddress, X509Certificate apiServerCert) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException, URISyntaxException {

        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(null);
        trustStore.setCertificateEntry("DIP1-SIGNING", apiServerCert);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X509");
        trustManagerFactory.init(trustStore);

        client = ClientBuilder.newClient();
        client.getSslContext().init(null, trustManagerFactory.getTrustManagers(), null);
        base = client.target(new URI("https://" + ipAddress.getHostAddress())).path("api");
    }


    /*
     *  If successful, secure token is set
     *  Else, throws exception
     */

    public void login(String name, char[] password) throws InvalidUserInfo {
        WebTarget webTarget = base.path("user/login");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.TEXT_PLAIN);

        UserInfoPojo userInfoPojo = new UserInfoPojo();
        userInfoPojo.setUsername(name);
        userInfoPojo.setPassword(password);

        Response response = invocationBuilder.post(Entity.json(userInfoPojo));

        if (response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode())
            throw new InvalidUserInfo();

        String result = response.readEntity(String.class);
        response.close();
        token = result;
    }

    /*
     * If successful, returns list of authority information
     * Else, throws exception
     *
     * @SecuredRootLevel
     */
    public AuthorityInfoPojo[] getOverallAuthorityShortInfoList() throws UnAuthorized, BadRequest, NotFound, ServerError {
        WebTarget webTarget = base.path("authority/get-overall-list");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token);

        Response response = invocationBuilder.get();

        checkException(response);


        AuthorityInfoPojo[] result = response.readEntity(AuthorityInfoPojo[].class);
        response.close();

        return result;

    }


    /*
     * If successful, returns list of votes that to be processed (votes that are in the vote pool of validator)
     * Else, throws exception
     *
     * @SecuredRootLevel
     */
    public VotePojo[] getToBeProcessedVotingList() throws UnAuthorized, BadRequest, NotFound, ServerError {
        WebTarget webTarget = base.path("authority/vote/get-processing-list");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token);

        Response response = invocationBuilder.get();

        checkException(response);

        VotePojo[] result = response.readEntity(VotePojo[].class);
        response.close();

        return result;

    }

    /*
     * If successful, returns list of on-going votings
     * Else, throws exception
     * @SecuredRootLevel
     */
    public VotingPojo[] getOnGoingVotingList() throws UnAuthorized, BadRequest, NotFound, ServerError {
        WebTarget webTarget = base.path("authority/vote/get-current-list");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token);

        Response response = invocationBuilder.get();

        checkException(response);

        VotingPojo[] result = response.readEntity(VotingPojo[].class);
        response.close();

        return result;

    }


    /*
     * For voting for adding a new authority or removing an existing authority
     *
     * If unsuccessful, throws Unsuccessful exception with status code or other exceptions
     *
     * return status code:
     * 1: being processed
     * 2: cannot be processed (e.g. already voted)
     *
     * @SecuredRootLevel
     */
    public void castVote(String name, ECPublicKey ecPublicKey, boolean add, boolean agree) throws UnAuthorized, BadRequest, NotFound, ServerError, Unsuccessful {
        WebTarget webTarget = base.path("authority/vote/cast");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.TEXT_PLAIN);

        AuthorityInfoPojo authorityInfoPojo = new AuthorityInfoPojo(name, ecPublicKey.getEncoded(), true);
        VotePojo votePojo = new VotePojo(authorityInfoPojo, add, agree);

        Response response = invocationBuilder.post(Entity.json(votePojo));

        checkException(response);

        byte result = Byte.valueOf(response.readEntity(String.class));
        response.close();

        if (result != 0)
            throw new Unsuccessful(result);


    }

    /*
     * If successful, return list of short information of the patient
     * Else, throws exception
     * @SecuredUserLevel
     */

    public PatientShortInfoPojo[] getPatientShortInfoList(byte[] patientIdentifier) throws UnAuthorized, BadRequest, NotFound, ServerError {


        WebTarget webTarget = base.path("patient/get-patient-short-info-list");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token);

        Response response = invocationBuilder.post(Entity.json(new ByteArrayWrapper(patientIdentifier)));

        checkException(response);

        PatientShortInfoPojo[] result = response.readEntity(PatientShortInfoPojo[].class);
        response.close();

        return result;
    }

    /*
     * If successful, returns list of the requested patient information contents
     * Else, throws exception
     * @SecuredUserLevel
     */
    public PatientInfoContentPojo[] getPatientContentList(LocationPojo[] locationPojos) throws UnAuthorized, BadRequest, NotFound, ServerError {


        WebTarget webTarget = base.path("patient/get-patient-info-content-list");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token);

        Response response = invocationBuilder.post(Entity.json(locationPojos));

        checkException(response);

        PatientInfoContentPojo[] result = response.readEntity(PatientInfoContentPojo[].class);
        response.close();

        return result;
    }

    /*
     * For registering patient information
     *
     * If unsuccessful, throws Unsuccessful exception with status code or other exceptions
     *
     * 1: being processed
     * 2: already exists
     *
     * Information for patient mobile application and registration software -----------------------
     * signature = signature( timeStamp  | encryptedInfo ) , "|" means concatenate.
     * (You may use GeneralHelper.longToBytes(timestamp) for converting long to byte arrays
     * and GeneralHelper.merge mergeByteArrays(...) for merging byte arrays)
     *
     * For the steps,
     * 1. patient gives timestamp and AES key from pseudo random function with input of the timestamp
     * 2. authority gives SHA256 hash of (timeStamp  | encryptedInfo) to patient
     * 3. patient sign the hash with "NONEwithECDSA" ("NONE" because signing with hash)
     *
     * Also, simply using Signature.sign(..) gives out DER encoded signature.
     * So, if the patient app uses it, set isSignatureDER as true.
     * --------------------------------------------------------------------------------------------
     *
     *
     * @SecuredUserLevel
     */

    public void registerPatientInfo(long timstamp, ECPublicKey publicKey, byte[] encryptedInfo, byte[] signature, boolean isSignatureDEREncoded) throws Unsuccessful, UnAuthorized, BadRequest, NotFound, ServerError {


        WebTarget webTarget = base.path("patient/register");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.TEXT_PLAIN).header(HttpHeaders.AUTHORIZATION, token);

        PatientInfoPojo patientInfoPojo = new PatientInfoPojo(timstamp, publicKey.getEncoded(), encryptedInfo, signature, true
                , isSignatureDEREncoded);


        Response response = invocationBuilder.put(Entity.json(patientInfoPojo));

        checkException(response);

        byte result = Byte.valueOf(response.readEntity(String.class));
        response.close();

        if (result != 0)
            throw new Unsuccessful(result);

    }

    /*
     * If unsuccessful, throws Unsuccessful exception with status code or other exceptions
     *
     * 1: being processed
     * 2: patient doesn't exist
     * 3: already updated
     *
     * Generally, it's the same as register (above), except that it doesn't get processed if the patient doesn't exist.
     */

    public void updatePatientInfo(long timstamp, ECPublicKey publicKey, byte[] encryptedInfo, byte[] signature, boolean isSignatureDEREncoded) throws Unsuccessful, UnAuthorized, BadRequest, NotFound, ServerError {


        WebTarget webTarget = base.path("patient/update");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.TEXT_PLAIN).header(HttpHeaders.AUTHORIZATION, token);

        PatientInfoPojo patientInfoPojo = new PatientInfoPojo(timstamp, publicKey.getEncoded(), encryptedInfo, signature, true
                , isSignatureDEREncoded);


        Response response = invocationBuilder.put(Entity.json(patientInfoPojo));

        checkException(response);

        byte result = Byte.valueOf(response.readEntity(String.class));
        response.close();

        if (result != 0)
            throw new Unsuccessful(result);

    }
    /*
     * For revoking authorization given to a medical organization
     *
     * If unsuccessful, throws Unsuccessful exception with status code or other exceptions
     *
     * 1: being processed
     * 2: authorized by another authority
     *
     * @SecuredUserLevel
     */

    public void revokeMedicalOrg(byte[] medicalOrgIdentifier) throws UnAuthorized, BadRequest, NotFound, ServerError, Unsuccessful {


        WebTarget webTarget = base.path("medical-org/revoke");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token);

        Response response = invocationBuilder.put(Entity.json(new ByteArrayWrapper(medicalOrgIdentifier)));

        checkException(response);

        byte result = Byte.valueOf(response.readEntity(String.class));
        response.close();

        if (result != 0)
            throw new Unsuccessful(result);

    }

    /*
     * For authorizing medical organizations
     *
     * If successful, returns certificate
     * Else, throws Unsuccessful exception with status code or other exceptions
     *
     * status code:
     * 1: being processed
     * 2: already exists
     *
     * @SecuredUserLevel
     */
    public X509Certificate authorizeMedicalOrg(String name, ECPublicKey publicKey, Date noAfter) throws UnAuthorized, BadRequest, NotFound, ServerError, IOException, Unsuccessful {


        WebTarget webTarget = base.path("medical-org/authorize");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token);


        MedicalOrgInfoPojo medicalOrgInfoPojo = new MedicalOrgInfoPojo(name, publicKey.getEncoded(), true);
        AuthorizationRequestPojo authorizationRequestPojo = new AuthorizationRequestPojo();
        authorizationRequestPojo.setMedicalOrgInfo(medicalOrgInfoPojo);
        authorizationRequestPojo.setNoAfter(noAfter);

        Response response = invocationBuilder.put(Entity.json(authorizationRequestPojo));

        checkException(response);

        byte[] result = response.readEntity(ByteArrayWrapper.class).getContent();
        response.close();

        if (result[0] != 0)
            throw new Unsuccessful(result[0]);


        byte[] certBytes = Arrays.copyOfRange(result, 1, result.length);

        try {
            return SecurityHelper.getX509FromBytes(certBytes);
        } catch (CertificateException e) {
            throw new ServerError(); // not expected if status (not HTTP status code) code is 0
        }
    }

    /*
     * If successful, returns list of short information of medical organizations authorized by this authority
     * Else, throws exception
     *
     * @SecuredUserLevel
     */

    public MedicalOrgShortInfoPojo[] getAllMedicalOrgShortInfoAuthorizedByMe() throws UnAuthorized, BadRequest, NotFound, ServerError {
        WebTarget webTarget = base.path("medical-org/get-authorization-list");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token);

        Response response = invocationBuilder.get();

        checkException(response);


        MedicalOrgShortInfoPojo[] result = response.readEntity(MedicalOrgShortInfoPojo[].class);
        response.close();

        return result;

    }


    /*
     * If successful, returns the corresponding DER encoded X509 certificate
     * Else, throws exception
     *
     * @SecuredUserLevel
     */

    public X509Certificate getIssuedMedicalOrgCertificate(byte[] medicalOrgIdentifier) throws UnAuthorized, BadRequest, NotFound, ServerError, IOException, Unsuccessful {


        WebTarget webTarget = base.path("medical-org/get-certificate");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token);

        Response response = invocationBuilder.put(Entity.json(new ByteArrayWrapper(medicalOrgIdentifier)));

        checkException(response);

        byte[] result = response.readEntity(ByteArrayWrapper.class).getContent();
        response.close();

        try {
            return SecurityHelper.getX509FromBytes(result);
        } catch (CertificateException e) {
            throw new ServerError(); // not expected if status (not HTTP status code) code is 0
        }
    }

    /*
     * If successful, returns certificate
     * Else, throws Unsuccessful exception with status code or other exceptions
     *
     * status code:
     * 1: not authorized
     * 2: not authorized by this authority
     *
     * @SecuredUserLevel
     */

    public X509Certificate renewMedicalOrgCertificate(byte[] medicalOrgIdentifier, Date noAfter) throws UnAuthorized, BadRequest, NotFound, ServerError, IOException, Unsuccessful {


        WebTarget webTarget = base.path("medical-org/renew-certificate");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token);


        CertificateRenewRequestPojo certificateRenewRequestPojo = new CertificateRenewRequestPojo(medicalOrgIdentifier, noAfter);

        Response response = invocationBuilder.put(Entity.json(certificateRenewRequestPojo));

        checkException(response);

        byte[] result = response.readEntity(ByteArrayWrapper.class).getContent();
        response.close();

        if (result[0] != 0)
            throw new Unsuccessful(result[0]);


        byte[] certBytes = Arrays.copyOfRange(result, 1, result.length);

        try {
            return SecurityHelper.getX509FromBytes(certBytes);
        } catch (CertificateException e) {
            throw new ServerError(); // not expected if status (not HTTP status code) code is 0
        }
    }

    private void checkException(Response response) throws UnAuthorized, BadRequest, NotFound, ServerError {
        if (response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode())
            throw new UnAuthorized("Unauthorized request.");
        else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode())
            throw new BadRequest();
        else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode())
            throw new NotFound();
        else if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
            throw new ServerError();
    }
}
