/*
 * WarningBar.java
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
package org.rstudio.studio.client.application.ui;

import org.rstudio.core.client.theme.res.ThemeResources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

public class WarningBar extends Composite
      implements HasCloseHandlers<WarningBar>
{
   interface Resources extends ClientBundle
   {
      @Source("WarningBar.css")
      Styles styles();

      ImageResource warningBarLeft();
      ImageResource warningBarRight();
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource warningBarTile();

      @Source("warningIconSmall_2x.png")
      ImageResource warningIconSmall2x(); 
   }

   interface Styles extends CssResource
   {
      String warningBar();
      String left();
      String right();
      String center();
      String warningIcon();
      String label();
      String dismiss();

      String warning();
      String error();
   }

   interface Binder extends UiBinder<Widget, WarningBar>{}
   static final Binder binder = GWT.create(Binder.class);

   public WarningBar()
   {
      initWidget(binder.createAndBindUi(this));
      dismiss_.addStyleName(ThemeResources.INSTANCE.themeStyles().handCursor());
      dismiss_.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            CloseEvent.fire(WarningBar.this, WarningBar.this);
         }
      });
   }

   public void setText(String value)
   {
      label_.setInnerText(value);
   }

   public void setSeverity(boolean severe)
   {
      if (severe)
      {
         addStyleName(styles_.error());
         removeStyleName(styles_.warning());
      }
      else
      {
         addStyleName(styles_.warning());
         removeStyleName(styles_.error());
      }
   }

   public int getHeight()
   {
      return 28;
   }

   public HandlerRegistration addCloseHandler(CloseHandler<WarningBar> handler)
   {
      return addHandler(handler, CloseEvent.getType());
   }

   @UiField
   SpanElement label_;
   @UiField
   Image dismiss_;

   private static final Styles styles_ =
         ((Resources) GWT.create(Resources.class)).styles();
   static
   {
      styles_.ensureInjected();
   }
}
