/*
 * HTMLPreview.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.htmlpreview;

import org.rstudio.core.client.Size;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.htmlpreview.events.ShowHTMLPreviewEvent;
import org.rstudio.studio.client.htmlpreview.events.ShowHTMLPreviewHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class HTMLPreview
{
   @Inject
   public HTMLPreview(EventBus eventBus, 
                      final SatelliteManager satelliteManager)
   {
      eventBus.addHandler(ShowHTMLPreviewEvent.TYPE, 
                          new ShowHTMLPreviewHandler() {
         @Override
         public void onShowHTMLPreview(ShowHTMLPreviewEvent event)
         {
            // open the window 
            satelliteManager.openSatellite(HTMLPreviewApplication.NAME,     
                                            event.getParams(),
                                            new Size(950,1200));   
            
         }  
      });
   }

}
