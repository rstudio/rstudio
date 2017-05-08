/*
 * MiddleMousePasteListener.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.core.client;

import org.rstudio.studio.client.application.Desktop;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.inject.Singleton;

@Singleton
public class MiddleMousePasteListener
{
   public MiddleMousePasteListener()
   {
      Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         @Override
         public void onPreviewNativeEvent(NativePreviewEvent preview)
         {
            switch (preview.getTypeInt())
            {
            case Event.ONMOUSEDOWN:
               onMouseDown(preview);
               break;
            case Event.ONMOUSEUP:
               onMouseUp(preview);
               break;
            case Event.ONMOUSEMOVE:
               // ignore mousemove events
               break;
            default:
               onDefault();
               break;
            }
         }
      });
   }
   
   private boolean isMiddleClick(NativePreviewEvent preview)
   {
      return preview.getNativeEvent().getButton() == Event.BUTTON_MIDDLE;
   }
   
   private void onMouseDown(NativePreviewEvent preview)
   {
      lastEventWasMiddleMouseDown_ = isMiddleClick(preview);
   }
   
   private void onMouseUp(NativePreviewEvent preview)
   {
      if (!lastEventWasMiddleMouseDown_ || !isMiddleClick(preview))
         return;
      
      NativeEvent event = preview.getNativeEvent();
      event.stopPropagation();
      event.preventDefault();
      
      if (Desktop.isDesktop())
         onPasteDesktop();
      else
         onPasteWeb();
   }
   
   private void onDefault()
   {
      lastEventWasMiddleMouseDown_ = false;
   }
   
   private void onPasteDesktop()
   {
      Desktop.getFrame().clipboardPaste();
   }
   
   private native void onPasteWeb()
   /*-{
      $doc.execCommand("paste");
   }-*/;
   
   private boolean lastEventWasMiddleMouseDown_;
}
