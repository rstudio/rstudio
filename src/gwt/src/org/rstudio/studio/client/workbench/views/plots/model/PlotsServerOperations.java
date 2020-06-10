/*
 * PlotsServerOperations.java
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
package org.rstudio.studio.client.workbench.views.plots.model;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.server.Bool;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.exportplot.model.SavePlotAsImageContext;

import com.google.gwt.json.client.JSONObject;


public interface PlotsServerOperations
{
   String getGraphicsUrl(String filename);
 
   String getFileUrl(FileSystemItem file);
   
   String getPlotExportUrl(String type, 
                           int width, 
                           int height, 
                           boolean attachment);
   
   void nextPlot(ServerRequestCallback<Void> requestCallback);
   void previousPlot(ServerRequestCallback<Void> requestCallback);
   
   void removePlot(ServerRequestCallback<Void> requestCallback);
   
   void clearPlots(ServerRequestCallback<Void> requestCallback);
      
   void refreshPlot(ServerRequestCallback<Void> requestCallback);
   
   void setManipulatorValues(JSONObject values,
                             ServerRequestCallback<Void> requestCallback);
   
   void manipulatorPlotClicked(int x, 
                               int y, 
                               ServerRequestCallback<Void> requestCallback);
   
   void locatorCompleted(Point point, 
                        ServerRequestCallback<Void> requestCallback);

   void getUniqueSavePlotStem(String directory,
                              ServerRequestCallback<String> requestCallback);
   
   void getSavePlotContext(
                  String directory,
                  ServerRequestCallback<SavePlotAsImageContext> requestCallback);
   
   void savePlotAs(FileSystemItem file,
                   String format,
                   int width,
                   int height,
                   boolean overwrite,
                   ServerRequestCallback<Bool> requestCallback);
   
   void savePlotAsPdf(FileSystemItem file,
                      double widthInches,
                      double heightInches,
                      boolean useCairoPdf,
                      boolean overwrite,
                      ServerRequestCallback<Bool> requestCallback);
   
   
   void copyPlotToClipboardMetafile(
                                 int width, 
                                 int height,
                                 ServerRequestCallback<Void> requestCallback);
   
   void copyPlotToCocoaPasteboard(int width, 
                                  int height,
                                  ServerRequestCallback<Void> requestCallback);
   
   void plotsCreateRPubsHtml(String title, 
                             String comment,
                             int width,
                             int height,
                             ServerRequestCallback<String> requestCallback);
}
