/*
 * Shelf.java
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
package org.rstudio.studio.client.workbench.views.history.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

public class Shelf extends Composite
{
   interface Binder extends UiBinder<HorizontalPanel, Shelf> {}
   interface Resources extends ClientBundle
   {
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource shelfbg();

      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource shelfbgLarge();
      
      @Source("Shelf.css")
      Styles styles();
   }

   interface Styles extends CssResource
   {
      String shelf();
      String largeShelf();
      String left();
      String right();
   }

   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }

   public Shelf()
   {
      this(false);
   }
   
   public Shelf(boolean large)
   {
      large_ = large;
      HorizontalPanel mainPanel = binder.createAndBindUi(this);
      if (large_)
         mainPanel.setStyleName(RES.styles().largeShelf());
  
      initWidget(mainPanel);
      

      left_.setHeight("100%");
      right_.setHeight("100%");

      left_.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
      right_.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);

      mainPanel.setCellHorizontalAlignment(right_,
                                           HorizontalPanel.ALIGN_RIGHT);
   }

   public void addLeftWidget(Widget w)
   {
      left_.add(w);
   }

   public void addRightWidget(Widget w)
   {
      right_.add(w);
   }
   
   public void setRightVerticalAlignment(VerticalAlignmentConstant alignment)
   {
      right_.setVerticalAlignment(alignment);
   }

   public int getHeight()
   {

      if (large_)
         return RES.shelfbgLarge().getHeight();
      else
         return RES.shelfbg().getHeight();
   }

   public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
   {
      return addDomHandler(handler, KeyDownEvent.getType());
   }

   private static final Binder binder = GWT.create(Binder.class);

   @UiField
   HorizontalPanel left_;
   @UiField
   HorizontalPanel right_;
   
   private boolean large_ = false;
   
   private static final Resources RES = (Resources)GWT.create(Resources.class);
}
