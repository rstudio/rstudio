/*
 * RSConnectPreAuthToken.java
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

import com.google.gwt.core.client.JavaScriptObject;

public class RSConnectPreAuthToken extends JavaScriptObject
{
   protected RSConnectPreAuthToken() 
   {
   }
   
   public final native String getToken() /*-{
      return this.token;
   }-*/;

   public final native String getPrivateKey() /*-{
      return this.private_key;
   }-*/;

   public final native String getClaimUrl() /*-{
      return this.claim_url;
   }-*/;
}
