
/*
 * ChunkInlineOutput.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.VirtualConsole;
import org.rstudio.core.client.widget.MiniPopupPanel;
import org.rstudio.core.client.widget.PreWidget;
import org.rstudio.studio.client.rmarkdown.events.ChunkExecStateChangedEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteErrorEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteErrorHandler;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteOutputEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteOutputHandler;

public class ChunkInlineOutput extends MiniPopupPanel
                               implements ConsoleWriteOutputHandler,
                                          ConsoleWriteErrorHandler,
                                          ChunkExecStateChangedEvent.Handler
{
   public ChunkInlineOutput(String chunkId) 
   {
      super(true, false, true);
      
      vconsole_ = new VirtualConsole();
      console_ = new PreWidget();
      chunkId_ = chunkId;
      
      setWidget(console_);
   }
   
   public String chunkId()
   {
      return chunkId_;
   }
   
   @Override
   public void onChunkExecStateChanged(ChunkExecStateChangedEvent event)
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void onConsoleWriteError(ConsoleWriteErrorEvent event)
   {
      vconsole_.submitAndRender(event.getError(), "black", console_.getElement());
   }

   @Override
   public void onConsoleWriteOutput(ConsoleWriteOutputEvent event)
   {
      vconsole_.submitAndRender(event.getOutput(), "black", console_.getElement());
   }

   private final VirtualConsole vconsole_;
   private final PreWidget console_;
   private final String chunkId_;
}
