/*
 * ShinyAppsServerOperations.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.common.shiny.model;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.shiny.model.ShinyAppsApplicationInfo;
import org.rstudio.studio.client.shiny.model.ShinyAppsDeploymentFiles;
import org.rstudio.studio.client.shiny.model.ShinyAppsDeploymentRecord;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public interface ShinyAppsServerOperations
{
   void removeShinyAppsAccount(String accountName, 
               ServerRequestCallback<Void> requestCallback);

   void getShinyAppsAccountList(
               ServerRequestCallback<JsArrayString> requestCallback);

   void connectShinyAppsAccount(String command, 
               ServerRequestCallback<Void> requestCallback);

   void getShinyAppsAppList(String accountName,
               ServerRequestCallback<JsArray<ShinyAppsApplicationInfo>> requestCallback);
   
   void getShinyAppsDeployments(String dir, 
               ServerRequestCallback<JsArray<ShinyAppsDeploymentRecord>> requestCallback); 
   
   void getDeploymentFiles (String dir, 
               ServerRequestCallback<ShinyAppsDeploymentFiles> requestCallback);
   
   void deployShinyApp(String dir, String file, String account, String appName, 
               ServerRequestCallback<Boolean> requestCallback);
}
