/*
 * StatusBarWidget.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.status;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.widget.WidgetableWithHeight;

public class StatusBarWidget extends Composite
      implements StatusBar, WidgetableWithHeight
{
   private int height_;

   interface Styles extends CssResource
   {
      String statusBar();
   }

   interface Resources extends ClientBundle
   {
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource statusBarTile();

      ImageResource upDownArrow();

      ImageResource statusBarSeparator();

      @Source("StatusBar.css")
      Styles styles();
   }

   interface Binder extends UiBinder<HorizontalPanel, StatusBarWidget>
   {
   }

   public StatusBarWidget()
   {
      Binder binder = GWT.create(Binder.class);
      HorizontalPanel hpanel = binder.createAndBindUi(this);
      hpanel.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);

      hpanel.setCellWidth(hpanel.getWidget(1), "100%");

      initWidget(hpanel);

      height_ = 16;
   }

   public int getHeight()
   {
      return height_;
   }

   public Widget toWidget()
   {
      return this;
   }

   public StatusBarElement getPosition()
   {
      return position_;
   }

   public StatusBarElement getFunction()
   {
      return function_;
   }

   public StatusBarElement getLanguage()
   {
      return language_;
   }

   public StatusBarElement getEncoding()
   {
      return encoding_;
   }

   public StatusBarElement getTabs()
   {
      return tabStyle_;
   }

   @UiField
   StatusBarElementWidget position_;
   @UiField
   StatusBarElementWidget function_;
   @UiField
   StatusBarElementWidget language_;
   @UiField
   StatusBarElementWidget encoding_;
   @UiField
   StatusBarElementWidget tabStyle_;
}
