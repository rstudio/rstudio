/*
 * MenubarPanel.java
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
package org.rstudio.studio.client.application.ui.impl.header;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class MenubarPanel extends Composite
{
   static
   {
      ((Resources)GWT.create(Resources.class)).styles().ensureInjected();
   }
   
   interface Resources extends ClientBundle
   {
      @Source("MenubarPanel.css")
      Styles styles();

      ImageResource menubarLeft();
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource menubarTile();
      ImageResource menubarRight();
   }

   interface Styles extends CssResource
   {
      String panel();
      String left();
      String center();
      String right();
   }

   interface MyUiBinder extends UiBinder<Widget, MenubarPanel> {}

   public MenubarPanel(Widget widget)
   {
      widget_ = widget;
      initWidget(((MyUiBinder)GWT.create(MyUiBinder.class)).createAndBindUi(this));
   }

   @UiField(provided = true)
   final Widget widget_;
}
