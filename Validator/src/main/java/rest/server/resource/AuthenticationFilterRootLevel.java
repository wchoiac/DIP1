package rest.server.resource;


import org.glassfish.grizzly.http.server.Request;
import rest.server.ValidatorRestServer;
import rest.server.manager.SessionManager;

import javax.annotation.Priority;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;


// reference: https://stackoverflow.com/questions/26777083/best-practice-for-rest-token-based-authentication-with-jax-rs-and-jersey
@SecuredRootLevel
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilterRootLevel implements ContainerRequestFilter {


    @Context
    private javax.inject.Provider<Request> requestProvider;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        String token = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        if ( token == null) {
            abortWithUnauthorized(requestContext);
            throw new NotAuthorizedException("Authorization header must be provided");
        }

        if(!ValidatorRestServer.getRunningServer().getAPIResolver().isValidRootLevelToken(token)){
            abortWithUnauthorized(requestContext);
            return;
        }
        Request request=requestProvider.get();
        ValidatorRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr()+": access to "+request.getRequestURI()+ " as "+ SessionManager.getUserName(token));

    }
    private void abortWithUnauthorized(ContainerRequestContext requestContext) {
        Request request=requestProvider.get();
        ValidatorRestServer.getRunningServer().getAPILogger().info(request.getRemoteAddr()+": unauthorized access to "+request.getRequestURI());
        requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED).build());
    }
}
