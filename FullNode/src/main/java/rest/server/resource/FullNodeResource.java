package rest.server.resource;

import blockchain.manager.BlockChainManager;
import blockchain.manager.datastructure.RecordShortInfo;
import exception.BlockChainObjectParsingException;
import exception.FileCorruptionException;
import general.utility.GeneralHelper;
import org.glassfish.grizzly.http.server.Request;
import rest.pojo.*;
import rest.server.FullNodeRestServer;
import rest.server.exception.BadRequest;
import rest.server.exception.InvalidUserInfo;
import rest.server.exception.NotFound;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;


//hash algorithm is always SHA256
//signature algorithm is always SHA256withECDSA

@Provider
@Path("/")
public class FullNodeResource {

    /*
     *  If successful, Secure token
     *  Else, return "login failed"
     */
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    //{"username": <API user's username - string>, "password": <APU user's password - char array>}
    @Path("user/login")
    public Response login(@Context Request request, UserInfoPojo userInfoPojo) {

        String token;
        try {
            token = FullNodeRestServer.getRunningServer().getAPIResolver().apiLogin(userInfoPojo);
        } catch (IOException e) {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: SERVER_ERROR\n"+GeneralHelper.getStackTrace(e));
            return Response.serverError().build();
        } catch (BadRequest e) {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: BAD_REQUEST\n"+GeneralHelper.getStackTrace(e));
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidUserInfo e) {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: UNAUTHORIZED\n"+" login fail due to invalid input" + userInfoPojo.getUsername());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: OK\nlogin success(" + userInfoPojo.getUsername() + ")");
        return Response.ok(token,MediaType.TEXT_PLAIN).build();

    }

    /*
     *return status code(1 byte) + transaction id(32 byte) (if successful):
     *
     * status code:
     * 0: successful
     * 1: being processed
     * 2: already exists or invalid (e.g. wrong signature)
     *
     * transaction id:
     * hash of the transaction
     *
     * To note, signature = signature( GeneralHelper.longToBytes(getTimestamp()) | encryptedRecord | medicalOrgIdentifier ), "|" means concatenate
     */

