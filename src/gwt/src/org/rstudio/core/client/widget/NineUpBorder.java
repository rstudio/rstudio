/*
 * NineUpBorder.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.user.client.ui.*;

public class NineUpBorder extends ResizeComposite implements AcceptsOneWidget
{
   public interface Resources extends ClientBundle
   {
      ImageResource topLeft();
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource top();
      ImageResource topRight();
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource left();
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource right();
      ImageResource bottomLeft();
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource bottom();
      ImageResource bottomRight();
   }

   public NineUpBorder(Resources resources,
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

      Image topLeft = new Image(resources.topLeft());
      Image top = new Image(resources.top());
      Image topRight = new Image(resources.topRight());
      Image left = new Image(resources.left());
      Image right = new Image(resources.right());
      Image bottomLeft = new Image(resources.bottomLeft());
      Image bottom = new Image(resources.bottom());
      Image bottomRight = new Image(resources.bottomRight());

      panel_.add(topLeft);
      panel_.setWidgetTopHeight(topLeft, 0, Unit.PX, topLeft.getHeight(), Unit.PX);
      panel_.setWidgetLeftWidth(topLeft, 0, Unit.PX, topLeft.getWidth(), Unit.PX);

      panel_.add(top);
      panel_.setWidgetLeftRight(top,
                                topLeft.getWidth(),
                                Unit.PX,
                                topRight.getWidth(),
                                Unit.PX);
      panel_.setWidgetTopHeight(top, 0, Unit.PX, top.getHeight(), Unit.PX);
      makeRepeating(top, "repeat-x");

      panel_.add(topRight);
      panel_.setWidgetRightWidth(topRight, 0, Unit.PX, topRight.getWidth(), Unit.PX);
      panel_.setWidgetTopHeight(topRight, 0, Unit.PX, topRight.getHeight(), Unit.PX);

      panel_.add(left);
      panel_.setWidgetLeftWidth(left, 0, Unit.PX, left.getWidth(), Unit.PX);
      panel_.setWidgetTopBottom(left,
                                topLeft.getHeight(),
                                Unit.PX,
                                bottomLeft.getHeight(),
                                Unit.PX);
      makeRepeating(left, "repeat-y");

      panel_.add(right);
      panel_.setWidgetRightWidth(right, 0, Unit.PX, right.getWidth(), Unit.PX);
      panel_.setWidgetTopBottom(right,
                                topRight.getHeight(),
                                Unit.PX,
                                bottomRight.getHeight(),
                                Unit.PX);
      makeRepeating(right, "repeat-y");

      panel_.add(bottomLeft);
      panel_.setWidgetBottomHeight(bottomLeft,
                                   0,
                                   Unit.PX,
                                   bottomLeft.getHeight(),
                                   Unit.PX);
      panel_.setWidgetLeftWidth(bottomLeft, 0, Unit.PX, bottomLeft.getWidth(), Unit.PX);

      panel_.add(bottom);
      panel_.setWidgetLeftRight(bottom, bottomLeft.getWidth(), Unit.PX, bottomRight.getWidth(), Unit.PX);
      panel_.setWidgetBottomHeight(bottom,
                                   0,
                                   Unit.PX,
                                   bottom.getHeight(),
                                   Unit.PX);
      makeRepeating(bottom, "repeat-x");

      panel_.add(bottomRight);
      panel_.setWidgetRightWidth(bottomRight, 0, Unit.PX, bottomRight.getWidth(), Unit.PX);
      panel_.setWidgetBottomHeight(bottomRight,
                                   0,
                                   Unit.PX,
                                   bottomRight.getHeight(),
                                   Unit.PX);

      initWidget(panel_);
   }

   private void makeRepeating(Image image, String repeatStyle)
   {
      Element container = panel_.getWidgetContainerElement(image);
      container.getStyle().setBackgroundImage("url(" + image.getUrl() + ")");
      container.getStyle().setProperty("backgroundRepeat", repeatStyle);
      image.setVisible(false);
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
         updateFillColor();
      }
   }

   public LayoutPanel getLayoutPanel()
   {
      return panel_;
   }

   public void setFillColor(String fillColor)
   {
      fillColor_ = fillColor;
      updateFillColor();
   }

   private void updateFillColor()
   {
      if (w_ != null)
      {
         Style s = panel_.getWidgetContainerElement(w_.asWidget()).getStyle();
         s.setBackgroundColor(fillColor_);
      }
   }

   private final LayoutPanel panel_;
   private IsWidget w_;
   private final int marginTop_;
   private final int marginRight_;
   private final int marginBottom_;
   private final int marginLeft_;
   private String fillColor_;
}
