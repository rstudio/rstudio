/*
 * FixedRatioImage.java
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
package org.rstudio.core.client.widget;

import org.rstudio.core.client.dom.ImageElementEx;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

public class FixedRatioImage extends Composite 
{
   private static FixedRatioImageUiBinder uiBinder = GWT
         .create(FixedRatioImageUiBinder.class);

   interface FixedRatioImageUiBinder extends UiBinder<Widget, FixedRatioImage>
   {
   }

   // A fixed-ratio image automatically shrinks to fit its container while
   // preserving its aspect ratio.
   public FixedRatioImage(double height, double width)
   {
      initWidget(uiBinder.createAndBindUi(this));
      
      padding_.getElement().getStyle().setPaddingTop(
            (height / width) * 100.0, 
            Unit.PCT);
   }
   
   public void setUrl(final String url, final Command onLoaded)
   {
      DOM.sinkEvents(source_.getElement(), Event.ONLOAD);
      DOM.setEventListener(source_.getElement(), new EventListener()
      {
         @Override
         public void onBrowserEvent(Event event)
         {
            if (DOM.eventGetType(event) != Event.ONLOAD)
               return;

            ImageElementEx src = source_.getElement().cast();

            // scale the image to its natural size, but not beyond it
            outer_.getElement().getStyle().setProperty("maxWidth", 
                  src.naturalWidth() + "px");
            
            // load the image from the hidden source element into the background
            // of the wrapper
            image_.getElement().getStyle().setProperty("backgroundImage", 
                  "url(" + url + ")");
            if (onLoaded != null)
               onLoaded.execute();
         }
      });

      source_.setUrl(url);
   }
   
   @UiField Image source_;
   @UiField HTMLPanel padding_;
   @UiField HTMLPanel outer_;
   @UiField HTMLPanel image_;
}
