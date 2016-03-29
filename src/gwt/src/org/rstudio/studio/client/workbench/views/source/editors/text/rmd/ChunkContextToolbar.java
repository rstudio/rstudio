/*
 * ChunkContextToolbar.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.ExecuteChunksEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

public class ChunkContextToolbar extends Composite
{

   private static ChunkContextToolbarUiBinder uiBinder = GWT
         .create(ChunkContextToolbarUiBinder.class);

   interface ChunkContextToolbarUiBinder
         extends UiBinder<Widget, ChunkContextToolbar>
   {
   }
   
   public interface Resources extends ClientBundle
   {
      ImageResource runChunk();
      ImageResource runChunkPending();
      ImageResource runPreviousChunksLight();
      ImageResource runPreviousChunksDark();
      ImageResource chunkOptionsLight();
      ImageResource chunkOptionsDark();
   }
   
   public final static Resources RES = GWT.create(Resources.class);

   public ChunkContextToolbar(boolean dark, boolean runPrevious, boolean run)
   {
      initWidget(uiBinder.createAndBindUi(this));

      options_.setResource(dark ? RES.chunkOptionsDark() :
                                  RES.chunkOptionsLight());
      
      if (runPrevious)
         initRunPrevious(dark);
      else
         runPrevious_.setVisible(false);
      
      if (run)
         initRun();
      else
         run_.setVisible(false);
   }
   
   private void initRun()
   {
      DOM.sinkEvents(run_.getElement(), Event.ONCLICK);
      DOM.setEventListener(run_.getElement(), new EventListener()
      {
         @Override
         public void onBrowserEvent(Event event)
         {
            if (DOM.eventGetType(event) == Event.ONCLICK)
            {
               RStudioGinjector.INSTANCE.getGlobalDisplay().showMessage(
                     GlobalDisplay.MSG_INFO, "NYI", "Run (NYI)");
            }
         }
      });
   }
   
   private void initRunPrevious(boolean dark)
   {
      runPrevious_.setResource(dark ? RES.runPreviousChunksDark() :
                                      RES.runPreviousChunksLight());
      DOM.sinkEvents(runPrevious_.getElement(), Event.ONCLICK);
      DOM.setEventListener(runPrevious_.getElement(), new EventListener()
      {
         @Override
         public void onBrowserEvent(Event event)
         {
            if (DOM.eventGetType(event) == Event.ONCLICK)
            {
               RStudioGinjector.INSTANCE.getGlobalDisplay().showMessage(
                     GlobalDisplay.MSG_INFO, "NYI", "Run Previous (NYI)");
            }
         }
      });
   }

   @UiField Image options_;
   @UiField Image runPrevious_;
   @UiField Image run_;
   
   public final static String LINE_WIDGET_TYPE = "ChunkToolbar";
}
