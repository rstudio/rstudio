/*
 * PDFViewerApplication.java
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

import org.rstudio.studio.client.application.ApplicationUncaughtExceptionHandler;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteApplication;
import org.rstudio.studio.client.pdfviewer.events.InitCompleteEvent;
import org.rstudio.studio.client.pdfviewer.ui.PDFViewerApplicationView;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceThemes;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class PDFViewerApplication extends SatelliteApplication
{
   public static final String NAME = "pdf";
   
   @Inject
   public PDFViewerApplication(
                        PDFViewerApplicationView view,
                        Satellite satellite,
                        Provider<AceThemes> pAceThemes,
                        ApplicationUncaughtExceptionHandler uncaughtExHandler)
   {
      super(NAME, view, satellite, pAceThemes, uncaughtExHandler);
      view.addInitCompleteHandler(new InitCompleteEvent.Handler() {
         @Override
         public void onInitComplete(InitCompleteEvent event)
         {
            flushPendingEvents();
         }
      });
   }

   @Override
   protected boolean manuallyFlushPendingEvents()
   {
      return true;
   }
}
