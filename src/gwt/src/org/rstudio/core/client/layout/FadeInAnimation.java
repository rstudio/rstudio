/*
 * FadeInAnimation.java
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

public class FadeInAnimation extends Animation
{
   public FadeInAnimation(Widget widget,
                          double targetOpacity,
                          Command callback)
   {
      this(new ArrayList<>(), targetOpacity, callback);
      widgets_.add(widget);
   }

   public FadeInAnimation(ArrayList<Widget> widgets,
                          double targetOpacity,
                          Command callback)
   {
      this.widgets_ = widgets;
      targetOpacity_ = targetOpacity;
      callback_ = callback;
   }

   @Override
   protected void onStart()
   {
      for (Widget w : widgets_)
         w.getElement().getStyle().setDisplay(Style.Display.BLOCK);
      super.onStart();
   }

   @Override
   protected void onUpdate(double progress)
   {
      for (Widget w : widgets_)
         w.getElement().getStyle().setOpacity(targetOpacity_ * progress);
   }

   @Override
   protected void onComplete()
   {
      for (Widget w : widgets_)
      {
         w.getElement().getStyle().setOpacity(targetOpacity_);
         w.setVisible(true);
      }
      if (callback_ != null)
         callback_.execute();
   }

   private ArrayList<Widget> widgets_;
   private final double targetOpacity_;
   private final Command callback_;
}
