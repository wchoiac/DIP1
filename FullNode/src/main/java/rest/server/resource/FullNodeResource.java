package rest.server.resource;

import exception.BlockChainObjectParsingException;
import exception.FileCorruptionException;
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
            e.printStackTrace();
            return Response.serverError().build();
        } catch (BadRequest e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidUserInfo e) {
            FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ": login fail " + userInfoPojo.getUsername());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        FullNodeRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr() + ": login success(" + userInfoPojo.getUsername() + ")");
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
     * To note, signature = signature( GeneralHelper.longToBytes(getTimestamp()) | encryptedRecord|medicalOrgIdentifier ) ), "|" means concatenate
     */

    @PUT
    @SecuredUserLevel
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("record/add-transaction")
    public Response addTransaction(TransactionPojo transactionPojo) {


        try {
            return Response.ok(new ByteArrayWrapper(FullNodeRestServer.getRunningServer().getAPIResolver().addTransaction(transactionPojo))
                    , MediaType.APPLICATION_JSON).build();
        } catch ( FileCorruptionException|BlockChainObjectParsingException | IOException  e) {
            e.printStackTrace();
            return Response.serverError().build();
        } catch (BadRequest e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).build();
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
    public Response getRecordShortInfoList(ByteArrayWrapper byteArrayWrapper) {


        try {
            return Response.ok(FullNodeRestServer.getRunningServer().getAPIResolver().getRecordShortInfoList(byteArrayWrapper.getContent())
                    , MediaType.APPLICATION_JSON).build();

        } catch (BlockChainObjectParsingException|IOException e) {
            e.printStackTrace();
            return Response.serverError().build();
        } catch (InvalidKeySpecException|BadRequest e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (NotFound e) {
            e.printStackTrace();
            return Response.status(Response.Status.NOT_FOUND).build(); //patient doesn't exist
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
    public Response getRecordContentsList(ArrayList<LocationPojo> locationPojos) {


        try {
            return Response.ok(FullNodeRestServer.getRunningServer().getAPIResolver().getRecordContentsList(locationPojos)
                    , MediaType.APPLICATION_JSON).build();
        } catch (IOException|BlockChainObjectParsingException e) {
            e.printStackTrace();
            return Response.serverError().build();
        } catch (NotFound e) {
            e.printStackTrace(); // patient not found
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (BadRequest e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).build();
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
    public Response getPatientShortInfoList(ByteArrayWrapper wrapper) {


        try {
            return Response.ok(FullNodeRestServer.getRunningServer().getAPIResolver().getPatientShortInfoList(wrapper.getContent())
                    , MediaType.APPLICATION_JSON).build();
        } catch (IOException | BlockChainObjectParsingException e) {
            e.printStackTrace();
            return Response.serverError().build();
        } catch (BadRequest e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (NotFound e) {
            e.printStackTrace(); // patient not found
            return Response.status(Response.Status.NOT_FOUND).build();
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
    @Path("patient/get-patient-info-content-list")
    public Response getPatientInfoContentsList(ArrayList<LocationPojo> locationPojos) {

        try {
            return Response.ok(FullNodeRestServer.getRunningServer().getAPIResolver().getPatientInfoContentsList(locationPojos)
                    , MediaType.APPLICATION_JSON).build();
        } catch (IOException | BlockChainObjectParsingException e) {
            e.printStackTrace();
            return Response.serverError().build();
        } catch (NotFound e) {
            e.printStackTrace();// patient not found
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (BadRequest e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).build();
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
