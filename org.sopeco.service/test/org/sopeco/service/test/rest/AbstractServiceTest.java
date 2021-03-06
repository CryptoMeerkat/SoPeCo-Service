/**
 * Copyright (c) 2014 SAP
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the SAP nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SAP BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.sopeco.service.test.rest;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sopeco.persistence.entities.definition.ExperimentSeriesDefinition;
import org.sopeco.persistence.entities.definition.MeasurementSpecification;
import org.sopeco.persistence.entities.definition.ScenarioDefinition;
import org.sopeco.service.configuration.ServiceConfiguration;
import org.sopeco.service.rest.json.CustomObjectMapper;
import org.sopeco.service.test.configuration.TestConfiguration;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

/**
 * The {@link AbstractServiceTest} is used to bundle everything releated for the JUnit
 * tests:
 * <ul>
 * <li>server initialization</li>
 * <li>client initialization</li>
 * <li>database cleanup after test</li>
 * </ul>
 * 
 * @author Peter Merkert
 */
public abstract class AbstractServiceTest extends JerseyTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractServiceTest.class);
	
	/**
	 * True, when to skip all the test.
	 */
	protected static boolean skipTests = true;
	
	/**
	 * This method is called on the Grizzly container creation of a {@link JerseyTest}.
	 * It's used to configure where the servlet container.<br />
	 * In this case, the package is definied where the RESTful services are and
	 * the {@link CustomObjectMapper} is registered.
	 */
	@Override
    protected Application configure() {
		ResourceConfig rc = new ResourceConfig();
		rc.packages(ServiceConfiguration.PACKAGE_NAME_REST);

		// the ContainerLifeCycleListener is in the configuration package and need to be defined
		rc.packages(ServiceConfiguration.PACKAGE_NAME_LIFECYCLELISTENER);
		
		// the CustomObjectMapper must be wrapped into a Jackson Json Provider
		// otherwise Jersey does not recognize to use Jackson for JSON
		// converting
		JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
        provider.setMapper(new CustomObjectMapper());
		rc.register(provider);
		
		return rc;
    }

	/**
	 * The {@link Client} needs also the {@link CustomObjectMapper}, which
	 * defines the mixin used when the objects were serialized.
	 */
	@Override
	protected void configureClient(ClientConfig config) {
		JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
        provider.setMapper(new CustomObjectMapper());
        config.register(provider);
	}
	
	/**
	 * Cleans up the database means: Delete the scenario with name {@code 
	 * MeasurementControllerServiceTest.SCENARIO_NAME} in the database. This scenario
	 * is used by every single test and it can cause errors, when the scenario is created,
	 * but already in the database. Because the database instance is then not updated,
	 * which can result in unexpected behaviour.
	 * <br />
	 * In addition all the scheduled scenarios for the test account are deleted.
	 */
	@After
	public void cleanUpDatabase() {
		if (skipTests) return;
		
		LOGGER.debug("Cleaning up the database.");
		
		// connect to test users account
		String accountname = TestConfiguration.TESTACCOUNTNAME;
		String password = TestConfiguration.TESTPASSWORD;
		String scenarioNameEmpty = TestConfiguration.TEST_CLEAN_SCENARIO_NAME;
		String measSpecNameEmpty = TestConfiguration.TEST_CLEAN_MEASUREMENT_SPECIFICATION_NAME;
		
		Response r = target().path(ServiceConfiguration.SVC_ACCOUNT)
						     .path(ServiceConfiguration.SVC_ACCOUNT_LOGIN)
						     .queryParam(ServiceConfiguration.SVCP_ACCOUNT_NAME, accountname)
						     .queryParam(ServiceConfiguration.SVCP_ACCOUNT_PASSWORD, password)
			    	         .request(MediaType.APPLICATION_JSON)
						     .get();
						
		String token = r.readEntity(String.class);

		// clean the scheduling list for the user
		target().path(ServiceConfiguration.SVC_EXECUTE)
				.path(ServiceConfiguration.SVC_EXECUTE_SCHEDULE)
				.queryParam(ServiceConfiguration.SVCP_SCENARIO_TOKEN, token)
				.request(MediaType.APPLICATION_JSON)
				.delete();

		// TODO: delete all executed experiment details
		
		// now create empty scenario to delete the test scenario
		ExperimentSeriesDefinition esd = new ExperimentSeriesDefinition();
		target().path(ServiceConfiguration.SVC_SCENARIO)
				.path(ServiceConfiguration.SVC_SCENARIO_ADD)
				.path(scenarioNameEmpty)
				.queryParam(ServiceConfiguration.SVCP_SCENARIO_SPECNAME, measSpecNameEmpty)
				.queryParam(ServiceConfiguration.SVCP_SCENARIO_TOKEN, token)
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(esd, MediaType.APPLICATION_JSON));
		
		// delete the example scenario
		target().path(ServiceConfiguration.SVC_SCENARIO)
				.path(TestConfiguration.TEST_SCENARIO_NAME)
			    .queryParam(ServiceConfiguration.SVCP_SCENARIO_TOKEN, token)
			    .request(MediaType.APPLICATION_JSON)
			    .delete();
	}
	
	/**
	 * Logs the given user with the password in and returns the fetched token.
	 * 
	 * @param accountname	the account name
	 * @param password		the password corresponding to the account
	 * @return				the token
	 */
	protected String login(String accountname, String password) {
		Response r = target().path(ServiceConfiguration.SVC_ACCOUNT)
						     .path(ServiceConfiguration.SVC_ACCOUNT_LOGIN)
						     .queryParam(ServiceConfiguration.SVCP_ACCOUNT_NAME, accountname)
						     .queryParam(ServiceConfiguration.SVCP_ACCOUNT_PASSWORD, password)
			    	         .request(MediaType.APPLICATION_JSON)
						     .get();

		assertEquals(Status.OK.getStatusCode(), r.getStatus());
		
		return r.readEntity(String.class); // the token is in the Response object
	}
	
	/**
	 * Creates a scenario with the given data and returns the ScenarioDefinition.
	 * The passed {@link ExperimentSeriesDefinition} can be a just empty created one.
	 * 
	 * @param scenarioName	the name of the {@link ScenarioDefinition}
	 * @param measSpecName	the name of the {@link MeasurementSpecification}
	 * @param esd			the {@link ExperimentSeriesDefinition}
	 * @param token			the user identification
	 * @return				the created {@link ScenarioDefinition}
	 */
	protected ScenarioDefinition createScenario(String scenarioName, String measSpecName, ExperimentSeriesDefinition esd, String token) {
		
		// most times the scenario in the testing already exists, that's why no test on the Response are made
		target().path(ServiceConfiguration.SVC_SCENARIO)
				.path(ServiceConfiguration.SVC_SCENARIO_ADD)
				.path(scenarioName)
				.queryParam(ServiceConfiguration.SVCP_SCENARIO_SPECNAME, measSpecName)
				.queryParam(ServiceConfiguration.SVCP_SCENARIO_TOKEN, token)
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(esd, MediaType.APPLICATION_JSON));

		Response r = target().path(ServiceConfiguration.SVC_SCENARIO)
							 .path(scenarioName)
							 .path(ServiceConfiguration.SVC_SCENARIO_DEFINITON)
							 .queryParam(ServiceConfiguration.SVCP_SCENARIO_TOKEN, token)
							 .request(MediaType.APPLICATION_JSON)
							 .get();

		assertEquals(Status.OK.getStatusCode(), r.getStatus());
		
		return r.readEntity(ScenarioDefinition.class);
	}
	
	/**
	 * Logs the given user out.
	 * 
	 * @param token	the token corresponding to the user, which wants to be logged out	
	 */
	protected void logout(String token) {
		Response r = target().path(ServiceConfiguration.SVC_ACCOUNT)
						     .path(ServiceConfiguration.SVC_ACCOUNT_LOGOUT)
						     .queryParam(ServiceConfiguration.SVCP_ACCOUNT_TOKEN, token)
			    	         .request(MediaType.APPLICATION_JSON)
			 				 .delete();

		assertEquals(Status.OK.getStatusCode(), r.getStatus());
	}
	
}
