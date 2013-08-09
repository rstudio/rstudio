/*
 * ConsoleErrorFrame.java
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

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.common.debugging.model.ErrorFrame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class ConsoleErrorFrame extends Composite
{

   private static ConsoleErrorFrameUiBinder uiBinder = GWT
         .create(ConsoleErrorFrameUiBinder.class);

   interface ConsoleErrorFrameUiBinder extends
         UiBinder<Widget, ConsoleErrorFrame>
   {
   }

   public ConsoleErrorFrame(ErrorFrame frame, ConsoleError.Observer observer)
   {
      initWidget(uiBinder.createAndBindUi(this));
      
      frame_ = frame;
      observer_ = observer;
      
      boolean hasSource = !frame.getFileName().isEmpty();
      functionName.setText(frame.getFunctionName() + (hasSource ? " at" : ""));
      if (hasSource)
      {
         sourceLink.setText(
               FileSystemItem.getNameFromPath(frame.getFileName()) + 
               "#" + frame.getLineNumber());
         sourceLink.addClickHandler(new ClickHandler()
         {            
            @Override
            public void onClick(ClickEvent event)
            {
               if (frame_ != null)
               {
                  observer_.showSourceForFrame(frame_);
               }
            }
         });
      }
   }

   @UiField
   Label functionName;
   @UiField
   Anchor sourceLink;

   private ConsoleError.Observer observer_;
   private ErrorFrame frame_ = null;
}
