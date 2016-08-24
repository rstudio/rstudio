/*
 * ChunkSatelliteWindow.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.SatelliteWindow;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOptions;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputHost;

@Singleton
public class ChunkSatelliteWindow extends SatelliteWindow
                                  implements ChunkSatelliteView
{

   @Inject
   public ChunkSatelliteWindow(Provider<EventBus> pEventBus,
                               Provider<FontSizeManager> pFSManager)
   {
      super(pEventBus, pFSManager);
   }

   @Override
   protected void onInitialize(LayoutPanel mainPanel, JavaScriptObject params)
   {
      chunkWindowParams_ = params.cast();

      String title = "RStudio Chunk Window";
      Window.setTitle(title);

      ChunkOutputHost chunkOutputHost = new ChunkOutputHost()
      {
         @Override
         public void onOutputRemoved(final ChunkOutputWidget widget)
         {
         }
         
         @Override
         public void onOutputHeightChanged(ChunkOutputWidget widget,
                                           int height,
                                           boolean ensureVisible)
         {
         }
      };
      
      chunkOutputWidget_ = new ChunkOutputWidget(
         chunkWindowParams_.getDocId(),
         chunkWindowParams_.getChunkId(),
         RmdChunkOptions.create(),
         ChunkOutputWidget.EXPANDED,
         chunkOutputHost);
      
      mainPanel.add(chunkOutputWidget_);
      mainPanel.setWidgetLeftRight(chunkOutputWidget_, 0, Unit.PX, 0, Unit.PX);
      mainPanel.setWidgetTopBottom(chunkOutputWidget_, 0, Unit.PX, 0, Unit.PX);
   }

   @Override
   public void reactivate(JavaScriptObject params)
   {
   }
   
   @Override 
   public Widget getWidget()
   {
      return this;
   }

   private ChunkOutputWidget chunkOutputWidget_;
   private ChunkWindowParams chunkWindowParams_;
}