/*
 * FadeOutAnimation.java
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
package org.rstudio.core.client.layout;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

public class FadeOutAnimation extends Animation
{
   public FadeOutAnimation(Widget widget, Command callback)
   {
      List<Widget> widgets = new ArrayList<Widget>();
      widgets.add(widget);
      widgets_ = widgets;
      callback_ = callback;
   }
   public FadeOutAnimation(ArrayList<Widget> widgets, Command callback)
   {
      widgets_ = widgets;
      callback_ = callback;
   }

   @Override
   protected void onUpdate(double progress)
   {
      for (Widget w : widgets_)
         w.getElement().getStyle().setOpacity(1.0 - progress);
   }

   @Override
   protected void onComplete()
   {
      for (Widget w : widgets_)
      {
         Style style = w.getElement().getStyle();
         style.setDisplay(Style.Display.NONE);
         style.setOpacity(1.0);
      }
      if (callback_ != null)
         callback_.execute();
   }

   private final List<Widget> widgets_;
   private final Command callback_;
}
