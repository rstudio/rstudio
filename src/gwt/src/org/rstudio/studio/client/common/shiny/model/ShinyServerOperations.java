/*
 * ShinyServerOperations.java
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
package org.rstudio.studio.client.common.shiny.model;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.shiny.model.ShinyRunCmd;

public interface ShinyServerOperations
{
   void getShinyCapabilities(
               ServerRequestCallback<ShinyCapabilities> requestCallback);

   void getShinyViewerType(
               ServerRequestCallback<String> requestCallback);
   
   void setShinyViewerType(
               String viewerType, 
               ServerRequestCallback<Void> requestCallback);
   
   void getShinyRunCmd(
               String shinyFile,
               String extendedType,
               ServerRequestCallback<ShinyRunCmd> requestCallback);

   void runShinyBackgroundApp(
               String shinyFile,
               String extendedType,
               ServerRequestCallback<String> requestCallback);
   
   void stopShinyApp(
               String id,
               ServerRequestCallback<Void> requestCallback);
}
