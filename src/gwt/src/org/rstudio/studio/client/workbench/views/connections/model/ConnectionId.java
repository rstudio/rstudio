/*
 * ConnectionId.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.workbench.views.connections.model;

import com.google.gwt.core.client.JavaScriptObject;

public class ConnectionId extends JavaScriptObject
{ 
   protected ConnectionId()
   {
   }
   
   public static native final ConnectionId create(String type, String host) /*-{ 
      return {
         type: type,
         host: host
      }; 
    }-*/;
   
   public final native String getType() /*-{
      return this.type;
   }-*/;
  
   public final native String getHost() /*-{
      return this.host;
   }-*/;
   
   public final String asString()
   {
      return getType() + " - " + getHost();
   }
   
   public final boolean equalTo(ConnectionId other)
   {
      return getType() == other.getType() && 
             getHost() == other.getHost();
   }
}
