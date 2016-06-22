/*
 * NewSparkConnectionContext.java
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


import org.rstudio.core.client.StringUtil;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public class NewSparkConnectionContext extends JavaScriptObject
{
   protected NewSparkConnectionContext()
   {
   }
   
   public final native JsArrayString getRemoteServers() /*-{
      return this.remote_servers;
   }-*/;
   
   public final native String getSparkHome() /*-{
      return this.spark_home;
   }-*/;
   
   public final native JsArray<SparkVersion> getSparkVersions() /*-{
      return this.spark_versions;
   }-*/;
   
   public final boolean getLocalConnectionsSupported()
   {
      return getSparkVersions().length() > 0;
   }
   
   public final boolean getClusterConnectionsSupported()
   {
      return !StringUtil.isNullOrEmpty(getSparkHome());
   }
   
   public final native boolean getCanInstallSparkVersions() /*-{
      return this.can_install_spark_versions;
   }-*/; 
   
   public final native boolean isJavaInstalled() /*-{
      return this.java_installed;
   }-*/;
   
   public final native String getJavaInstallUrl() /*-{
      return this.java_install_url;
   }-*/;
}