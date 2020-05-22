/*
 * ProgressSpinner.java
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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

import org.rstudio.core.client.ColorUtil;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.dom.DomUtils;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.Context2d.LineCap;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;

public class ProgressSpinner extends Composite
{
   public ProgressSpinner(Element element)
   {
      this(colorFromStyle(element));
   }
   
   private static int colorFromStyle(Element element)
   {
      Style style = DomUtils.getComputedStyles(element);
      style.getBackgroundColor();
      
      ColorUtil.RGBColor bgColor = 
         ColorUtil.RGBColor.fromCss(style.getBackgroundColor());
      
      return bgColor.isDark() ?
         ProgressSpinner.COLOR_WHITE:
         ProgressSpinner.COLOR_BLACK;
   }
   
   public ProgressSpinner(int color)
   {
      // compute sizes
      outerRadius_ = (COORD_SIZE / 2) - 10;
      innerRadius_ = (outerRadius_ / 2) + 5;
      colorType_ = color;

      // create canvas host
      canvas_ = Canvas.createIfSupported();
      if (canvas_ == null)
      {
         Debug.log("Can't create progress spinner (no HTML5 canvas support)");
         initWidget(new HTMLPanel(""));
         return;
      }
      
      initWidget(canvas_);
      
      // initialize canvas
      canvas_.setCoordinateSpaceWidth(COORD_SIZE);
      canvas_.setCoordinateSpaceHeight(COORD_SIZE);

      Roles.getImgRole().set(canvas_.getElement());
      Roles.getImgRole().setAriaLabelProperty(canvas_.getElement(), "Busy");
   }
   
   @Override
   public void setVisible(boolean visible)
   {
      if (visible)
      {
         requestStopAnimating_ = false;
         startAnimating();
      }
      else
      {
         stopAnimating();
      }
      
      super.setVisible(visible);
   }
   
   @Override
   public void onUnload()
   {
      stopAnimating();
      super.onUnload();
   }

   public boolean isSupported()
   {
      return canvas_ != null;
   }
   
   public void setColorType(int color)
   {
      colorType_ = color;
   }
   
   public void startAnimating()
   {
      if (isAnimating_ || requestStopAnimating_)
         return;
      
      isAnimating_ = true;
      animate();
      Scheduler.get().scheduleFixedPeriod(() -> { return animate(); }, FRAME_RATE_MS);
   }
   
   public void stopAnimating()
   {
      requestStopAnimating_ = true;
   }
   
   private boolean animate()
   {
      frame_++;
      redraw();
      
      if (requestStopAnimating_)
      {
         requestStopAnimating_ = false;
         isAnimating_ = false;
         return false;
      }
      
      return true;
   }
   
   private void redraw()
   {
      String color = colorType_ == COLOR_WHITE ? "255, 255, 255" : "0, 0, 0";

      Context2d ctx = canvas_.getContext2d();
      double center = COORD_SIZE / 2;
      // clear canvas (we draw with an alpha channel so otherwise would stack)
      ctx.clearRect(0, 0, COORD_SIZE, COORD_SIZE);
      for (int i = 0; i < NUM_BLADES; i++)
      {
         // compute angle for this blade
         double theta = ((2 * Math.PI) / NUM_BLADES) * i;
         double sin = Math.sin(theta);
         double cos = Math.cos(theta);

         // set line drawing context
         ctx.beginPath();
         ctx.setLineWidth(BLADE_WIDTH);
         ctx.setLineCap(LineCap.ROUND);

         // compute transparency for this blade
         double alpha = 1.0 - (((double)((i + frame_) % NUM_BLADES)) / 
                               ((double)NUM_BLADES));
         ctx.setStrokeStyle("rgba(" + color + ", " + alpha + ")");
         
         // draw the blade
         ctx.moveTo(center + sin * innerRadius_,
                    center + cos * innerRadius_);
         ctx.lineTo(center + sin * outerRadius_, 
                    center + cos * outerRadius_);
         ctx.stroke();
      }
   }
   
   // drawing parameters 
   private final static int NUM_BLADES    = 12;
   private final static int BLADE_WIDTH   = 9;
   private final static int COORD_SIZE    = 100;
   private final static int FRAME_RATE_MS = 75;
   
   public final static int COLOR_WHITE = 0;
   public final static int COLOR_BLACK = 1;
   
   private final int innerRadius_;
   private final int outerRadius_;
   private final Canvas canvas_;
   private int colorType_;

   private int frame_ = 0;
   private boolean isAnimating_ = false;
   private boolean requestStopAnimating_ = false;
}
