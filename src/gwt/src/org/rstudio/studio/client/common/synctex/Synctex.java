/*
 * Synctex.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.common.synctex;

import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.synctex.model.PdfLocation;
import org.rstudio.studio.client.common.synctex.model.SourceLocation;
import org.rstudio.studio.client.pdfviewer.PDFViewerApplication;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class Synctex
{
   @Inject
   public Synctex(GlobalDisplay globalDisplay,
                  Satellite satellite, 
                  SatelliteManager satelliteManager)
   {
      globalDisplay_ = globalDisplay;
      satellite_ = satellite;
      satelliteManager_ = satelliteManager;
      
      // main window and satellite export callbacks to eachother
      if (!satellite.isCurrentWindowSatellite())
         registerMainWindowCallbacks();
      else if (satellite.getSatelliteName().equals(PDFViewerApplication.NAME))
         registerSatelliteCallbacks();
   }

   public boolean forwardSearch(SourceLocation sourceLocation)
   {
      // return false if there is no pdf viewer
      WindowEx window = satelliteManager_.getSatelliteWindowObject(
                                                   PDFViewerApplication.NAME);
      if (window == null)
         return false;
      
      // activate the satellite
      satelliteManager_.activateSatelliteWindow(PDFViewerApplication.NAME);
         
      // execute the forward search
      callForwardSearch(window, sourceLocation);
  
      return true;
   }
   
   public void inverseSearch(PdfLocation pdfLocation)
   {
      satellite_.focusMainWindow();
      callInverseSearch(pdfLocation);
   }
   
   
   private void doForwardSearch(JavaScriptObject sourceLocationObject)
   {
      //SourceLocation sourceLocation = sourceLocationObject.cast();
      
      globalDisplay_.showErrorMessage("Synctex", "Forward Search");
      
      // TODO: fire an event which is handled by PDFViewerPresenter
   }
   
   private void doInverseSearch(JavaScriptObject pdfLocationObject)
   {
      //PdfLocation pdfLocation = pdfLocationObject.cast();
      
      globalDisplay_.showErrorMessage("Synctex", "Invsere Search");
      
      
      // TODO: fire an event which is handled by Source
      
      
   }   

   private native void registerMainWindowCallbacks() /*-{
      var synctex = this;     
      $wnd.synctexInverseSearch = $entry(
         function(pdfLocation) {
            synctex.@org.rstudio.studio.client.common.synctex.Synctex::doInverseSearch(Lcom/google/gwt/core/client/JavaScriptObject;)(pdfLocation);
         }
      ); 
   }-*/;
   
   private final native void callInverseSearch(JavaScriptObject pdfLocation)/*-{
      $wnd.opener.synctexInverseSearch(pdfLocation);
   }-*/;
   
   private native void registerSatelliteCallbacks() /*-{
      var synctex = this;     
      $wnd.synctexForwardSearch = $entry(
         function(sourceLocation) {
            synctex.@org.rstudio.studio.client.common.synctex.Synctex::doForwardSearch(Lcom/google/gwt/core/client/JavaScriptObject;)(sourceLocation);
         }
      ); 
   }-*/;
   
   private native void callForwardSearch(JavaScriptObject satellite,
                                         JavaScriptObject sourceLocation) /*-{
      satellite.synctexForwardSearch(sourceLocation);
   }-*/;
   
   private final GlobalDisplay globalDisplay_;
   private final Satellite satellite_;
   private final SatelliteManager satelliteManager_;
}
