package rest.server.resource;

import blockchain.block.PatientInfo;
import exception.BlockChainObjectParsingException;
import exception.FileCorruptionException;
import org.bouncycastle.operator.OperatorCreationException;
import org.glassfish.grizzly.http.server.Request;
import rest.pojo.*;
import rest.server.ValidatorRestServer;
import rest.server.exception.BadRequest;
import rest.server.exception.InvalidUserInfo;
import rest.server.exception.NotFound;
import rest.server.exception.ServerError;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;


//hash algorithm is always SHA256
//signature algorithm is always SHA256withECDSA

/**
 *
 */
@Provider
@Path("/")
public class ValidatorResource {


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("test")
    public Response test(@Context Request request, @QueryParam("test") String test) {

        System.out.println(test + " " + request.getRequestURI());

        return Response.serverError().build();
    }

    /*
     *  If successful, Secure token
     *  Else, return "login failed"
     */
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("user/login")
    public Response login(@Context Request request, UserInfoPojo userInfoPojo) {

        String token;
        try {
            token = ValidatorRestServer.getRunningServer().getAPIResolver().apiLogin(userInfoPojo);
        } catch (IOException e) {
            e.printStackTrace();
            return Response.serverError().build();
        } catch (BadRequest e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidUserInfo e) {
            ValidatorRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ": login fail " + userInfoPojo.getUsername());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        ValidatorRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ": login success(" + userInfoPojo.getUsername() + ")");
        return Response.ok(token,MediaType.TEXT_PLAIN).build();

    }

    /*
     * return list of authority information
     */
    @GET
    @SecuredRootLevel
    @Produces(MediaType.APPLICATION_JSON) // Read with AuthorityInfoPojo[]
    @Path("authority/get-overall-list")
    public Response getOverallList() {

        try {
            return Response.ok(ValidatorRestServer.getRunningServer().getAPIResolver().getOverallList(), MediaType.APPLICATION_JSON).build();
        } catch (IOException | BlockChainObjectParsingException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }


    /*
     * return list of votes that to be processed
     */
    @GET
    @SecuredRootLevel
    @Produces(MediaType.APPLICATION_JSON) // Read with VotePojo[]
    @Path("authority/vote/get-processing-list")
    public Response getMyVotes() {

        return Response.ok(ValidatorRestServer.getRunningServer().getAPIResolver().getMyVotes()
                , MediaType.APPLICATION_JSON).build();

    }


    /*
     * return list of on-going votings
     */
    @GET
    @SecuredRootLevel
    @Produces(MediaType.APPLICATION_JSON) //Read with VotingPojo[]
    @Path("authority/vote/get-current-list")
    public Response getCurrentVotingList() {

        return Response.ok(ValidatorRestServer.getRunningServer().getAPIResolver().getCurrentVotingList()
                , MediaType.APPLICATION_JSON).build();

    }


    /*
     *return status code:
     * 0: successful
     * 1: being processed
     * 2: cannot be processed (e.g. already voted)
     */
    @POST
    @SecuredRootLevel
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("authority/vote/cast")
    public Response castVote(VotePojo votePojo) {
        try {
            return Response.ok("" + ValidatorRestServer.getRunningServer().getAPIResolver().castVote(votePojo), MediaType.TEXT_PLAIN).build();
        } catch (IOException | BlockChainObjectParsingException e) {
            e.printStackTrace();
            return Response.serverError().build();
        } catch (InvalidKeySpecException | BadRequest e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    /*
     * return list of short information of the patient
     */
    @POST
    @SecuredUserLevel
    @Produces(MediaType.APPLICATION_JSON) // Read with PatientShortInfoPojo[]
    @Consumes(MediaType.APPLICATION_JSON) // {"content": <Patient's identifier - byte array>} - use ByteArrayWrapper
    @Path("patient/get-patient-short-info-list")
    public Response getPatientShortInfoList(ByteArrayWrapper wrapper) {

        try {
            return Response.ok(ValidatorRestServer.getRunningServer().getAPIResolver().getPatientShortInfoList(wrapper.getContent())
                    , MediaType.APPLICATION_JSON).build();
        } catch (IOException | BlockChainObjectParsingException e) {
            e.printStackTrace();
            return Response.serverError().build();
        } catch (BadRequest e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (NotFound e) {
            e.printStackTrace();
            return Response.status(Response.Status.NOT_FOUND).build();
        }


    }

    /*
     * return list of the requested patient information contents
     */
    @POST
    @SecuredUserLevel
    @Produces(MediaType.APPLICATION_JSON) // read with PatientInfoContentPojo[]
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("patient/get-patient-info-content-list")
    public Response getPatientInfoContentsList(ArrayList<LocationPojo> locationPojos) {

        try {
            return Response.ok(ValidatorRestServer.getRunningServer().getAPIResolver().getPatientInfoContentsList(locationPojos)
                    , MediaType.APPLICATION_JSON).build();
        } catch (IOException | BlockChainObjectParsingException e) {
            e.printStackTrace();
            return Response.serverError().build();
        } catch (NotFound e) {
            e.printStackTrace();// may be just bad request
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (BadRequest e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

    }

    /*
     *return status code:
     * 0: successful
     * 1: being processed
     * 2: already exists
     *
     * To note, signature = signature( GeneralHelper.longToBytes(timeStamp) | encryptedInfo ) , "|" means concatenate
     */
    @PUT
    @SecuredUserLevel
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("patient/register")
    public Response register(PatientInfoPojo patientInfoPojo) {

        try {
            return Response.ok("" + ValidatorRestServer.getRunningServer().getAPIResolver().register(patientInfoPojo)
                    , MediaType.TEXT_PLAIN).build();
        } catch (ServerError|IOException | BlockChainObjectParsingException e) {
            e.printStackTrace();
            return Response.serverError().build();
        } catch (InvalidKeySpecException | BadRequest e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

    }

    /*
     *return status code:
     * 0: successful
     * 1: being processed
     * 2: patient doesn't exist
     * 3: already updated
     *
     * To note, signature = signature( GeneralHelper.longToBytes(timeStamp) | encryptedInfo ), "|" means concatenate
     */
    @PUT
    @SecuredUserLevel
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("patient/update")
    public Response update(PatientInfoPojo patientInfoPojo) {

        try {
            return Response.ok("" + ValidatorRestServer.getRunningServer().getAPIResolver().update(patientInfoPojo)
                    , MediaType.TEXT_PLAIN).build();
        } catch (ServerError|IOException | BlockChainObjectParsingException e) {
            e.printStackTrace();
            return Response.serverError().build();
        } catch (InvalidKeyException|SignatureException|InvalidKeySpecException | BadRequest e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    /*
     *return status code:
     * 0: successful
     * 1: being processed
     * 2: authorized by another authority
     */
    @PUT
    @SecuredUserLevel
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON) //{"content": <Medical organization's identifier - byte array>}
    @Path("medical-org/revoke")
    public Response revoke(ByteArrayWrapper byteArrayWrapper) {

        try {
            return Response.ok("" + ValidatorRestServer.getRunningServer().getAPIResolver().revoke(byteArrayWrapper.getContent())
                    , MediaType.TEXT_PLAIN).build();
        } catch (IOException | BlockChainObjectParsingException|FileCorruptionException e) {
            e.printStackTrace();
            return Response.serverError().build();
        } catch (BadRequest e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }  catch (NotFound notFound) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

    }

    /*
     * return status code(1 byte) + encoded certificate(if success):
     * status code:
     * 0: successful
     * 1: being processed
     * 2: already exists
     */
    @SecuredUserLevel
    @PUT
    @Produces(MediaType.APPLICATION_JSON)// {"content": <Status code(1 byte) + DER encoded certificate(if success) - byte array>} - use ByteArrayWrapper
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("medical-org/authorize")
    public Response authorize(AuthorizationRequestPojo authorizationRequestPojo) {


        try {
            return Response.ok(new ByteArrayWrapper(ValidatorRestServer.getRunningServer().getAPIResolver().authorize(authorizationRequestPojo))
                    , MediaType.APPLICATION_JSON).build();
        } catch (FileCorruptionException|OperatorCreationException | BlockChainObjectParsingException | NoSuchAlgorithmException | IOException | CertificateException e) {
            e.printStackTrace();
            return Response.serverError().build();
        } catch (InvalidKeySpecException | BadRequest e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /*
     * return list of information of medical organizations authorized by this authority
     */
    @GET
    @SecuredUserLevel
    @Produces(MediaType.APPLICATION_JSON) // Read with MedicalOrgShortInfoPojo[]
    @Path("medical-org/get-authorization-list")
    public Response loadAllMedicalOrgShortInfoAuthorizedByMe() {

        try {
            return Response.ok(ValidatorRestServer.getRunningServer().getAPIResolver().loadAllMedicalOrgShortInfoAuthorizedByThisAuthority()
                    , MediaType.APPLICATION_JSON).build();
        } catch (BlockChainObjectParsingException | IOException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }

    }

    /*
     * return the corresponding DER encoded X509 certificate
     */
    @POST
    @SecuredUserLevel
    @Produces(MediaType.APPLICATION_JSON) // {"content": <DER encoded X509 certificate - byte array>} - Read with ByteArrayWrapper
    @Consumes(MediaType.APPLICATION_JSON) // {"content": <Medical organization's identifier - byte array>} - use ByteArrayWrapper
    @Path("medical-org/get-certificate")
    public Response getCertificate(ByteArrayWrapper wrapper) {


        try {
            byte[] result = ValidatorRestServer.getRunningServer().getAPIResolver().getCertificate(wrapper.getContent());
            return Response.ok(new ByteArrayWrapper(result), MediaType.APPLICATION_JSON).build();
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
            return Response.serverError().build();
        } catch (NotFound e) {
            e.printStackTrace();
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (BadRequest e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

    }

    /*
     * return status code(1 byte) + encoded certificate:
     * status code:
     * 0: successful
     * 1: not authorized
     * 2: not authorized by this authority
     */
    @POST
    @SecuredUserLevel
    @Produces(MediaType.APPLICATION_JSON)// {"content": <Status code(1 byte) + DER encoded certificate(if success) - byte array>} - Read with ByteArrayWrapper
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("medical-org/renew-certificate")
    public Response renewCertificate(AuthorizationRequestPojo authorizationRequestPojo) {


        try {
            return Response.ok(new ByteArrayWrapper(ValidatorRestServer.getRunningServer().getAPIResolver().renewCertificate(authorizationRequestPojo))
                    , MediaType.APPLICATION_JSON).build();
        } catch (FileCorruptionException|OperatorCreationException | BlockChainObjectParsingException | NoSuchAlgorithmException | IOException | CertificateException e) {
            e.printStackTrace();
            return Response.serverError().build();
        } catch (InvalidKeySpecException | BadRequest e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

    }

}
