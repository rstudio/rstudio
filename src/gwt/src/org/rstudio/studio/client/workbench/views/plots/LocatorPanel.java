/*
 * LocatorPanel.java
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
package org.rstudio.studio.client.workbench.views.plots;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.Point;
import org.rstudio.core.client.layout.FadeOutAnimation;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.HasCustomizableToolbar.Customizer;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.common.icons.StandardIcons;

import java.util.ArrayList;

public class LocatorPanel extends LayoutPanel
   implements Locator.Display, HasSelectionHandlers<Point>
{
   public LocatorPanel()
   {
      setStylePrimaryName(ThemeStyles.INSTANCE.locatorPanel());
          
      feedbackImage_ = new Image(new ImageResource2x(StandardIcons.INSTANCE.click_feedback2x()));
      feedbackImage_.setVisible(false);
      add(feedbackImage_);
      setWidgetTopHeight(feedbackImage_, 0, Unit.PX, FB_HEIGHT, Unit.PX);
      setWidgetLeftWidth(feedbackImage_, 0, Unit.PX, FB_WIDTH, Unit.PX);

      inputPanel_ = new InputPanel();
      inputPanel_.setSize("100%", "100%");
      add(inputPanel_);
      setWidgetLeftRight(inputPanel_, 0, Unit.PX, 0, Unit.PX);
      setWidgetTopBottom(inputPanel_, 0, Unit.PX, 0, Unit.PX);
      inputPanel_.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            Element el = inputPanel_.getElement();
            int x = event.getNativeEvent().getClientX();
            int y = event.getNativeEvent().getClientY();

            Point p = new Point(
                  x - el.getAbsoluteLeft(), y - el.getAbsoluteTop());

            showFeedbackAt(p);

            SelectionEvent.fire(LocatorPanel.this, p);
         }
      });
          
      // fill entire area of parent container
      setSize("100%", "100%");
   }
   
   public void showLocator(Plots.Parent parent)
   {
      // set parent 
      parent_ = parent ;
      
      // add to parent
      parent_.add(this);
      
      // add custom locator toolbar to parent
      installCustomToolbar();
      
      // subscribe to ESC key browser-wide for dismissal of the Locator UI
      // (unhook any existing subscription first)
      unhookNativePreviewHandler();
      escHandlerReg_ = Event.addNativePreviewHandler(new NativePreviewHandler(){

         public void onPreviewNativeEvent(NativePreviewEvent event)
         {
            if (event.getTypeInt() == Event.ONKEYDOWN
                && event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE)
            {
               event.cancel();
               SelectionEvent.fire(LocatorPanel.this, null);
            }
         }
         
      });
   }
   
   public void hideLocator()
   {
      // make sure we unsubscribe from events
      unhookNativePreviewHandler();
      
      // ice custom toolbar
      parent_.removeCustomToolbar();
      
      // remove from parent
      removeFromParent();
   }
   
   public HandlerRegistration addSelectionHandler(
         SelectionHandler<Point> handler)
   {
      return addHandler(handler, SelectionEvent.getType());
   }


   @Override
   protected void onUnload()
   {
      // make sure we unsubscribe from events
      unhookNativePreviewHandler();
      
      // nix any existing feedback
      cancelFeedback();
      
      // super
      super.onUnload();
   }
   
   private void installCustomToolbar()
   {
      parent_.installCustomToolbar(new Customizer() {
         public void setToolbarContents(Toolbar toolbar)
         {
            toolbar.addLeftWidget(
               new Label("Locator active (Esc to finish)"));
            
            SmallButton doneButton = new SmallButton("Finish"); 
            doneButton.addClickHandler(new ClickHandler() {
               public void onClick(ClickEvent event)
               {
                  SelectionEvent.fire(LocatorPanel.this, null);
               }         
            });   
            toolbar.addRightWidget(doneButton);
            toolbar.addRightWidget(new HTML("&nbsp;&nbsp;"));
         }
      });
   }

   private void showFeedbackAt(Point p)
   {
      cancelFeedback();

      setWidgetTopHeight(feedbackImage_, p.getY() - FB_OFFSET_Y,
                                Unit.PX, FB_HEIGHT, Unit.PX);
      setWidgetLeftWidth(feedbackImage_, p.getX() - FB_OFFSET_X,
                                Unit.PX, FB_WIDTH, Unit.PX);
      forceLayout();
      feedbackImage_.setVisible(true);
      feedbackImage_.getElement().getStyle().setOpacity(1.0);
      feedbackTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            feedbackTimer_ = null;
            ArrayList<Widget> widgets = new ArrayList<Widget>();
            widgets.add(feedbackImage_);
            feedbackAnimation_ = new FadeOutAnimation(widgets, 
                                                      new Command() {
               public void execute()
               {
                  feedbackAnimation_ = null;
               }      
            });
            feedbackAnimation_.run(300);
         }
      };
      feedbackTimer_.schedule(700);
   }

   private void cancelFeedback()
   {
      if (feedbackTimer_ != null)
      {
         feedbackTimer_.cancel();
         feedbackTimer_ = null;
      }
      if (feedbackAnimation_ != null)
      {
         feedbackAnimation_.cancel();
         feedbackAnimation_ = null;
      }
   }
   
   private void unhookNativePreviewHandler()
   {
      if (escHandlerReg_ != null)
      {
         escHandlerReg_.removeHandler();
         escHandlerReg_ = null;
      }
   }
   
   private static class InputPanel extends SimplePanel
   {
      public HandlerRegistration addClickHandler(ClickHandler handler)
      {
         return addDomHandler(handler, ClickEvent.getType());
      }
   }
   
   private Plots.Parent parent_;
   private Timer feedbackTimer_;
   private Animation feedbackAnimation_;
   private Image feedbackImage_;
   private static final int FB_HEIGHT = 24;
   private static final int FB_WIDTH = 24;
   private static final int FB_OFFSET_Y = 23;
   private static final int FB_OFFSET_X = 0;
   private InputPanel inputPanel_;
   private HandlerRegistration escHandlerReg_ = null;
}
