/*
 * SlidingLayoutPanel.java
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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.layout.client.Layout.AnimationCallback;
import com.google.gwt.layout.client.Layout.Layer;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * SlidingLayoutPanel is a layout panel that hosts two widgets and can slide
 * back and forth between them, from left to right, in an (optionally) animated
 * way.
 */
public class SlidingLayoutPanel extends LayoutPanel
{
   public enum Direction
   {
      SlideLeft,
      SlideRight
   }

   public SlidingLayoutPanel(Widget left, Widget right)
   {
      // left widget is displayed initially
      left_ = left;
      add(left);
      setWidgetTopBottom(left_, 0, Unit.PX, 0, Unit.PX);
      setWidgetLeftRight(left_, 0, Unit.PX, 0, Unit.PX);
      
      // right widget is offscreen
      right_ = right;
      add(right);
      setWidgetTopBottom(right_, 0, Unit.PX, 0, Unit.PX);
      setWidgetLeftRight(right_, -5000, Unit.PX, 5000, Unit.PX);
   }

   public void slideWidgets(Direction direction, boolean animate,
                        final Command onComplete)
   {
      Widget from;
      Widget to;
      if (direction == Direction.SlideLeft)
      {
         from = right_;
         to = left_;
      }
      else
      {
         from = left_;
         to = right_;
      }

      int width = getOffsetWidth();

      setWidgetLeftWidth(from, 0, Unit.PX, width, Unit.PX);
      setWidgetLeftWidth(to, direction == Direction.SlideRight ? 
         width : -width, Unit.PX, width, Unit.PX);
      forceLayout();

      setWidgetLeftWidth(from, direction == Direction.SlideRight ? 
         -width : width, Unit.PX, width, Unit.PX);
      setWidgetLeftWidth(to, 0, Unit.PX, width, Unit.PX); 
      to.setVisible(true);
      from.setVisible(true);
      
      final Command completeLayout = new Command() {

         @Override
         public void execute()
         {
            setWidgetLeftRight(to, 0, Unit.PX, 0, Unit.PX);
            from.setVisible(false);
            forceLayout();
            onComplete.execute();
         }
         
      };
      
      if (animate)
      {
         animate(300, new AnimationCallback()
         {
            public void onAnimationComplete()
            {
               completeLayout.execute();
            }
   
            public void onLayout(Layer layer, double progress)
            {
            }
         });
      }
      else
      {
         completeLayout.execute();
      }
   }
   
   private final Widget left_;
   private final Widget right_;
}
