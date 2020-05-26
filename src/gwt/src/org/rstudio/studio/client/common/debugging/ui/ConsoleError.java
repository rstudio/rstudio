/*
 * ConsoleError.java
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

package org.rstudio.studio.client.common.debugging.ui;

import org.rstudio.core.client.VirtualConsole;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

public class ConsoleError extends Composite
{
   private static ConsoleErrorUiBinder uiBinder = GWT
         .create(ConsoleErrorUiBinder.class);

   interface ConsoleErrorUiBinder extends UiBinder<Widget, ConsoleError>
   {
   }
   
   public interface Observer
   {
      void onErrorBoxResize();
      void runCommandWithDebug(String command);
   }

   public ConsoleError(UnhandledError err, 
                       String errorClass, 
                       Observer observer, 
                       String command)
   {
      observer_ = observer;
      command_ = command;
      
      initWidget(uiBinder.createAndBindUi(this));
      
      VirtualConsole vc = RStudioGinjector.INSTANCE.getVirtualConsoleFactory().create(errorMessage.getElement());
      vc.submit(err.getErrorMessage().trim());
      errorMessage.addStyleName(errorClass);
      
      EventListener showHideTraceback = new EventListener()
      {
         @Override
         public void onBrowserEvent(Event event)
         {
            if (DOM.eventGetType(event) == Event.ONCLICK)
            {
               setTracebackVisible(!showingTraceback_);
               observer_.onErrorBoxResize();
            }
         }
      };
      
      EventListener rerunWithDebug = new EventListener()
      {
         @Override
         public void onBrowserEvent(Event event)
         {
            if (DOM.eventGetType(event) == Event.ONCLICK)
            {
               observer_.onErrorBoxResize();
               observer_.runCommandWithDebug(command_);
            }
         }
      };
      
      DOM.sinkEvents(showTracebackText.getElement(), Event.ONCLICK);
      DOM.setEventListener(showTracebackText.getElement(), showHideTraceback);
      DOM.sinkEvents(showTracebackImage.getElement(), Event.ONCLICK);
      DOM.setEventListener(showTracebackImage.getElement(), showHideTraceback);
      rerunText.setVisible(command_ != null);
      rerunImage.setVisible(command_ != null);
      DOM.sinkEvents(rerunText.getElement(), Event.ONCLICK);
      DOM.setEventListener(rerunText.getElement(), rerunWithDebug);
      DOM.sinkEvents(rerunImage.getElement(), Event.ONCLICK);
      DOM.setEventListener(rerunImage.getElement(), rerunWithDebug);
      
      for (int i = err.getErrorFrames().length() - 1; i >= 0; i--)
      {
         ConsoleErrorFrame frame = new ConsoleErrorFrame(i + 1, 
               err.getErrorFrames().get(i));
         framePanel.add(frame);
      }
   }
   
   public void setTracebackVisible(boolean visible)
   {
      showingTraceback_ = visible;
      showTracebackText.setText(showingTraceback_ ? 
            "Hide Traceback" : "Show Traceback");
      framePanel.setVisible(showingTraceback_);
   }

   @UiField Anchor showTracebackText;
   @UiField Image showTracebackImage;
   @UiField Anchor rerunText;
   @UiField Image rerunImage;
   @UiField HTMLPanel framePanel;
   @UiField HTML errorMessage;
   
   private Observer observer_;
   private boolean showingTraceback_ = false;
   private String command_;
}
