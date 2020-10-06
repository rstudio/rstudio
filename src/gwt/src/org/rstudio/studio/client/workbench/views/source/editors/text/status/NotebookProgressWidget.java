/*
 * NotebookProgressWidget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.status;

import java.util.ArrayList;

import org.rstudio.core.client.ColorUtil;

import com.google.gwt.animation.client.AnimationScheduler;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class NotebookProgressWidget extends Composite
                                    implements HasClickHandlers
{

   private static NotebookProgressWidgetUiBinder uiBinder = GWT
         .create(NotebookProgressWidgetUiBinder.class);

   interface NotebookProgressWidgetUiBinder
         extends UiBinder<Widget, NotebookProgressWidget>
   {
   }
   
   @Override
   public void setVisible(boolean visible)
   {
      super.setVisible(visible);
      if (visible)
      {
         AnimationScheduler.get().requestAnimationFrame(
               new AnimationScheduler.AnimationCallback()
         {
            @Override
            public void execute(double pos)
            {
               if (start_ == 0)
               {
                  // initialization
                  start_ = pos;
               }
               else if (pos - start_ > STEP_MS)
               {
                  highlight_ += Math.round(pos - start_) / STEP_MS;
                  start_ = pos;
                  updateProgressBar(false);
               }
               
               // request another frame if we're still showing
               if (isVisible())
               {
                  final AnimationScheduler.AnimationCallback callback = this;
                  new Timer()
                  {
                     @Override
                     public void run()
                     {
                        AnimationScheduler.get().requestAnimationFrame(
                              callback);
                     }
                  }.schedule(STEP_MS);
               }
            }
            
            private double start_ = 0;
            private final int STEP_MS = 15;
         });
      }
   }
   
   public void setPercent(int percent)
   {
      percent_ = percent;
      updateProgressBar(true);
   }
   
   public void setLabel(String label)
   {
      progressLabel_.setText(label + ": ");
   }
   
   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return manager_.addHandler(ClickEvent.getType(), handler);
   }
   
   public HandlerRegistration addCancelHandler(final Command onCanceled)
   {
      cancelCommands_.add(onCanceled);
      return new HandlerRegistration()
      {
         @Override
         public void removeHandler()
         {
            cancelCommands_.remove(onCanceled);
         }
      };
   }

   public NotebookProgressWidget()
   {
      manager_ = new HandlerManager(this);
      cancelCommands_ = new ArrayList<Command>();

      initWidget(uiBinder.createAndBindUi(this));
      
      // ensure elements look clickable
      progressBar_.getElement().getStyle().setCursor(Cursor.POINTER);
      interruptButton_.getElement().getStyle().setCursor(Cursor.POINTER);

      MouseDownHandler handler = new MouseDownHandler()
      {
         public void onMouseDown(MouseDownEvent event)
         {
            event.preventDefault();
            event.stopPropagation();

            ClickEvent.fireNativeEvent(
                  Document.get().createClickEvent(0, 0, 0, 0, 0, false, false, 
                        false, false),
                  manager_);
         }
      };

      // connect native click handler
      progressBar_.addDomHandler(handler, MouseDownEvent.getType());

      interruptButton_.addDomHandler(new MouseDownHandler()
      {
         @Override
         public void onMouseDown(MouseDownEvent event)
         {
            for (Command cancelCommand: cancelCommands_)
               cancelCommand.execute();
         }
      }, MouseDownEvent.getType());
   }
   
   private void updateProgressBar(boolean resetHighlight)
   {
      ColorUtil.RGBColor barColor = ColorUtil.RGBColor.fromCss(
            "rgba(24, 163, 82, 1.0)");
      ColorUtil.RGBColor highColor = ColorUtil.RGBColor.fromCss(
            "rgba(208, 233, 201, 1.0)");
      ColorUtil.RGBColor emptyColor = ColorUtil.RGBColor.fromCss(
            "rgba(24, 163, 82, 0.3)");
      
      int end = Math.round((float)progressBar_.getOffsetWidth() *
            ((float) percent_ / (float) 100));
      
      final int HEAD_WIDTH = 10;
      final int TAIL_WIDTH = 35;
      
      int high = highlight_ % (progressBar_.getOffsetWidth() + TAIL_WIDTH);
      int highStart = Math.max(high - TAIL_WIDTH, 0);
      int highEnd = Math.min(high + HEAD_WIDTH, end);
      highlight_ = high; // avoid overflow
      if (high > end && highStart <= end)
      {
         highColor = highColor.mixedWith(barColor, 
               (float)(end - highStart) / (float) TAIL_WIDTH, 1);
         high = end;
      }
      else if (highStart > end)
      {
         // don't draw highlight bar if it's fully out past the edge
         if (resetHighlight)
            highlight_ = 0;
         high = 0;
         highStart = 0;
         highEnd = 0;
      }
      
      progressBar_.getElement().getStyle().setBackgroundImage(
            "linear-gradient(to right, " +
              barColor.asRgb()   + " 0, " +
              barColor.asRgb()   + " " + highStart + "px, " +
              highColor.asRgb()  + " " + high      + "px, " +
              barColor.asRgb()   + " " + highEnd   + "px, " +
              barColor.asRgb()   + " " + end       + "px, " +
              emptyColor.asRgb() + " " + end       + "px, " +
              emptyColor.asRgb() + " 100%)");
   }
   
   @UiField HTMLPanel progressBar_;
   @UiField HorizontalPanel root_;
   @UiField Label progressLabel_;
   @UiField Image interruptButton_;

   private int percent_ = 0;
   private int highlight_ = 0;
   private final HandlerManager manager_;
   private final ArrayList<Command> cancelCommands_;
}
