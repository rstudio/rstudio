/*
 * NewRSConnectAccountResult.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.rsconnect.model;

public class NewRSConnectAccountResult
{
   public enum AccountType
   {
      RSConnectCloudAccount,
      RSConnectLocalAccount
   }
   
   public NewRSConnectAccountResult(String cloudSecret)
   {
      type_ = AccountType.RSConnectCloudAccount;
      cloudSecret_ = cloudSecret;
   }
   
   public NewRSConnectAccountResult(String serverName, String serverUrl, 
                                    String accountNickname)
   {
      type_ = AccountType.RSConnectLocalAccount;
      serverName_ = serverName;
      serverUrl_ = serverUrl;
      accountNickname_ = accountNickname;
   }
   
   public String getCloudSecret()
   {
      return cloudSecret_;
   }
   
   public String getServerName()
   {
      return serverName_;
   }

   public String getServerUrl()
   {
      return serverUrl_;
   }
   
   public String getAccountNickname()
   {
      return accountNickname_;
   }
   
   public void setAccountNickname(String nick)
   {
      accountNickname_ = nick;
   }
   
   public AccountType getAccountType()
   {
      return type_;
   }
   
   public RSConnectServerInfo getServerInfo()
   {
      return serverInfo_;
   }
   
   public RSConnectPreAuthToken getPreAuthToken()
   {
      return preAuthToken_;
   }
   
   public RSConnectAuthUser getAuthUser()
   {
      return authUser_;
   }
   
   public void setServerInfo (RSConnectServerInfo info)
   {
      serverInfo_ = info;
   }
   
   public void setPreAuthToken (RSConnectPreAuthToken token)
   {
      preAuthToken_ = token;
   }
   
   public void setAuthUser (RSConnectAuthUser user)
   {
      authUser_ = user;
   }

   private AccountType type_;
   private String cloudSecret_;
   private String serverName_;
   private String serverUrl_;
   private String accountNickname_;
   
   private RSConnectServerInfo serverInfo_;
   private RSConnectPreAuthToken preAuthToken_;
   private RSConnectAuthUser authUser_;
}
