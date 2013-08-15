/*
 * ConsoleError.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import org.rstudio.studio.client.common.debugging.model.ErrorFrame;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
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
      void showSourceForFrame(ErrorFrame frame);
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
   
      errorMessage.setText(err.getErrorMessage().trim());
      errorMessage.addStyleName(errorClass);
      
      ClickHandler showHideTraceback = new ClickHandler()
      {         
         @Override
         public void onClick(ClickEvent event)
         {
            setTracebackVisible(!showingTraceback_);
            observer_.onErrorBoxResize();
         }
      };
      
      ClickHandler rerunWithDebug = new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            observer_.runCommandWithDebug(command_);
         }
      };
      
      showTracebackText.addClickHandler(showHideTraceback);
      showTracebackImage.addClickHandler(showHideTraceback);
      rerunText.addClickHandler(rerunWithDebug);
      rerunImage.addClickHandler(rerunWithDebug);
      
      for (int i = err.getErrorFrames().length() - 1; i >= 0; i--)
      {
         ConsoleErrorFrame frame = new ConsoleErrorFrame(
               err.getErrorFrames().get(i), observer_);
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
   @UiField Label errorMessage;
   
   private Observer observer_;
   private boolean showingTraceback_ = false;
   private String command_;
}
