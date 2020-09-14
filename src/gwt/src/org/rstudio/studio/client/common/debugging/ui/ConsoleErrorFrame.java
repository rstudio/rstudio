/*
 * ConsoleErrorFrame.java
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

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.debugging.model.ErrorFrame;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent;
import org.rstudio.studio.client.common.filetypes.model.NavigationMethods;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
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

   public ConsoleErrorFrame(int number, ErrorFrame frame)
   {
      initWidget(uiBinder.createAndBindUi(this));

      frame_ = frame;

      boolean hasSource = !frame.getFileName().isEmpty();
      functionName.setText(frame.getFunctionName() + (hasSource ? " at" : ""));
      frameNumber.setText((Integer.valueOf(number)).toString() + ".");
      if (hasSource)
      {
         sourceLink.setText(
               FileSystemItem.getNameFromPath(frame.getFileName()) +
               "#" + frame.getLineNumber());
         DOM.sinkEvents(sourceLink.getElement(), Event.ONCLICK);
         DOM.setEventListener(sourceLink.getElement(), new EventListener()
         {
            @Override
            public void onBrowserEvent(Event event)
            {
               if (DOM.eventGetType(event) == Event.ONCLICK &&
                   frame_ != null)
               {
                  showSourceForFrame(frame_);
               }

            }
         });
      }
   }

   private void showSourceForFrame(ErrorFrame frame)
   {
      FileSystemItem sourceFile = FileSystemItem.createFile(
            frame.getFileName());
      RStudioGinjector.INSTANCE.getEventBus().fireEvent(
            new OpenSourceFileEvent(sourceFile,
                             FilePosition.create(
                                   frame.getLineNumber(),
                                   frame.getCharacterNumber()),
                             FileTypeRegistry.R,
                             NavigationMethods.HIGHLIGHT_LINE));
   }

   @UiField
   Label functionName;
   @UiField
   Anchor sourceLink;
   @UiField
   Label frameNumber;

   private ErrorFrame frame_ = null;
}
