/*
 * RSConnectAuthUser.java
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

public class RSConnectAuthUser extends JavaScriptObject
{
   protected RSConnectAuthUser()
   {
   }
   
   public final native boolean isValidUser() /*-{
      return this !== null && this.hasOwnProperty("id");
   }-*/;
   
   public final native int getId() /*-{
      return this.id;
   }-*/;
   
   public final native String getEmail() /*-{
      return this.email;
   }-*/;

   public final native String getUsername() /*-{
      return this.username;
   }-*/;

   public final native String getFirstName() /*-{
      return this.first_name;
   }-*/;

   public final native String getLastName() /*-{
      return this.last_name;
   }-*/;
}
