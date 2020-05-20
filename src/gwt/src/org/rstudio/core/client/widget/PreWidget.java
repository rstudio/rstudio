/*
 * PreWidget.java
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
package org.rstudio.core.client.widget;

import java.util.ArrayList;

import org.rstudio.studio.client.workbench.views.source.editors.text.events.PasteEvent;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

public class PreWidget extends Widget implements HasKeyDownHandlers,
                                                 HasClickHandlers
{
   public PreWidget()
   {
      setElement(Document.get().createPreElement());
      getElement().setTabIndex(0);
   }

   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return addDomHandler(handler, ClickEvent.getType());
   }

   public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
   {
      return addDomHandler(handler, KeyDownEvent.getType());
   }
   
   public HandlerRegistration addPasteHandler(final PasteEvent.Handler handler)
   {
      // GWT doesn't understand paste events via BrowserEvents.Paste, so we need
      // to manually sink and register for the paste event.
      sinkEvents(Event.ONPASTE);
      pasteHandlers_.add(handler);
      return new HandlerRegistration()
      {
         @Override
         public void removeHandler()
         {
            pasteHandlers_.remove(handler);
         }
      };
   }
   
   public void setText(String text)
   {
      getElement().setInnerText(text);
   }

   public void appendText(String text)
   {
      getElement().setInnerText(getElement().getInnerText() + text);
   }
   
   @Override
   public void onBrowserEvent(Event event)
   {
      super.onBrowserEvent(event);
      if (event.getTypeInt() == Event.ONPASTE)
      {
         for (PasteEvent.Handler handler: pasteHandlers_)
         {
            handler.onPaste(new PasteEvent(getClipboardText(event)));
         }
      }
   }
   
   private final native String getClipboardText(Event event) /*-{
      return event.clipboardData.getData('text/plain');
   }-*/;
   
   private ArrayList<PasteEvent.Handler> pasteHandlers_ = 
         new ArrayList<PasteEvent.Handler>();
}
