/*
 * SlideLabel.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.*;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.resources.CoreResources;

public class SlideLabel extends Widget
{
   static Resources RESOURCES = (Resources)GWT.create(Resources.class);
   public static void ensureStylesInjected() 
   {
      RESOURCES.style().ensureInjected();
   }

   interface Binder extends UiBinder<Element, SlideLabel> {}
   private static Binder binder = GWT.create(Binder.class);

   interface Resources extends ClientBundle
   {
      @Source("SlideLabel.css")
      SlideLabelCss style();

      @Source("slideLabelBottom.png")
      DataResource slideLabelBottom();
      @Source("slideLabelBottomLeft.png")
      DataResource slideLabelBottomLeft();
      @Source("slideLabelBottomRight.png")
      DataResource slideLabelBottomRight();
      @Source("slideLabelFill.png")
      DataResource slideLabelFill();
      @Source("slideLabelLeft.png")
      DataResource slideLabelLeft();
      @Source("slideLabelRight.png")
      DataResource slideLabelRight();
   }

   interface SlideLabelCss extends CssResource
   {
      String curtain();
      String border();
      String progress();
      String content();

      String W();
      String C();
      String E();
      String SW();
      String S();
      String SE();
   }

   public static Command show(String label,
                              boolean asHtml,
                              boolean showProgressSpinner,
                              final LayoutPanel panel)
   {
      final SlideLabel slideLabel = showInternal(label,
                                                 asHtml,
                                                 showProgressSpinner,
                                                 panel);
      slideLabel.show();
      return new Command()
      {
         public void execute()
         {
            panel.remove(slideLabel);
         }
      };
   }

   public static void show(String label,
                           boolean asHtml,
                           boolean showProgressSpinner,
                           int autoHideMillis,
                           final LayoutPanel panel)
   {
      final SlideLabel slideLabel = showInternal(label,
                                                 asHtml,
                                                 showProgressSpinner,
                                                 panel);
      slideLabel.show(autoHideMillis, new Command()
      {
         public void execute()
         {
            panel.remove(slideLabel);
         }
      });
   }

   private static SlideLabel showInternal(String label,
                                          boolean asHtml,
                                          boolean showProgressSpinner,
                                          LayoutPanel panel)
   {
      final SlideLabel slideLabel = new SlideLabel(showProgressSpinner);
      slideLabel.setText(label, asHtml);
      panel.add(slideLabel);
      panel.setWidgetLeftRight(slideLabel,
                               0, Style.Unit.PX,
                               0, Style.Unit.PX);
      panel.setWidgetTopHeight(slideLabel,
                               0, Style.Unit.PX,
                               100, Style.Unit.PX);
      panel.forceLayout();
      return slideLabel;
   }

   public SlideLabel(boolean showProgressSpinner)
   {
      setElement(binder.createAndBindUi(this));
      if (showProgressSpinner)
         progress_.setSrc(CoreResources.INSTANCE.progress_gray_as_data().getSafeUri().asString());
      else
         progress_.getStyle().setDisplay(Style.Display.NONE);
      curtain_.getStyle().setHeight(0, Style.Unit.PX);
   }

   public void setText(String label, boolean asHtml)
   {
      if (asHtml)
         content_.setInnerHTML(label);
      else
         content_.setInnerText(label);
   }

   public void show()
   {
      show(-1, null);
   }

   public void show(final int autoHideMillis, final Command executeOnComplete)
   {
      assert autoHideMillis >= 0 || executeOnComplete == null:
            "Possible error: executeOnComplete will never be called with " +
            "negative value for autoHideMillis";

      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         public void execute()
         {
            stopCurrentAnimation();
            currentAnimation_ = new Animation() {
               @Override
               protected void onStart()
               {
                  setVisible(true);
                  curtain_.getStyle().setHeight(0, Style.Unit.PX);
                  height = content_.getOffsetHeight() + 14 + 14;
                  super.onStart();
               }

               @Override
               protected void onUpdate(double progress)
               {
                  setHeight(height * progress);
               }

               @Override
               protected void onComplete()
               {
                  currentAnimation_ = null;
                  if (autoHideMillis >= 0)
                  {
                     currentAutoHideTimer_ = new Timer() {
                        @Override
                        public void run()
                        {
                           currentAutoHideTimer_ = null;
                           hide(executeOnComplete);
                        }
                     };
                     currentAutoHideTimer_.schedule(autoHideMillis);
                  }
               }

               private int height;
            };
            currentAnimation_.run(ANIM_MILLIS);
         }
      });
   }

   public void hide()
   {
      hide(null);
   }

   public void hide(final Command executeOnComplete)
   {
      stopCurrentAnimation();
      final int height = curtain_.getOffsetHeight();
      currentAnimation_ = new Animation() {
         @Override
         protected void onUpdate(double progress)
         {
            setHeight(height * (1-progress));
         }

         @Override
         protected void onComplete()
         {
            currentAnimation_ = null;
            super.onComplete();
            setVisible(false);
            if (executeOnComplete != null)
               executeOnComplete.execute();
         }
      };
      currentAnimation_.run(ANIM_MILLIS);
   }

   private void setHeight(double height)
   {
      curtain_.getStyle().setHeight((int)height, Style.Unit.PX);
   }

   private void stopCurrentAnimation()
   {
      if (currentAnimation_ != null)
      {
         currentAnimation_.cancel();
         currentAnimation_ = null;
      }

      if (currentAutoHideTimer_ != null)
      {
         currentAutoHideTimer_.cancel();
         currentAutoHideTimer_ = null;
      }
   }

   @UiField
   DivElement curtain_;
   @UiField
   DivElement content_;
   @UiField
   TableElement border_;
   @UiField
   ImageElement progress_;

   private Animation currentAnimation_;
   private Timer currentAutoHideTimer_;

   private static final int ANIM_MILLIS = 250;
}
