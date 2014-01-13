package org.sopeco.service.rest;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sopeco.persistence.entities.definition.ExperimentSeriesDefinition;
import org.sopeco.service.configuration.TestConfiguration;
import org.sopeco.service.configuration.ServiceConfiguration;
import org.sopeco.service.shared.Message;

import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

public class MeasurementSpecServiceTest extends JerseyTest {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(MeasurementSpecServiceTest.class.getName());
	
	public MeasurementSpecServiceTest() {
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
		return new WebAppDescriptor.Builder(new String[] {
				TestConfiguration.PACKAGE_NAME_JSON,
				TestConfiguration.PACKAGE_NAME_REST })
				.initParam(TestConfiguration.PACKAGE_NAME_POJO, "true")
				.clientConfig(createClientConfig())
				.build();
	}

	/**
	 * Sets the client config for the client to accept JSON and
	 * converting JSON Object to POJOs.
	 * This method is called by {@link configure()}.
	 * 
	 * @return ClientConfig to work with JSON
	 */
	private static ClientConfig createClientConfig() {
		ClientConfig config = new DefaultClientConfig();
	    config.getClasses().add(JacksonJsonProvider.class);
	    config.getFeatures().put(TestConfiguration.PACKAGE_NAME_POJO, Boolean.TRUE);
	    return config;
	}
	
	/**
	 * Checks if it is possible to resgister an account twice.
	 */
	@Test
	public void testMeasurementSpecNameListing() {
		String accountname = TestConfiguration.TESTACCOUNTNAME;
		String password = TestConfiguration.TESTPASSWORD;
		
		// just create the account once to be sure it already exists
		Message m = resource().path(ServiceConfiguration.SVC_ACCOUNT)
							  .path(ServiceConfiguration.SVC_ACCOUNT_LOGIN)
							  .queryParam(ServiceConfiguration.SVCP_ACCOUNT_NAME, accountname)
							  .queryParam(ServiceConfiguration.SVCP_ACCOUNT_PASSWORD, password)
							  .get(Message.class);
		
		String token = m.getMessage();
		 
		// add at least the examplescenario for ensurance that there is an measurementSpec available
		ExperimentSeriesDefinition esd = new ExperimentSeriesDefinition();
		resource().path(ServiceConfiguration.SVC_SCENARIO)
				  .path(ServiceConfiguration.SVC_SCENARIO_ADD)
				  .path("examplescenario")
				  .queryParam(ServiceConfiguration.SVCP_SCENARIO_SPECNAME, "examplespecname")
				  .queryParam(ServiceConfiguration.SVCP_SCENARIO_TOKEN, token)
				  .accept(MediaType.APPLICATION_JSON)
				  .type(MediaType.APPLICATION_JSON)
				  .post(Boolean.class, esd);
		
		// TODO switch to the scenario just build
		
		@SuppressWarnings("unchecked")
		List<String> measurementList = (List<String>)resource().path(ServiceConfiguration.SVC_MEASUREMENT)
														       .path(ServiceConfiguration.SVC_MEASUREMENT_LIST)
															   .queryParam(ServiceConfiguration.SVCP_MEASUREMENT_TOKEN, token)
														       .get(List.class);

		assertEquals(true, measurementList.size() > 0);
	}
	

	@Test
	public void testMeasurementSpecNameDoubleAdding() {
		String accountname = TestConfiguration.TESTACCOUNTNAME;
		String password = TestConfiguration.TESTPASSWORD;
		
		// just create the account once to be sure it already exists
		Message m = resource().path(ServiceConfiguration.SVC_ACCOUNT)
							  .path(ServiceConfiguration.SVC_ACCOUNT_LOGIN)
							  .queryParam(ServiceConfiguration.SVCP_ACCOUNT_NAME, accountname)
							  .queryParam(ServiceConfiguration.SVCP_ACCOUNT_PASSWORD, password)
							  .get(Message.class);
		
		String token = m.getMessage();
		
		System.out.println("Tokenized: " + token);
		
		// add at least the examplescenario for ensurance that there is an measurementSpec available
		ExperimentSeriesDefinition esd = new ExperimentSeriesDefinition();
		resource().path(ServiceConfiguration.SVC_SCENARIO)
				  .path(ServiceConfiguration.SVC_SCENARIO_ADD)
				  .path("examplescenario")
				  .queryParam(ServiceConfiguration.SVCP_SCENARIO_SPECNAME, "examplespecname")
				  .queryParam(ServiceConfiguration.SVCP_SCENARIO_TOKEN, token)
				  .accept(MediaType.APPLICATION_JSON)
				  .type(MediaType.APPLICATION_JSON)
				  .post(Boolean.class, esd);
		
		// switch to scenario (if not already in this scenario)
		resource().path(ServiceConfiguration.SVC_SCENARIO)
				  .path(ServiceConfiguration.SVC_SCENARIO_SWITCH)
				  .queryParam(ServiceConfiguration.SVCP_SCENARIO_NAME, "examplescenario")
				  .queryParam(ServiceConfiguration.SVCP_SCENARIO_TOKEN, token)
				  .accept(MediaType.APPLICATION_JSON)
				  .put(Boolean.class);
		
		// now create the measurement spec for the user once
		resource().path(ServiceConfiguration.SVC_MEASUREMENT)
		          .path(ServiceConfiguration.SVC_MEASUREMENT_CREATE)
		          .queryParam(ServiceConfiguration.SVCP_MEASUREMENT_TOKEN, token)
		          .queryParam(ServiceConfiguration.SVCP_MEASUREMENT_NAME, "measurementspecexample")
		          .post(Boolean.class);
		
		//create it now a second time, this must fail
		Boolean b = resource().path(ServiceConfiguration.SVC_MEASUREMENT)
					          .path(ServiceConfiguration.SVC_MEASUREMENT_CREATE)
					          .queryParam(ServiceConfiguration.SVCP_MEASUREMENT_TOKEN, token)
					          .queryParam(ServiceConfiguration.SVCP_MEASUREMENT_NAME, "measurementspecexample")
					          .post(Boolean.class);
		
		// the second addition must fail
		assertEquals(false, b);
	}
}
