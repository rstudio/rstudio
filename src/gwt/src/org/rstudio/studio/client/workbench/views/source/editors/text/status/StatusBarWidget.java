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
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.widget.IsWidgetWithHeight;

public class StatusBarWidget extends Composite
      implements StatusBar, IsWidgetWithHeight
{
   private int height_;

   interface Binder extends UiBinder<HorizontalPanel, StatusBarWidget>
   {
   }

   public StatusBarWidget()
   {
      Binder binder = GWT.create(Binder.class);
      HorizontalPanel hpanel = binder.createAndBindUi(this);
      hpanel.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);

      hpanel.setCellWidth(hpanel.getWidget(2), "100%");

      // tweak image baseline for ubuntu mono on chrome
      if (BrowseCap.hasUbuntuFonts() && BrowseCap.isChrome())
         scopeIcon_.getElement().getStyle().setTop(0, Unit.PX);
      
      initWidget(hpanel);

      height_ = 16;
   }

   public int getHeight()
   {
      return height_;
   }

   public Widget asWidget()
   {
      return this;
   }

   public StatusBarElement getPosition()
   {
      return position_;
   }

   public StatusBarElement getScope()
   {
      return scope_;
   }

   public StatusBarElement getLanguage()
   {
      return language_;
   }

   public void setScopeVisible(boolean visible)
   {
      scope_.setClicksEnabled(visible);
      scope_.setContentsVisible(visible);
      scopeIcon_.setVisible(visible);
   }
   
   public void setScopeType(int type)
   {
      if (type == StatusBar.SCOPE_FUNCTION)
         scopeIcon_.setResource(RES.function());
      else if (type == StatusBar.SCOPE_CHUNK)
         scopeIcon_.setResource(RES.chunk());
      else if (type == StatusBar.SCOPE_SECTION)
         scopeIcon_.setResource(RES.section());
   }

   @UiField
   StatusBarElementWidget position_;
   @UiField
   StatusBarElementWidget scope_;
   @UiField
   StatusBarElementWidget language_;
   @UiField
   Image scopeIcon_;
   
   interface Resources extends ClientBundle
   {
      ImageResource chunk();
      ImageResource function();
      ImageResource section();
   }
   private static Resources RES = GWT.create(Resources.class);
}
