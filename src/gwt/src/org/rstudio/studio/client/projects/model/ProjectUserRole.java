/*
 * ProjectUserRole.java
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

package org.rstudio.studio.client.projects.model;

import com.google.gwt.core.client.JavaScriptObject;

public class ProjectUserRole extends JavaScriptObject
{
   protected ProjectUserRole()
   {
   }
   
   public final static native ProjectUserRole create(
         String username, String role) /*-{
      return {
         "username": username,
         "role"    : role
      };
   }-*/;
   
   public final native String getUsername() /*-{
      return this.username;
   }-*/;

   public final native String getRole() /*-{
      return this.role;
   }-*/;
   
   public final static String ROLE_COLLABORATOR = "collaborator";
   public final static String ROLE_OWNER = "owner";
}
