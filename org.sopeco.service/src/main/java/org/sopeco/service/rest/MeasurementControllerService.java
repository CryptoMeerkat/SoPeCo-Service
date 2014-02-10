package org.sopeco.service.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sopeco.engine.measurementenvironment.IMeasurementEnvironmentController;
import org.sopeco.engine.measurementenvironment.connector.MEConnectorFactory;
import org.sopeco.engine.measurementenvironment.socket.SocketAppWrapper;
import org.sopeco.engine.measurementenvironment.socket.SocketManager;
import org.sopeco.persistence.entities.definition.MeasurementEnvironmentDefinition;
import org.sopeco.service.configuration.ServiceConfiguration;
import org.sopeco.service.persistence.ServicePersistenceProvider;
import org.sopeco.service.persistence.entities.Users;
import org.sopeco.service.shared.MECStatus;
import org.sopeco.service.shared.ServiceResponse;

/**
 * The <code>MeasurementControllerService</code> provides services to handle MeasurementEnvironemtControllers (MEC)
 * of SoPeCo. The service alowes to request information about connected MECs.<br />
 * Currently only socket connection are allowed and can be used.
 * 
 * @author Peter Merkert
 */
@Path(ServiceConfiguration.SVC_MEC)
public class MeasurementControllerService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MeasurementControllerService.class);
	
	/**
	 * The URL pattern for a controller URL.
	 */
	private static final String[] CONTROLLER_URL_PATTERN = new String[] { "^socket://[a-zA-Z0-9\\.]+(:[0-9]{1,5})?/[a-zA-Z][a-zA-Z0-9]*$" };
	
	/**
	 * Returns the URI pattern for the socket connection URL.
	 * 
	 * @return the URI pattern for the socket connection URL
	 */
	@GET
	@Path(ServiceConfiguration.SVC_MEC_VALIDATE)
	@Produces(MediaType.APPLICATION_JSON)
	public String[] getValidUrlPattern() {
		return CONTROLLER_URL_PATTERN;
	}
	
	/**
	 * Returns the status of the MEC on the given URL. The status of the MEC can be fetched via
	 * the {@link MECStatus}. If the URL is inavlid or something happened to the MEC, null might
	 * be returned.
	 * 
	 * @param usertoken authentification of the user
	 * @param url 		the URL to the MEC
	 * @return 			the MEC status of the controller, null if a connection issue happened
	 */
	@GET
	@Path(ServiceConfiguration.SVC_MEC_STATUS)
	@Produces(MediaType.APPLICATION_JSON)
	public ServiceResponse<MECStatus> getMECStatus(@QueryParam(ServiceConfiguration.SVCP_MEC_TOKEN) String usertoken,
								  @QueryParam(ServiceConfiguration.SVCP_MEC_URL) String url) {
		
		Users u = ServicePersistenceProvider.getInstance().loadUser(usertoken);

		if (u == null) {
			LOGGER.warn("Invalid token '{}'!", usertoken);
			return new ServiceResponse<MECStatus>(Status.UNAUTHORIZED);
		}

		if (!checkUrlIsValid(url) || url == null) {
			LOGGER.debug("Controller-Status: NO_VALID_MEC_URL");
			return new ServiceResponse<MECStatus>(Status.OK, new MECStatus(MECStatus.NO_VALID_MEC_URL));
		}
		
	
		try {
			
			IMeasurementEnvironmentController mec = MEConnectorFactory.connectTo(new URI(url));

			if (mec == null) {
				LOGGER.debug("Controller not reachable: NO_VALID_MEC_URL");
				return new ServiceResponse<MECStatus>(Status.OK, new MECStatus(MECStatus.NO_VALID_MEC_URL));
			}
			
			MeasurementEnvironmentDefinition med = mec.getMEDefinition();

			if (med == null) {
				LOGGER.debug("Controller-Status: STATUS_ONLINE_NO_META");
				return new ServiceResponse<MECStatus>(Status.OK, new MECStatus(MECStatus.STATUS_ONLINE_NO_META));
			} else {
				LOGGER.debug("Controller-Status: STATUS_ONLINE");
				return new ServiceResponse<MECStatus>(Status.OK, new MECStatus(MECStatus.STATUS_ONLINE));
			}

		} catch (URISyntaxException e) {
			LOGGER.error(e.getMessage());
			return new ServiceResponse<MECStatus>(Status.CONFLICT, null, "invalid URI");
		} catch (RemoteException e) {
			LOGGER.error(e.getMessage());
			return new ServiceResponse<MECStatus>(Status.INTERNAL_SERVER_ERROR, null, "connection establishing failed");
		} catch (IllegalStateException x) {
			LOGGER.debug("Controller-Status: STATUS_OFFLINE");
			return new ServiceResponse<MECStatus>(Status.OK, new MECStatus(MECStatus.STATUS_OFFLINE));
		}
	
	}
	
	/**
	 * This methods returns the whole list for connected controllers on the given MEC ID.
	 * 
	 * @param usertoken authentification of the user
	 * @param id 		the MEC ID
	 * @return 			list of all controllers currently connected to given MEC ID with given, null
	 *  	   			if an error occured
	 */
	@GET
	@Path(ServiceConfiguration.SVC_MEC_LIST)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ServiceResponse<List<String>> getControllerList(@QueryParam(ServiceConfiguration.SVCP_MEC_TOKEN) String usertoken,
			      				     	  @QueryParam(ServiceConfiguration.SVCP_MEC_ID) String id) {
		
		Users u = ServicePersistenceProvider.getInstance().loadUser(usertoken);

		if (u == null) {
			LOGGER.info("Invalid token '{}'!", usertoken);
			return new ServiceResponse<List<String>>(Status.UNAUTHORIZED, null);
		}
		
		SocketAppWrapper app = SocketManager.getSocketApp(id);
		
		if (app == null) {
			LOGGER.info("SocketAppWrapper is in invalid state.");
			return new ServiceResponse<List<String>>(Status.INTERNAL_SERVER_ERROR, null);
		}
			
		List<String> tmpList = Arrays.asList(app.getAvailableController());

		return new ServiceResponse<List<String>>(Status.OK, tmpList);
	}
	
	/**
	 * Before: The MeasurementEnvironmentController should be connected to the service
	 * ServerSocket. Otherwise the method is going to fail and return null.
	 * <br />
	 * <br />
	 * The MED is requested from the from the MEC connected to the given URL.
	 * The MED is returned WITHOUT being stored in the database. This has to be done
	 * manually via the service at med/set!
	 * 
	 * @param usertoken authentification of the user
	 * @param uri 		the URI of the MeasurementEnvironmentController already connected to the
	 * 		  			ServerSocket of the service
	 * @return 			the {@code MeasurementEnvironmentDefinition} to the MEC on the given URI
	 */
	@GET
	@Path(ServiceConfiguration.SVC_MEC_MED)
	@Produces(MediaType.APPLICATION_JSON)
	public ServiceResponse<MeasurementEnvironmentDefinition> getMEDefinitionFromMEC(@QueryParam(ServiceConfiguration.SVCP_MEC_TOKEN) String usertoken,
																   @QueryParam(ServiceConfiguration.SVCP_MEC_URL) String uri) {
		
		Users u = ServicePersistenceProvider.getInstance().loadUser(usertoken);

		if (u == null) {
			LOGGER.info("Invalid token '{}'!", usertoken);
			return new ServiceResponse<MeasurementEnvironmentDefinition>(Status.UNAUTHORIZED, null);
		}
		
		try {
			
			// connect to the mec via Socket
			IMeasurementEnvironmentController mec = MEConnectorFactory.connectTo(new URI(uri));
			
			if (mec == null) {
				LOGGER.info("The connected MEC cannot be fetched correctly.");
				return new ServiceResponse<MeasurementEnvironmentDefinition>(Status.ACCEPTED, null, "The connected MEC cannot be fetched correctly.");
			}
			
			MeasurementEnvironmentDefinition med = mec.getMEDefinition();
			
			if (med == null) {
				LOGGER.info("The connected med has no valid MeasurementEnvironmentDefinition.");
				return new ServiceResponse<MeasurementEnvironmentDefinition>(Status.CONFLICT, null, "The connected med has no valid MeasurementEnvironmentDefinition.");
			}
			
			return new ServiceResponse<MeasurementEnvironmentDefinition>(Status.OK, med);
			
		} catch (URISyntaxException e) {
			LOGGER.error(e.getMessage());
			return new ServiceResponse<MeasurementEnvironmentDefinition>(Status.CONFLICT, null, "URI invalid");
		} catch (RemoteException e) {
			LOGGER.error(e.getMessage());
			return new ServiceResponse<MeasurementEnvironmentDefinition>(Status.INTERNAL_SERVER_ERROR, null, "remote exception");
		} catch (IllegalStateException x) {
			LOGGER.error("Controller probably offline.");
			return new ServiceResponse<MeasurementEnvironmentDefinition>(Status.INTERNAL_SERVER_ERROR, null, "controller probably offline");
		}
	}
	
	
	/**
	 * Before: The MeasurementEnvironmentController (MEC) should be connected to the service
	 * ServerSocket. Otherwise the method is going to fail and return false.
	 * <br />
	 * After getting the {@code MeasurementEnvironmentDefinition} (MED) from the MEC, the MED
	 * is stored in the account database.
	 * <br />
	 * This method only uses the services at {@code getMEDefinitionFromMEC()} and
	 * {@code MeasurementEnvironmentDefinitionService.setNewMEDefinition()}. See
	 * their comments for more information.
	 * 
	 * @param usertoken authentification of the user
	 * @param uri 		the URI of the MeasurementEnvironmentController already connected to the
	 * 		  			ServerSocket of the service
	 * @return 			true, if the MED from the MEC was saved successfully in the database
	 */
	@POST
	@Path(ServiceConfiguration.SVC_MEC_MED)
	@Produces(MediaType.APPLICATION_JSON)
	public ServiceResponse<Boolean> setMEDefinitionFromMEC(@QueryParam(ServiceConfiguration.SVCP_MEC_TOKEN) String usertoken,
										  @QueryParam(ServiceConfiguration.SVCP_MEC_URL) String uri) {
		
		Users u = ServicePersistenceProvider.getInstance().loadUser(usertoken);

		if (u == null) {
			LOGGER.info("Invalid token '{}'!", usertoken);
			return new ServiceResponse<Boolean>(Status.UNAUTHORIZED, false);
		}
		
		ServiceResponse<MeasurementEnvironmentDefinition> sr_med = getMEDefinitionFromMEC(usertoken, uri);
		return MeasurementEnvironmentDefinitionService.setNewMEDefinition(sr_med.getObject(), u);
	}
	
	/**************************************HELPER****************************************/

	/**
	 * Checks if the given URI is like a valid pattern.
	 * 
	 * @param uri 	the URI to check
	 * @return 		true, if the URI has a valid pattern
	 */
	private boolean checkUrlIsValid(String uri) {
		
		for (String pattern : CONTROLLER_URL_PATTERN) {
			if (uri.matches(pattern)) {
				return true;
			}
		}

		return false;
	}
	
}
