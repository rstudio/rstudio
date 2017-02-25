
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
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteErrorEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteErrorHandler;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteOutputEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteOutputHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.AnchoredSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.PopupPanel;

public class ChunkInlineOutput extends MiniPopupPanel
                               implements ConsoleWriteOutputHandler,
                                          ConsoleWriteErrorHandler
{
   public enum State
   {
      Queued,
      Started,
      Finished
   }

   public ChunkInlineOutput(String chunkId, final AnchoredSelection selection) 
   {
      super(true, false, true);
      
      console_ = new PreWidget();
      vconsole_ = new VirtualConsole(console_.getElement());
      chunkId_ = chunkId;
      selection_ = selection;
      state_ = State.Queued;
      
      addStyleName(RES.styles().panel());
      
      // detach anchored selection when closing so we don't accumulate 
      // unused anchors in the document
      addCloseHandler(new CloseHandler<PopupPanel>()
      {
         @Override
         public void onClose(CloseEvent<PopupPanel> arg0)
         {
            selection.detach();
         }
      });
      
      setWidget(console_);
   }
   
   public String chunkId()
   {
      return chunkId_;
   }
   
   public Range range()
   {
      return selection_.getRange();
   }
   
   public State state()
   {
      return state_;
   }
   
   public void setState(State state)
   {
      state_ = state;
   }
   
   @Override
   public void onConsoleWriteOutput(ConsoleWriteOutputEvent event)
   {
      vconsole_.submit(event.getOutput(), RES.styles().output());
   }

   @Override
   public void onConsoleWriteError(ConsoleWriteErrorEvent event)
   {
      vconsole_.submit(event.getError(), RES.styles().error());
   }

   public interface Styles extends CssResource
   {
      String panel();
      String output();
      String error();
   }
   
   public interface Resources extends ClientBundle
   {
      @Source("ChunkInlineOutput.css")
      Styles styles();
   }
   
   private static Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }

   private final VirtualConsole vconsole_;
   private final PreWidget console_;
   private final String chunkId_;
   private final AnchoredSelection selection_;
   private State state_;
}
