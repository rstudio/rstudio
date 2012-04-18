/*
 * InfoBar.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.theme.res.ThemeResources;

public class InfoBar extends Composite 
{
   public static final int INFO = 0;
   public static final int WARNING = 1;
   public static final int ERROR = 2;
   
   public InfoBar(int mode)
   {
      this(mode, null);
   }
   
   public InfoBar(int mode, ClickHandler dismissHandler)
   {
      switch(mode)
      {
      case WARNING:
         icon_ = new Image(ThemeResources.INSTANCE.warningSmall());
         break;
      case ERROR:
         icon_ = new Image(ThemeResources.INSTANCE.errorSmall());
         break;
      case INFO:
      default:
         icon_ = new Image(ThemeResources.INSTANCE.infoSmall());
         break;
      
      }
     
      initWidget(binder.createAndBindUi(this));
      
      if (dismissHandler != null)
         dismiss_.addClickHandler(dismissHandler);
      else
         dismiss_.setVisible(false);
   }
   
  
   public String getText()
   {
      return label_.getText();
   }

   public void setText(String text)
   {
      label_.setText(text);
   }

   public int getHeight()
   {
      return 19;
   }

   @UiField
   protected DockLayoutPanel container_;
   @UiField(provided = true)
   protected Image icon_;
   @UiField
   protected Label label_;
   @UiField
   Image dismiss_;

   interface MyBinder extends UiBinder<Widget, InfoBar>{}
   private static MyBinder binder = GWT.create(MyBinder.class);
}
