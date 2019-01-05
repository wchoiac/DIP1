
import config.Configuration;
import exception.InvalidUser;
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
     *  If successful, secure token
     */

    public void login(String name, char[] password) throws InvalidUser {
        WebTarget webTarget = base.path("user/login");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.TEXT_PLAIN);

        UserInfoPojo userInfoPojo = new UserInfoPojo();
        userInfoPojo.setUsername(name);
        userInfoPojo.setPassword(password);

        Response response = invocationBuilder.post(Entity.json(userInfoPojo));

        String result = response.readEntity(String.class);
        response.close();

        if (result == null)
            throw new InvalidUser("Invalid user");
        token = result;

        System.out.println(token);

    }

    /*
     * return status code(1 byte) + transaction id(32 byte) (if successful)
     * status code:
     * 0: successful
     * 1: being processed
     * 2: already exists
     */
    public X509Certificate authorize(String name, ECPublicKey publicKey, Date noAfter) throws Exception {


        WebTarget webTarget = base.path("medical-org/authorize");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token);


        MedicalOrgInfoPojo medicalOrgInfoPojo = new MedicalOrgInfoPojo(name, publicKey.getEncoded(), true);
        AuthorizationRequestPojo authorizationRequestPojo = new AuthorizationRequestPojo();
        authorizationRequestPojo.setMedicalOrgInfo(medicalOrgInfoPojo);
        authorizationRequestPojo.setNoAfter(noAfter);

        Response response = invocationBuilder.put(Entity.json(authorizationRequestPojo));
        byte[] result = response.readEntity(ByteArrayWrapper.class).getContent();
        response.close();

        if (result == null || result[0] != 0) {
            System.out.println(result[0]);
            throw new Exception("Invalid Input");
        }

        byte[] certBytes = Arrays.copyOfRange(result, 1, result.length);

        return SecurityHelper.getX509FromBytes(certBytes);
    }

    public MedicalOrgShortInfoPojo[] loadAllMedicalOrgShortInfoAuthorizedByMe() throws InvalidUser {
        WebTarget webTarget = base.path("medical-org/get-authorization-list");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token);

        Response response = invocationBuilder.get();
        MedicalOrgShortInfoPojo[] result = response.readEntity(MedicalOrgShortInfoPojo[].class);
        response.close();

        return result;

    }
    /*
     *return status code:
     * 0: successful
     * 1: being processed
     * 2: already exists
     */

    public byte register(long timstamp, ECPublicKey publicKey, byte[] encryptedInfo, byte[] signature) throws NoSuchAlgorithmException {


        WebTarget webTarget = base.path("patient/register");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.TEXT_PLAIN).header(HttpHeaders.AUTHORIZATION, token);

        PatientInfoPojo patientInfoPojo = new PatientInfoPojo(timstamp, publicKey.getEncoded(), encryptedInfo, signature, true
                , false);


        Response response = invocationBuilder.put(Entity.json(patientInfoPojo));
        byte result = Byte.valueOf(response.readEntity(String.class));
        response.close();

        return result;
    }

    /*
     * return list of short information of the patient
     */

    public PatientShortInfoPojo[] getPatientShortInfoList(byte[] patientIdentifier) throws Exception {


        WebTarget webTarget = base.path("patient/get-patient-short-info-list");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token);

        Response response = invocationBuilder.post(Entity.json(new ByteArrayWrapper(patientIdentifier)));


        PatientShortInfoPojo[] result = response.readEntity(PatientShortInfoPojo[].class);
        response.close();

        return result;
    }

    /*
     * return list of the requested patient information contents
     */
    public PatientInfoContentPojo[] getPatientContentList(LocationPojo[] locationPojos) throws Exception {


        WebTarget webTarget = base.path("patient/get-patient-info-content-list");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token);

        Response response = invocationBuilder.post(Entity.json(locationPojos));


        PatientInfoContentPojo[] result = response.readEntity(PatientInfoContentPojo[].class);
        response.close();

        return result;
    }
