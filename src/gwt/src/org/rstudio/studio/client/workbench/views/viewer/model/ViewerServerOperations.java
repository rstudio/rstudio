/*
 * ViewerServerOperations.java
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
package org.rstudio.studio.client.workbench.views.viewer.model;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.exportplot.model.SavePlotAsImageContext;

public interface ViewerServerOperations
{
   void viewerStopped(ServerRequestCallback<Void> requestCallback);
   
   void viewerBack(ServerRequestCallback<Void> requestCallback);
   void viewerForward(ServerRequestCallback<Void> requestCallback);
   void viewerCurrent(ServerRequestCallback<Void> requestCallback);
   void viewerClearCurrent(ServerRequestCallback<Void> requestCallback);
   void viewerClearAll(ServerRequestCallback<Void> requestCallback);
   
   void getViewerExportContext(
         String directory,
         ServerRequestCallback<SavePlotAsImageContext> requestCallback);
   
   void viewerSaveAsWebPage(String targetPath,
                            ServerRequestCallback<Void> requestCallback);
   
   void viewerCreateRPubsHtml(String title, 
                              String comment,
                              ServerRequestCallback<String> requestCallback);
}