    @PUT
    @SecuredUserLevel
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("record/add-transaction")
    public Response addTransaction(@Context Request request, TransactionPojo transactionPojo) {


        try {
            ByteArrayWrapper result =new ByteArrayWrapper(FullNodeRestServer.getRunningServer().getAPIResolver().addTransaction(transactionPojo));
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: OK");
            return Response.ok(result, MediaType.APPLICATION_JSON).build();
        } catch ( FileCorruptionException|BlockChainObjectParsingException | IOException  e) {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: SERVER_ERROR\n"+GeneralHelper.getStackTrace(e));
            return Response.serverError().build();
        } catch (BadRequest e) {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: BAD_REQUEST\n"+GeneralHelper.getStackTrace(e));
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch(Exception e)
        {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: SERVER_ERROR\n"+GeneralHelper.getStackTrace(e));
            return Response.serverError().build();
        }

    }

    /*
     * return list of short information of the patient's records
     */
    @POST
    @SecuredUserLevel
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("record/get-record-short-info-list")
    public Response getRecordShortInfoList(@Context Request request, ByteArrayWrapper byteArrayWrapper) {


        try {

            ArrayList<RecordShortInfoPojo> recordShortInfoPojos =FullNodeRestServer.getRunningServer().getAPIResolver().getRecordShortInfoList(byteArrayWrapper.getContent());
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: OK");
            return Response.ok(recordShortInfoPojos, MediaType.APPLICATION_JSON).build();

        } catch (BlockChainObjectParsingException|IOException e) {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: SERVER_ERROR\n"+GeneralHelper.getStackTrace(e));
            return Response.serverError().build();
        } catch (InvalidKeySpecException|BadRequest e) {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: BAD_REQUEST\n"+GeneralHelper.getStackTrace(e));
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (NotFound e) {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: NOT_FOUND\n"+GeneralHelper.getStackTrace(e));
            return Response.status(Response.Status.NOT_FOUND).build(); //patient doesn't exist
        } catch(Exception e)
        {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: SERVER_ERROR\n"+GeneralHelper.getStackTrace(e));
            return Response.serverError().build();
        }

    }


    /*
     * return list of the requested records' contents
     * Or return null if error during processing
     */
    @POST
    @SecuredUserLevel
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("record/get-record-contents-list")
    public Response getRecordContentsList( @Context Request request,ArrayList<LocationPojo> locationPojos) {


        try {

            ArrayList<RecordContentPojo> recordContentPojos =FullNodeRestServer.getRunningServer().getAPIResolver().getRecordContentsList(locationPojos);
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: OK");
            return Response.ok(recordContentPojos, MediaType.APPLICATION_JSON).build();
        } catch (IOException|BlockChainObjectParsingException e) {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: SERVER_ERROR\n"+GeneralHelper.getStackTrace(e));
            return Response.serverError().build();
        } catch (NotFound e) {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: NOT_FOUND\n"+GeneralHelper.getStackTrace(e));
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (BadRequest e) {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: BAD_REQUEST\n"+GeneralHelper.getStackTrace(e));
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch(Exception e)
        {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: SERVER_ERROR\n"+GeneralHelper.getStackTrace(e));
            return Response.serverError().build();
        }

    }


    /*
     * return list of short information of the patient
     */
    @POST
    @SecuredUserLevel
    @Produces(MediaType.APPLICATION_JSON)
    // [{"location":{"blockHash": <Block hash - byte array>,"targetIdentifier":<Target hash - byte array>}, "timestamp": <Timestamp in millisecond (POSIX time)- long>},...]
    @Consumes(MediaType.APPLICATION_JSON) // {"content": <Patient's identifier - byte array>}
    @Path("patient/get-patient-short-info-list")
    public Response getPatientShortInfoList(@Context Request request, ByteArrayWrapper wrapper) {


        try {

            ArrayList<PatientShortInfoPojo> patientShortInfoPojos =FullNodeRestServer.getRunningServer().getAPIResolver().getPatientShortInfoList(wrapper.getContent());
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: OK");
            return Response.ok(patientShortInfoPojos, MediaType.APPLICATION_JSON).build();
        } catch (IOException | BlockChainObjectParsingException e) {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: SERVER_ERROR\n"+GeneralHelper.getStackTrace(e));
            return Response.serverError().build();
        } catch (BadRequest e) {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: BAD_REQUEST\n"+GeneralHelper.getStackTrace(e));
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (NotFound e) {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: NOT_FOUND\n"+GeneralHelper.getStackTrace(e));
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch(Exception e)
        {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: SERVER_ERROR\n"+GeneralHelper.getStackTrace(e));
            return Response.serverError().build();
        }


    }

    /*
     * return list of the requested patient information contents
     */
    @POST
    @SecuredUserLevel
    @Produces(MediaType.APPLICATION_JSON) // {"encryptedInfo": <Encrypted patient information - byte array>}
    @Consumes(MediaType.APPLICATION_JSON)
    // [{"location":{"blockHash": <Block hash - byte array>,"targetIdentifier":<Target hash - byte array>},....]
    @Path("patient/get-patient-info-contents-list")
    public Response getPatientInfoContentsList(@Context Request request, ArrayList<LocationPojo> locationPojos) {

        try {
            ArrayList<PatientInfoContentPojo> patientInfoContentPojos =FullNodeRestServer.getRunningServer().getAPIResolver().getPatientInfoContentsList(locationPojos);
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: OK");
            return Response.ok(patientInfoContentPojos, MediaType.APPLICATION_JSON).build();
        } catch (IOException | BlockChainObjectParsingException e) {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: SERVER_ERROR\n"+GeneralHelper.getStackTrace(e));
            return Response.serverError().build();
        } catch (NotFound e) {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: NOT_FOUND\n"+GeneralHelper.getStackTrace(e));
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (BadRequest e) {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: BAD_REQUEST\n"+GeneralHelper.getStackTrace(e));
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch(Exception e)
        {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ":"+"Response: SERVER_ERROR\n"+GeneralHelper.getStackTrace(e));
            return Response.serverError().build();
        }


    }

    /*
     * return this medical organization's identifier
     */
    @GET
    @SecuredUserLevel
    @Produces(MediaType.APPLICATION_JSON)
    @Path("medical-org/get-identifier")
    public Response getMedicalOrgIdentifier() {

        return Response.ok(new ByteArrayWrapper(FullNodeRestServer.getRunningServer().getAPIResolver().getMedicalOrgIdentifier())
                , MediaType.APPLICATION_JSON).build();

    }

}
