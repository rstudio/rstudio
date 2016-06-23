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


import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.application.Desktop;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public class NewSparkConnectionContext extends JavaScriptObject
{
   protected NewSparkConnectionContext()
   {
   }
  
   
   public final native String getSparkHome() /*-{
      return this.spark_home;
   }-*/;
   
   public final native JsArray<SparkVersion> getSparkVersions() /*-{
      return this.spark_versions;
   }-*/;
   
   public final native String getDefaultSparkVersion() /*-{
      return this.spark_default;
   }-*/;
   
   public final native String getDefaultHadoopVersion() /*-{
      return this.hadoop_default;
   }-*/;
   
   public final boolean getLocalConnectionsSupported()
   {
      return hasConnectionsOption(MASTER_LOCAL) && 
             (getSparkVersions().length() > 0);
   }
   
   public final boolean getClusterConnectionsEnabled()
   {
      return hasConnectionsOption(MASTER_CLUSTER);
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
   
   public final List<String> getClusterServers() 
   {
      // start with primed servers
      ArrayList<String> servers = getDefaultClusterConnections();
      
      // add mru as necessary
      JsArrayString mruServers = getRemoteServers();
      for (int i = 0; i<mruServers.length(); i++)
         if (!servers.contains(mruServers.get(i)))
            servers.add(mruServers.get(i));
      
      return servers;
   }
   
   public final native String getDefaultClusterUrl() /*-{
      return this.default_cluster_url;
   }-*/;
   
   private final native JsArrayString getRemoteServers() /*-{
      return this.remote_servers;
   }-*/;
   
   private final List<String> getConnectionsOption() 
   {
      ArrayList<String> connectionsOption = new ArrayList<String>();
      if (Desktop.isDesktop())
      {
         connectionsOption.add(MASTER_LOCAL);
         connectionsOption.add(MASTER_CLUSTER);
      }
      else
      {
         JsArrayString connectionsNative = getConnectionsOptionNative();
         for (int i = 0; i<connectionsNative.length(); i++)
            connectionsOption.add(connectionsNative.get(i));
      }
      
      return connectionsOption;
   }
   
   private final native JsArrayString getConnectionsOptionNative() /*-{
      return this.connections_option;
   }-*/; 

   private final boolean hasConnectionsOption(String option)
   {
      List<String> connectionsOption = getConnectionsOption();
      for (int i = 0; i<connectionsOption.size(); i++)
         if (connectionsOption.get(i).equals(option))
            return true;
      return false;
   }
   
   private final ArrayList<String> getDefaultClusterConnections()
   {
      ArrayList<String> connections = new ArrayList<String>();
      List<String> connectionsOption = getConnectionsOption();
      for (int i = 0; i<connectionsOption.size(); i++)
      {
         String option = connectionsOption.get(i);
         if (!MASTER_LOCAL.equals(option) && !MASTER_CLUSTER.equals(option))
         {
            if (!option.startsWith("spark://"))
               option = "spark://" + option;
            connections.add(option);
         }
      }
      return connections;
   }
   
   public static String MASTER_LOCAL = "local";
   public static String MASTER_CLUSTER = "cluster";
}