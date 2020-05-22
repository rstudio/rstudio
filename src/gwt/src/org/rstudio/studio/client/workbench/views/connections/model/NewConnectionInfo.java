/*
 * NewConnectionContext.java
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

package org.rstudio.studio.client.workbench.views.connections.model;


import com.google.gwt.core.client.JavaScriptObject;

public class NewConnectionInfo extends JavaScriptObject
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

   public final native String getSource() /*-{
      return this["source"];
   }-*/;

   public final native String getType() /*-{
      return this["type"];
   }-*/;

   public final native String getSubtype() /*-{
      return this["subtype"];
   }-*/;

   public final native String getHelp() /*-{
      return this["help"];
   }-*/;

   public final native String getSnippet() /*-{
      return this["snippet"];
   }-*/;

   public final native void setSnippet(String snippet) /*-{
      this["snippet"] = snippet;
   }-*/;

   public final native String iconData() /*-{
      return this["iconData"];
   }-*/;

   public final native boolean getLicensed() /*-{
      return this["licensed"];
   }-*/;

   public final native String getError() /*-{
      return this["error"];
   }-*/;

   public final native boolean getHasInstaller() /*-{
      return this["hasInstaller"];
   }-*/;

   public final native String getOdbcVersion() /*-{
      return this["odbcVersion"];
   }-*/;

   public final native String getOdbcLicense() /*-{
      return this["odbcLicense"];
   }-*/;

   public final native String getOdbcWarning() /*-{
      return this["odbcWarning"];
   }-*/;

   public final native String getOdbcInstallPath() /*-{
      return this["odbcInstallPath"];
   }-*/;

   public final native String getWarning() /*-{
      return this["warning"];
   }-*/;
}
