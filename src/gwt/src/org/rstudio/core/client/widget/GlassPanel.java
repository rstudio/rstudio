/*
 * GlassPanel.java
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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.*;

public class GlassPanel extends ResizeComposite
{
   public GlassPanel(Widget child)
   {
      child_ = child;
      panel_ = new LayoutPanel();

      panel_.add(child);
      panel_.setWidgetLeftRight(child, 0, Unit.PX, 0, Unit.PX);
      panel_.setWidgetTopBottom(child, 0, Unit.PX, 0, Unit.PX);

      glass_ = new GlassAttacher(panel_);

      setGlass(false);

      initWidget(panel_);
   }

   public void setGlass(boolean enabled)
   {
      glass_.setGlass(enabled);
   }
   
   public Element getChildContainerElement()
   {
      return panel_.getWidgetContainerElement(child_);
   }

   private LayoutPanel panel_;
   private GlassAttacher glass_;
   private final Widget child_;
}
