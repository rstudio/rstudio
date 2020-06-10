/*
 * LeftCenterRightBorder.java
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
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.user.client.ui.*;

public class LeftCenterRightBorder extends ResizeComposite implements AcceptsOneWidget
{
   public interface Resources 
   {
      ImageResource left();
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource center();
      ImageResource right();
   }

   public LeftCenterRightBorder(Resources resources,
                                int marginTop,
                                int marginRight,
                                int marginBottom,
                                int marginLeft)
   {
      marginTop_ = marginTop;
      marginRight_ = marginRight;
      marginBottom_ = marginBottom;
      marginLeft_ = marginLeft;
      panel_ = new LayoutPanel();

      Image left = new Image(resources.left());
      Image center = new Image(resources.center());
      Image right = new Image(resources.right());

      panel_.add(left);
      panel_.setWidgetLeftWidth(left, 0, Unit.PX, left.getWidth(), Unit.PX);
      panel_.setWidgetTopHeight(left, 0, Unit.PX, left.getHeight(), Unit.PX);

      panel_.add(center);
      panel_.setWidgetLeftRight(center,
                                left.getWidth(),
                                Unit.PX,
                                right.getWidth(),
                                Unit.PX);
      panel_.setWidgetTopHeight(center,
                                0,
                                Unit.PX,
                                center.getHeight(),
                                Unit.PX);
      Element centerEl = panel_.getWidgetContainerElement(center);
      centerEl.getStyle().setBackgroundImage("url(" + center.getUrl() + ")");
      centerEl.getStyle().setProperty("backgroundRepeat", "repeat-x");
      center.setVisible(false);

      panel_.add(right);
      panel_.setWidgetRightWidth(right, 0, Unit.PX, right.getWidth(), Unit.PX);
      panel_.setWidgetTopHeight(right, 0, Unit.PX, right.getHeight(), Unit.PX);

      initWidget(panel_);
   }

   @Override
   public void setWidget(IsWidget w)
   {
      if (w_ != null)
      {
         panel_.remove(w_);
         w_ = null;
      }

      w_ = w;
      if (w_ != null)
      {
         panel_.add(w_);
         panel_.setWidgetLeftRight(w_, marginLeft_, Unit.PX,  marginRight_, Unit.PX);
         panel_.setWidgetTopBottom(w_, marginTop_, Unit.PX,  marginBottom_, Unit.PX);
      }
   }

   private final LayoutPanel panel_;
   private IsWidget w_;
   private final int marginTop_;
   private final int marginRight_;
   private final int marginBottom_;
   private final int marginLeft_;
}
