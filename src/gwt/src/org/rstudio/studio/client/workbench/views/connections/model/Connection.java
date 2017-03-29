/*
 * Connection.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.connections.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class Connection extends JavaScriptObject
{ 
   protected Connection()
   {
   }
   
   public final native ConnectionId getId() /*-{
      return this.id;
   }-*/;
   
   public final native String getHost() /*-{
      return this.id.host;
   }-*/;
   
   public final native String getDisplayName() /*-{ 
      return this.display_name;
   }-*/;
   
   public final native String getConnectCode() /*-{
      return this.connect_code;
   }-*/;
   
   public final native JsArray<ConnectionObjectType> getObjectTypes() /*-{
      return this.object_types;
   }-*/;
   
   public final native JsArray<ConnectionAction> getActions() /*-{
      return this.actions;
   }-*/;
   
   public final native double getLastUsed() /*-{
      return this.last_used;
   }-*/;
   
   public final native String getIconData() /*-{
      return this.icon_data;
   }-*/;
   
   public final boolean isDataType(String dataType) 
   {
      if (dataType == null)
         return false;
      ConnectionObjectType type = getObjectType(dataType);
      if (type == null)
         return false;
      
      return type.isDataType();
   }
   
   public final ConnectionObjectType getObjectType(String objectType)
   {
      JsArray<ConnectionObjectType> types = getObjectTypes();
      for (int i = 0; i < types.length(); i++)
      {
         if (types.get(i).getName() == objectType)
            return types.get(i);
      }
      return null;
   }
}
