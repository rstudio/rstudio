/*
 * PDFViewer.java
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
package org.rstudio.studio.client.pdfviewer;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.NativeScreen;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.pdfviewer.events.ShowPDFViewerEvent;
import org.rstudio.studio.client.pdfviewer.events.ShowPDFViewerHandler;
import org.rstudio.studio.client.pdfviewer.model.PDFViewerParams;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PDFViewer 
{
   @Inject
   public PDFViewer(EventBus eventBus,
                    final SatelliteManager satelliteManager)
   {  
      eventBus.addHandler(ShowPDFViewerEvent.TYPE, 
                          new ShowPDFViewerHandler() 
      {
         @Override
         public void onShowPDFViewer(ShowPDFViewerEvent event)
         {
            // setup params
            PDFViewerParams params = PDFViewerParams.create();
                         
            // open the window 
            satelliteManager.openSatellite(PDFViewerApplication.NAME,     
                                            params,
                                            getPreferredWindowSize());   
         }
      });
   }

   
   private Size getPreferredWindowSize()
   {
      // compute available height (trim to 1200)
      NativeScreen screen = NativeScreen.get();
      int height = Math.min(screen.getAvailHeight(), 1200);
      
      // trim height as needed
      if (screen.getAvailHeight() >= 1100)
         height = height - 96;
      else if (BrowseCap.isLinux() && BrowseCap.isChrome())
         height = height - 100;
      else
         height = height - 5;
      
      // width always wants to be 1070 if it can be (assumes 200px sidebar)
      int width = Math.min(1070, screen.getAvailWidth());
        
      // return size
      return new Size(width, height);
   }
}