//
//    @GET
//    @Produces(MediaType.TEXT_PLAIN)
//    @Path("test")
//    public Response test(@Context Request request, @QueryParam("test") String test) {
//
//        System.out.println(test + " " + request.getRequestURI());
//
//        return Response.serverError().build();
//    }
//
//    /*
//     *  If successful, Secure token
//     *  Else, return "login failed"
//     */
//    @POST
//    @Produces(MediaType.TEXT_PLAIN)
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Path("user/login")
//    public Response login(@Context Request request, UserInfoPojo userInfoPojo) {
//
//        String token;
//        try {
//            token = ValidatorRestServer.getRunningServer().getAPIResolver().apiLogin(userInfoPojo);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return Response.serverError().build();
//        } catch (BadRequest e) {
//            return Response.status(Response.Status.BAD_REQUEST).build();
//        } catch (InvalidUserInfo e) {
//            ValidatorRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ": login fail " + userInfoPojo.getUsername());
//            return Response.status(Response.Status.UNAUTHORIZED).build();
//        }
//
//        ValidatorRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ": login success(" + userInfoPojo.getUsername() + ")");
//        return Response.ok(token,MediaType.TEXT_PLAIN).build();
//
//    }
//
//    /*
//     * return list of authority information
//     */
//    @GET
//    @SecuredRootLevel
//    @Produces(MediaType.APPLICATION_JSON) // Read with AuthorityInfoPojo[]
//    @Path("authority/get-overall-list")
//    public Response getOverallList() {
//
//        try {
//            return Response.ok(ValidatorRestServer.getRunningServer().getAPIResolver().getOverallList(), MediaType.APPLICATION_JSON).build();
//        } catch (IOException | BlockChainObjectParsingException e) {
//            e.printStackTrace();
//            return Response.serverError().build();
//        }
//    }
//
//
//    /*
//     * return list of votes that to be processed
//     */
//    @GET
//    @SecuredRootLevel
//    @Produces(MediaType.APPLICATION_JSON) // Read with VotePojo[]
//    @Path("authority/vote/get-processing-list")
//    public Response getMyVotes() {
//
//        return Response.ok(ValidatorRestServer.getRunningServer().getAPIResolver().getMyVotes()
//                , MediaType.APPLICATION_JSON).build();
//
//    }
//
//
//    /*
//     * return list of on-going votings
//     */
//    @GET
//    @SecuredRootLevel
//    @Produces(MediaType.APPLICATION_JSON) //Read with VotingPojo[]
//    @Path("authority/vote/get-current-list")
//    public Response getCurrentVotingList() {
//
//        return Response.ok(ValidatorRestServer.getRunningServer().getAPIResolver().getCurrentVotingList()
//                , MediaType.APPLICATION_JSON).build();
//
//    }
//
//
//    /*
//     *return status code:
//     * 0: successful
//     * 1: being processed
//     * 2: cannot be processed (e.g. already voted)
//     */
//    @POST
//    @SecuredRootLevel
//    @Produces(MediaType.TEXT_PLAIN)
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Path("authority/vote/cast")
//    public Response castVote(VotePojo votePojo) {
//        try {
//            return Response.ok("" + ValidatorRestServer.getRunningServer().getAPIResolver().castVote(votePojo), MediaType.TEXT_PLAIN).build();
//        } catch (IOException | BlockChainObjectParsingException e) {
//            e.printStackTrace();
//            return Response.serverError().build();
//        } catch (InvalidKeySpecException | BadRequest e) {
//            e.printStackTrace();
//            return Response.status(Response.Status.BAD_REQUEST).build();
//        }
//    }
//
//    /*
//     * return list of short information of the patient
//     */
//    @POST
//    @SecuredUserLevel
//    @Produces(MediaType.APPLICATION_JSON) // Read with PatientShortInfoPojo[]
//    @Consumes(MediaType.APPLICATION_JSON) // {"content": <Patient's identifier - byte array>} - use ByteArrayWrapper
//    @Path("patient/get-patient-short-info-list")
//    public Response getPatientShortInfoList(ByteArrayWrapper wrapper) {
//
//        try {
//            return Response.ok(ValidatorRestServer.getRunningServer().getAPIResolver().getPatientShortInfoList(wrapper.getContent())
//                    , MediaType.APPLICATION_JSON).build();
//        } catch (IOException | BlockChainObjectParsingException e) {
//            e.printStackTrace();
//            return Response.serverError().build();
//        } catch (BadRequest e) {
//            e.printStackTrace();
//            return Response.status(Response.Status.BAD_REQUEST).build();
//        } catch (NotFound e) {
//            e.printStackTrace();
//            return Response.status(Response.Status.NOT_FOUND).build();
//        }
//
//
//    }
//
//    /*
//     * return list of the requested patient information contents
//     */
//    @POST
//    @SecuredUserLevel
//    @Produces(MediaType.APPLICATION_JSON) // read with PatientInfoContentPojo[]
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Path("patient/get-patient-info-content-list")
//    public Response getPatientInfoContentsList(ArrayList<LocationPojo> locationPojos) {
//
//        try {
//            return Response.ok(ValidatorRestServer.getRunningServer().getAPIResolver().getPatientInfoContentsList(locationPojos)
//                    , MediaType.APPLICATION_JSON).build();
//        } catch (IOException | BlockChainObjectParsingException e) {
//            e.printStackTrace();
//            return Response.serverError().build();
//        } catch (NotFound e) {
//            e.printStackTrace();// may be just bad request
//            return Response.status(Response.Status.NOT_FOUND).build();
//        } catch (BadRequest e) {
//            e.printStackTrace();
//            return Response.status(Response.Status.BAD_REQUEST).build();
//        }
//
//    }
//
//    /*
//     *return status code:
//     * 0: successful
//     * 1: being processed
//     * 2: already exists
//     *
//     * To note, signature = signature( GeneralHelper.longToBytes(timeStamp) | encryptedInfo ) , "|" means concatenate
//     */
//    @PUT
//    @SecuredUserLevel
//    @Produces(MediaType.TEXT_PLAIN)
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Path("patient/register")
//    public Response register(PatientInfoPojo patientInfoPojo) {
//
//        try {
//            return Response.ok("" + ValidatorRestServer.getRunningServer().getAPIResolver().register(patientInfoPojo)
//                    , MediaType.TEXT_PLAIN).build();
//        } catch (ServerError|IOException | BlockChainObjectParsingException e) {
//            e.printStackTrace();
//            return Response.serverError().build();
//        } catch (InvalidKeySpecException | BadRequest e) {
//            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
//        }
//
//    }
//
//    /*
//     *return status code:
//     * 0: successful
//     * 1: being processed
//     * 2: patient doesn't exist
//     * 3: already updated
//     *
//     * To note, signature = signature( GeneralHelper.longToBytes(timeStamp) | encryptedInfo ), "|" means concatenate
//     */
//    @PUT
//    @SecuredUserLevel
//    @Produces(MediaType.TEXT_PLAIN)
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Path("patient/update")
//    public Response update(PatientInfoPojo patientInfoPojo) {
//
//        try {
//            return Response.ok("" + ValidatorRestServer.getRunningServer().getAPIResolver().update(patientInfoPojo)
//                    , MediaType.TEXT_PLAIN).build();
//        } catch (ServerError|IOException | BlockChainObjectParsingException e) {
//            e.printStackTrace();
//            return Response.serverError().build();
//        } catch (InvalidKeyException|SignatureException|InvalidKeySpecException | BadRequest e) {
//            e.printStackTrace();
//            return Response.status(Response.Status.BAD_REQUEST).build();
//        }
//    }
//
//    /*
//     *return status code:
//     * 0: successful
//     * 1: being processed
//     * 2: authorized by another authority
//     */
//    @PUT
//    @SecuredUserLevel
//    @Produces(MediaType.TEXT_PLAIN)
//    @Consumes(MediaType.APPLICATION_JSON) //{"content": <Medical organization's identifier - byte array>}
//    @Path("medical-org/revoke")
//    public Response revoke(ByteArrayWrapper byteArrayWrapper) {
//
//        try {
//            return Response.ok("" + ValidatorRestServer.getRunningServer().getAPIResolver().revoke(byteArrayWrapper.getContent())
//                    , MediaType.TEXT_PLAIN).build();
//        } catch (IOException | BlockChainObjectParsingException|FileCorruptionException e) {
//            e.printStackTrace();
//            return Response.serverError().build();
//        } catch (BadRequest e) {
//            return Response.status(Response.Status.BAD_REQUEST).build();
//        }  catch (NotFound notFound) {
//            return Response.status(Response.Status.NOT_FOUND).build();
//        }
//
//    }
//
//    /*
//     * return status code(1 byte) + encoded certificate(if success):
//     * status code:
//     * 0: successful
//     * 1: being processed
//     * 2: already exists
//     */
//    @SecuredUserLevel
//    @PUT
//    @Produces(MediaType.APPLICATION_JSON)// {"content": <Status code(1 byte) + DER encoded certificate(if success) - byte array>} - use ByteArrayWrapper
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Path("medical-org/authorize")
//    public Response authorize(AuthorizationRequestPojo authorizationRequestPojo) {
//
//
//        try {
//            return Response.ok(new ByteArrayWrapper(ValidatorRestServer.getRunningServer().getAPIResolver().authorize(authorizationRequestPojo))
//                    , MediaType.APPLICATION_JSON).build();
//        } catch (FileCorruptionException|OperatorCreationException | BlockChainObjectParsingException | NoSuchAlgorithmException | IOException | CertificateException e) {
//            e.printStackTrace();
//            return Response.serverError().build();
//        } catch (InvalidKeySpecException | BadRequest e) {
//            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
//        }
//    }
//
//    /*
//     * return list of information of medical organizations authorized by this authority
//     */
//    @GET
//    @SecuredUserLevel
//    @Produces(MediaType.APPLICATION_JSON) // Read with MedicalOrgShortInfoPojo[]
//    @Path("medical-org/get-authorization-list")
//    public Response loadAllMedicalOrgShortInfoAuthorizedByMe() {
//
//        try {
//            return Response.ok(ValidatorRestServer.getRunningServer().getAPIResolver().loadAllMedicalOrgShortInfoAuthorizedByThisAuthority()
//                    , MediaType.APPLICATION_JSON).build();
//        } catch (BlockChainObjectParsingException | IOException e) {
//            e.printStackTrace();
//            return Response.serverError().build();
//        }
//
//    }
//
//    /*
//     * return the corresponding DER encoded X509 certificate
//     */
//    @POST
//    @SecuredUserLevel
//    @Produces(MediaType.APPLICATION_JSON) // {"content": <DER encoded X509 certificate - byte array>} - Read with ByteArrayWrapper
//    @Consumes(MediaType.APPLICATION_JSON) // {"content": <Medical organization's identifier - byte array>} - use ByteArrayWrapper
//    @Path("medical-org/get-certificate")
//    public Response getCertificate(ByteArrayWrapper wrapper) {
//
//
//        try {
//            byte[] result = ValidatorRestServer.getRunningServer().getAPIResolver().getCertificate(wrapper.getContent());
//            return Response.ok(new ByteArrayWrapper(result), MediaType.APPLICATION_JSON).build();
//        } catch (CertificateEncodingException e) {
//            e.printStackTrace();
//            return Response.serverError().build();
//        } catch (NotFound e) {
//            e.printStackTrace();
//            return Response.status(Response.Status.NOT_FOUND).build();
//        } catch (BadRequest e) {
//            e.printStackTrace();
//            return Response.status(Response.Status.BAD_REQUEST).build();
//        }
//
//    }
//
//    /*
//     * return status code(1 byte) + encoded certificate:
//     * status code:
//     * 0: successful
//     * 1: not authorized
//     * 2: not authorized by this authority
//     */
//    @POST
//    @SecuredUserLevel
//    @Produces(MediaType.APPLICATION_JSON)// {"content": <Status code(1 byte) + DER encoded certificate(if success) - byte array>} - Read with ByteArrayWrapper
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Path("medical-org/renew-certificate")
//    public Response renewCertificate(AuthorizationRequestPojo authorizationRequestPojo) {
//
//
//        try {
//            return Response.ok(new ByteArrayWrapper(ValidatorRestServer.getRunningServer().getAPIResolver().renewCertificate(authorizationRequestPojo))
//                    , MediaType.APPLICATION_JSON).build();
//        } catch (FileCorruptionException|OperatorCreationException | BlockChainObjectParsingException | NoSuchAlgorithmException | IOException | CertificateException e) {
//            e.printStackTrace();
//            return Response.serverError().build();
//        } catch (InvalidKeySpecException | BadRequest e) {
//            return Response.status(Response.Status.BAD_REQUEST).build();
//        }
//
//    }

}
