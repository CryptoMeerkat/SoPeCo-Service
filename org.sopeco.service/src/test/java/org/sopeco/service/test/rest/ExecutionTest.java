package org.sopeco.service.test.rest;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MediaType;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sopeco.persistence.entities.definition.ExperimentSeriesDefinition;
import org.sopeco.persistence.entities.definition.ScenarioDefinition;
import org.sopeco.service.configuration.ServiceConfiguration;
import org.sopeco.service.persistence.entities.Account;
import org.sopeco.service.persistence.entities.ScheduledExperiment;
import org.sopeco.service.rest.StartUpService;
import org.sopeco.service.rest.json.CustomObjectWrapper;
import org.sopeco.service.shared.Message;
import org.sopeco.service.test.configuration.TestConfiguration;
import org.sopeco.service.test.rest.fake.TestMEC;

import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

public class ExecutionTest extends JerseyTest {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionTest.class.getName());
	
	public ExecutionTest() {
		super();
	}

	/**
	 * Configure is called on the object creation of a JerseyTest. It's used to
	 * configure where the JerseyTest can find JSON, the REST service to test
	 * and the JSON POJO.
	 * 
	 * @return the configuration
	 */
	@Override
	public WebAppDescriptor configure() {
		return new WebAppDescriptor.Builder(TestConfiguration.PACKAGE_NAME_REST)
				.contextListenerClass(StartUpService.class)
				.clientConfig(createClientConfig())
				.build();
	}

	/**
	 * Sets the client config for the client. The method is only used
	 * to give the possiblity to adjust the ClientConfig.
	 * 
	 * This method is called by {@link configure()}.
	 * 
	 * @return ClientConfig to work with JSON
	 */
	private static ClientConfig createClientConfig() {
		ClientConfig config = new DefaultClientConfig();
	    config.getClasses().add(CustomObjectWrapper.class);
	    return config;
	}
	
	@Test
	public void testExecution() {
		// connect to test users account
		String accountname = TestConfiguration.TESTACCOUNTNAME;
		String password = TestConfiguration.TESTPASSWORD;
		String measurmentspecificationname = "examplespecname";
		String scenarioname = "examplescenario";
		
		Message m = resource().path(ServiceConfiguration.SVC_ACCOUNT)
							  .path(ServiceConfiguration.SVC_ACCOUNT_LOGIN)
							  .queryParam(ServiceConfiguration.SVCP_ACCOUNT_NAME, accountname)
							  .queryParam(ServiceConfiguration.SVCP_ACCOUNT_PASSWORD, password)
							  .get(Message.class);
		
		String token = m.getMessage();

		// account is needed for account id
		Account account = resource().path(ServiceConfiguration.SVC_ACCOUNT)
								    .path(ServiceConfiguration.SVC_ACCOUNT_CONNECTED)
								    .queryParam(ServiceConfiguration.SVCP_ACCOUNT_TOKEN, token)
								    .accept(MediaType.APPLICATION_JSON)
								    .get(Account.class);
		
		// add scenario and switch to
		ExperimentSeriesDefinition esd = new ExperimentSeriesDefinition();
		esd.setName("experimentSeriesDefintion");
		resource().path(ServiceConfiguration.SVC_SCENARIO)
				  .path(ServiceConfiguration.SVC_SCENARIO_ADD)
				  .path(scenarioname)
				  .queryParam(ServiceConfiguration.SVCP_SCENARIO_SPECNAME, measurmentspecificationname)
				  .queryParam(ServiceConfiguration.SVCP_SCENARIO_TOKEN, token)
				  .accept(MediaType.APPLICATION_JSON)
				  .type(MediaType.APPLICATION_JSON)
				  .post(Boolean.class, esd);

		resource().path(ServiceConfiguration.SVC_SCENARIO)
				  .path(ServiceConfiguration.SVC_SCENARIO_SWITCH)
				  .path(ServiceConfiguration.SVC_SCENARIO_SWITCH_NAME)
				  .queryParam(ServiceConfiguration.SVCP_SCENARIO_NAME, scenarioname)
				  .queryParam(ServiceConfiguration.SVCP_SCENARIO_TOKEN, token)
				  .accept(MediaType.APPLICATION_JSON)
				  .put(Boolean.class);

		ScenarioDefinition sd = resource().path(ServiceConfiguration.SVC_SCENARIO)
										  .path(ServiceConfiguration.SVC_SCENARIO_CURRENT)
										  .queryParam(ServiceConfiguration.SVCP_SCENARIO_TOKEN, token)
										  .type(MediaType.APPLICATION_JSON)
										  .get(ScenarioDefinition.class);

		assertEquals(true, sd != null); // the user must have a scenario now
		
		//switch to created measurement specification
		Boolean b = resource().path(ServiceConfiguration.SVC_MEASUREMENT)
							  .path(ServiceConfiguration.SVC_MEASUREMENT_SWITCH)
							  .queryParam(ServiceConfiguration.SVCP_MEASUREMENT_NAME, measurmentspecificationname)
							  .queryParam(ServiceConfiguration.SVCP_MEASUREMENT_TOKEN, token)
							  .accept(MediaType.APPLICATION_JSON)
							  .put(Boolean.class);
		
		// now start the MEC fake, which connects to the ServerSocket created by the RESTful service
		TestMEC.start();
		
		boolean repeating = false;
		String controllerURL = "socket://" + TestMEC.MEC_ID + "/" + TestMEC.MEC_SUB_ID_1;
		String label = "myScheduledExperiment";
		long accountId = account.getId();
		boolean scenarioActive = true;
		long addedTime = System.currentTimeMillis();
		
		ScheduledExperiment se = new ScheduledExperiment();
		se.setScenarioDefinition(sd);
		se.setAccountId(accountId);
		se.setControllerUrl(controllerURL);
		se.setRepeating(repeating);
		se.setLabel(label);
		se.setActive(scenarioActive);
		se.setAddedTime(addedTime);
		
		// add to execution list
		b = resource().path(ServiceConfiguration.SVC_EXECUTE)
					  .path(ServiceConfiguration.SVC_EXECUTE_SCHEDULE)
					  .queryParam(ServiceConfiguration.SVCP_SCENARIO_TOKEN, token)
					  .accept(MediaType.APPLICATION_JSON)
					  .type(MediaType.APPLICATION_JSON)
					  .post(Boolean.class, se);

		assertEquals(true, b);
		
		// get if for the added scenario
		long tmpID = resource().path(ServiceConfiguration.SVC_EXECUTE)
						       .path(ServiceConfiguration.SVC_EXECUTE_ID)
							   .queryParam(ServiceConfiguration.SVCP_SCENARIO_TOKEN, token)
							   .accept(MediaType.APPLICATION_JSON)
							   .type(MediaType.APPLICATION_JSON)
							   .put(long.class, se);
		
		String id = String.valueOf(tmpID);
		
		b =  resource().path(ServiceConfiguration.SVC_EXECUTE)
				       .path(id)
				       .path(ServiceConfiguration.SVC_EXECUTE_EXECUTE)
				       .queryParam(ServiceConfiguration.SVCP_SCENARIO_TOKEN, token)
				       .accept(MediaType.APPLICATION_JSON)
				       .type(MediaType.APPLICATION_JSON)
				       .put(Boolean.class);
		
		assertEquals(true, b);
	}
	
}
