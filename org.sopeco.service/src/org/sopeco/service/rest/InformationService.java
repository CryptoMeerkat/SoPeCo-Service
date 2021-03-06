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
package org.sopeco.service.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.sopeco.config.Configuration;
import org.sopeco.persistence.config.PersistenceConfiguration;
import org.sopeco.service.configuration.ServiceConfiguration;

/**
 * The <code>InformationService</code> provides simple information about the SoPeCo service layer.
 * When requested with a browsers, the default <code>getInformation()</code> method is called.
 * 
 * @author Peter Merkert
 */
@Path(ServiceConfiguration.SVC_INFO)
public class InformationService {

	/**
	 * Prints information about the SoPeCo Service.
	 * 
	 * @return information text about this service
	 */
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String getInformation() {

		PersistenceConfiguration conf = PersistenceConfiguration.getSessionSingleton(Configuration.getGlobalSessionId());
		
		return "<h1>Information about SoPeCo Service</h1>"
				+ "<p>Services can be requested.</p>"
				+ "<h2>Configuration information</h2>"
				+ "Metadata database: " + conf.getMetaDataHost() + ":" + conf.getMetaDataPort();
	}
	
	/**
	 * Returns always OK.
	 * 
	 * @return {@link Response} OK
	 */
	@GET
	@Path(ServiceConfiguration.SVC_INFO_RUNNING)
	@Produces(MediaType.APPLICATION_JSON)
	public Response running() {
		return Response.ok().build();
	}
	
}
