/*
 * DataItem.java
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
package org.rstudio.studio.client.workbench.views.source.model;

import com.google.gwt.core.client.JavaScriptObject;
import org.rstudio.core.client.js.JsObject;

import java.util.HashMap;

public class DataItem extends JavaScriptObject
{
   public static String URI_PREFIX = "data://";
   
   protected DataItem()
   {
   }

   public static final native DataItem create(String caption,
                                              int totalObservations,
                                              int displayedObservations,
                                              int variables,
                                              String contentUrl) /*-{
      var dataItem = new Object();
      dataItem.caption = caption ;
      dataItem.totalObservations = totalObservations ;
      dataItem.displayedObservations = displayedObservations;
      dataItem.variables = variables;
      dataItem.contentUrl = contentUrl;
      return dataItem ;
   }-*/;

   public final String getURI()
   {
      return URI_PREFIX + getCaption();
   }
   
   public native final String getCaption() /*-{
      return this.caption;
   }-*/;
   
   public native final int getTotalObservations() /*-{
      // This will sometimes be a number, sometimes a string. Ugh
      return this.totalObservations - 0;
   }-*/;
   
   public native final int getDisplayedObservations() /*-{
      // This will sometimes be a number, sometimes a string. Ugh
      return this.displayedObservations - 0;
   }-*/;
   
   public native final int getVariables() /*-{
      // This will sometimes be a number, sometimes a string. Ugh
      return this.variables - 0;
   }-*/;
   
   public native final String getContentUrl() /*-{
      return this.contentUrl;
   }-*/;

   public final void fillProperties(HashMap<String, String> properties)
   {
      // This has the unfortunate side-effect of converting the numeric values
      // to strings. Can't be helped without refactoring
      // SourceServerOperations#modifyDocumentProperties to take a less typesafe
      // container type.
      properties.put("caption", getCaption());
      properties.put("totalObservations", getTotalObservations() + "");
      properties.put("displayedObservations", getDisplayedObservations() + "");
      properties.put("variables", getVariables() + "");
      properties.put("contentUrl", getContentUrl());
   }

   public final void fillProperties(JsObject properties)
   {
      properties.setString("caption", getCaption());
      properties.setInteger("totalObservations", getTotalObservations());
      properties.setInteger("displayedObservations", getDisplayedObservations());
      properties.setInteger("variables", getVariables());
      properties.setString("contentUrl", getContentUrl());
   }
}
