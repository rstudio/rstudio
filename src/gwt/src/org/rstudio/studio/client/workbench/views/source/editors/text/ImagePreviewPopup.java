/*
 * ImagePreviewPopup.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.dom.ImageElementEx;
import org.rstudio.core.client.widget.MiniPopupPanel;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.AnchoredSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

public class ImagePreviewPopup extends MiniPopupPanel
{
   public ImagePreviewPopup(DocDisplay display, Range range, String href, 
         String src)
   {
      super(true, false);
      
      // defer visibility until image has finished loading
      setVisible(false);
      image_ = new Image(src);
      error_ = new Label("No image at path " + href);
      
      final Element imgEl = image_.getElement();
      DOM.sinkEvents(imgEl, Event.ONLOAD | Event.ONERROR);
      DOM.setEventListener(imgEl, new EventListener()
      {
         @Override
         public void onBrowserEvent(Event event)
         {
            if (DOM.eventGetType(event) == Event.ONLOAD)
            {
               setWidget(image_);
               showSmall();
            }
            else
            {
               setWidget(error_);
               setVisible(true);
            }
         }
      });
      
      // allow zoom with double-click
      setTitle("Double-Click to Zoom");
      addDomHandler(new DoubleClickHandler()
      {
         @Override
         public void onDoubleClick(DoubleClickEvent event)
         {
            toggleSize();
         }
      }, DoubleClickEvent.getType());
      
      // use anchor + cursor changed handler for smart auto-dismiss
      anchor_ = display.createAnchoredSelection(
            range.getStart(),
            range.getEnd());
      
      handler_ = display.addCursorChangedHandler(new CursorChangedHandler()
      {
         @Override
         public void onCursorChanged(CursorChangedEvent event)
         {
            Position position = event.getPosition();
            if (!anchor_.getRange().contains(position))
               hide();
         }
      });
      
      addAttachHandler(new AttachEvent.Handler()
      {
         @Override
         public void onAttachOrDetach(AttachEvent event)
         {
            if (!event.isAttached())
               detachHandlers();
         }
      });
   }
   
   private boolean isSmall()
   {
      ImageElementEx el = image_.getElement().cast();
      Style style = el.getStyle();
      return
            SMALL_MAX_WIDTH.equals(style.getProperty("maxWidth")) ||
            SMALL_MAX_HEIGHT.equals(style.getProperty("maxHeight"));
   }
   
   private void showSmall()
   {
      showWithDimensions(SMALL_MAX_WIDTH, SMALL_MAX_HEIGHT);
   }
   
   private void showLarge()
   {
      showWithDimensions(LARGE_MAX_WIDTH, LARGE_MAX_HEIGHT);
   }
   
   private void showWithDimensions(String width, String height)
   {
      setVisible(true);
      
      ImageElementEx el = image_.getElement().cast();
      Style style = el.getStyle();
      
      boolean isWide = el.naturalWidth() > el.naturalHeight();
      if (isWide)
         style.setProperty("maxWidth", width);
      else
         style.setProperty("maxHeight", height);
   }
   
   private void toggleSize()
   {
      if (isSmall())
         showLarge();
      else
         showSmall();
   }
   
   private void detachHandlers()
   {
      anchor_.detach();
      handler_.removeHandler();
   }
   
   private final AnchoredSelection anchor_;
   private final HandlerRegistration handler_;
   private final Image image_;
   private final Label error_;
   
   private static final String SMALL_MAX_WIDTH  = "100px";
   private static final String SMALL_MAX_HEIGHT = "100px";
   
   private static final String LARGE_MAX_WIDTH  = "400px";
   private static final String LARGE_MAX_HEIGHT = "600px";
}

