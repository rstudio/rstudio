/*
 * FadeOutAnimation.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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

public class FadeOutAnimation extends Animation
{
   public FadeOutAnimation(ArrayList<Widget> widgets, Command callback)
   {
      this.widgets_ = widgets;
      callback_ = callback;
   }

   @Override
   protected void onUpdate(double progress)
   {
      for (Widget w : widgets_)
         w.getElement().getStyle().setOpacity(1.0-progress);
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

   private ArrayList<Widget> widgets_;
   private final Command callback_;
}
