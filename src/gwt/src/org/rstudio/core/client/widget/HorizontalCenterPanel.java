/*
 * HorizontalCenterPanel.java
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

import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class HorizontalCenterPanel extends DockPanel
{
   public HorizontalCenterPanel(Widget widget, int verticalOffset)
   {
      widget_ = widget;
      VerticalPanel verticalPadWidget = new VerticalPanel();
      add(verticalPadWidget, DockPanel.NORTH);
      setCellHeight(verticalPadWidget, verticalOffset + "px");
      add(widget_, DockPanel.CENTER);
      setCellHorizontalAlignment(widget, DockPanel.ALIGN_CENTER);   
   }
   
   protected Widget getWidget()
   {
      return widget_;
   }
   
   private final Widget widget_;
}
