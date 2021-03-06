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
package org.sopeco.service.persistence.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sopeco.service.configuration.ServiceConfiguration;

/**
 * The {@link Users} class stores all important information for one user in the database.
 * E.g. the current selected scenario of a user is stored here.
 * <br />
 * <br />
 * The class cannot be names <code>User</code>, as this name is prohibited to use in a
 * Derby/MySQL database.
 * 
 * @author Peter Merkert
 */
@Entity
@NamedQuery(name = "getUserByToken", query = "SELECT u FROM Users u WHERE u.token = :token")
public class Users {

	private static final Logger LOGGER = LoggerFactory.getLogger(Users.class.getName());

	@Id
	@Column(name = "token")
	private String token;
	
	@Column(name = "accountID")
	private long accountID;
	
	@Column(name = "lastRequestTime")
	private long lastRequestTime;

	protected Users() {
	}
	
	/**
	 * Creates a user with the given data. The last reuqeust time is automatically
	 * initialized with the currentTimeMillis().
	 * 
	 * @param token 	the user unique token
	 * @param accountID the account ID this user is connected to
	 */
	public Users(String token, long accountID) {
		this.token = token;
		lastRequestTime = System.currentTimeMillis();
		this.accountID = accountID;
	}
	
	// ******************************* Setter & Getter ************************************

	public String getToken() {
		return token;
	}
	
	public void setAccountID(long accountID) {
		this.accountID = accountID;
	}

	public long getAccountID() {
		return accountID;
	}
	
	public long getLastRequestTime() {
		return lastRequestTime;
	}

	public void setLastRequestTime(long pLastRequestTime) {
		this.lastRequestTime = pLastRequestTime;
	}
	
	// ******************************* Custom methods ************************************
	
	/**
	 * Returns whether the user token has timed out.
	 * 
	 * @return true, if the user token has times out
	 */
	public boolean isExpired() {
		LOGGER.debug("Checking user with token '{}' for being expired.");
		
		long userTimeout = ServiceConfiguration.USER_TIMEOUT;
		
		if (userTimeout == 0 || System.currentTimeMillis() < lastRequestTime + userTimeout) {
			
			return false;
			
		} else {
			
			return true;
			
		}
	}
	
	@Override
	public String toString() {
		return 	"_persisted user information_" + "\n"
				+ "token: " + token + "\n"
				+ "last action: " + lastRequestTime + "\n";
	}
	
}
