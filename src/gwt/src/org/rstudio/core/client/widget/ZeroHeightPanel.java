/*
 * ZeroHeightPanel.java
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.uibinder.client.*;
import com.google.gwt.user.client.ui.*;

/**
 * Displays a widget without letting its effective height disturb the layout.
 * The height/width of the wrapped widget must be fixed and known in advance.
 */
public class ZeroHeightPanel extends Composite
{
   interface Binder extends UiBinder<Widget, ZeroHeightPanel>
   {}

   @UiConstructor
   public ZeroHeightPanel(int width,
                          int height,
                          int yOffset)
   {
      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));
      outer_.getElement().getStyle().setWidth(width, Style.Unit.PX);
      inner_.getElement().getStyle().setWidth(width, Style.Unit.PX);
      inner_.getElement().getStyle().setHeight(height, Style.Unit.PX);
      inner_.getElement().getStyle().setBottom(-yOffset, Style.Unit.PX);
   }

   @UiChild
   public void addChild(Widget w)
   {
      inner_.add(w);
   }

   @UiField
   HTMLPanel outer_;
   @UiField
   FlowPanel inner_;
}
