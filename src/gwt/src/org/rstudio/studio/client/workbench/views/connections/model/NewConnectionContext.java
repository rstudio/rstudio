/*
 * NewConnectionContext.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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


import java.util.ArrayList;

import com.google.gwt.core.client.JavaScriptObject;

public class NewConnectionContext extends JavaScriptObject
{
   protected NewConnectionContext()
   {
   }

   public static class NewConnectionInfo extends JavaScriptObject
   {
      protected NewConnectionInfo()
      {
      }

      public final native String getPackage() /*-{
         return this["package"];
      }-*/;

      public final native String getVersion() /*-{
         return this["version"];
      }-*/;

      public final native String getName() /*-{
         return this["name"];
      }-*/;

      public final native String getType() /*-{
         return this["type"];
      }-*/;

      public final native String getHelp() /*-{
         return this["help"];
      }-*/;

      public final native String getSnippet() /*-{
         return this["snippet"];
      }-*/;

      public final native String iconData() /*-{
         return this["iconData"];
      }-*/;

      public final native boolean getLicensed() /*-{
         return this["licensed"];
      }-*/;
   }

   public final native int getConnectionsLength() /*-{
      return this.connectionsList.length;
   }-*/;
   
   public final native NewConnectionInfo getConnectionsItem(int index) /*-{
      return this.connectionsList[index];
   }-*/;

   public final ArrayList<NewConnectionInfo> getConnectionsList()
   {
      ArrayList<NewConnectionInfo> result = new ArrayList<NewConnectionInfo>(getConnectionsLength());
      for (int i = 0; i < getConnectionsLength(); i++)
         result.add(getConnectionsItem(i));

      return result;
   }
}