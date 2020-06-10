/*
 * BinarySplitLayoutPanel.java
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

import com.google.gwt.aria.client.OrientationValue;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.layout.client.Layout;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;

public class BinarySplitLayoutPanel extends LayoutPanel
      implements MouseDownHandler, MouseMoveHandler, MouseUpHandler,
                 KeyDownHandler, BlurHandler, FocusHandler
{
   /**
    * Default number of pixels pane splitters are moved by arrow keys
    */
   public static final int KEYBOARD_MOVE_SIZE = 20;

   public BinarySplitLayoutPanel(String name, Widget[] widgets, int splitterHeight)
   {
      widgets_ = widgets;
      splitterHeight_ = splitterHeight;

      setWidgets(widgets);

      splitterPos_ = 300;
      topIsFixed_ = false;
      splitter_ = new HTML();
      splitter_.setStylePrimaryName("gwt-SplitLayoutPanel-VDragger");
      splitter_.addMouseDownHandler(this);
      splitter_.addMouseMoveHandler(this);
      splitter_.addMouseUpHandler(this);
      splitter_.addDomHandler(this, KeyDownEvent.getType());
      splitter_.addDomHandler(this, BlurEvent.getType());
      splitter_.addDomHandler(this, FocusEvent.getType());
      splitter_.getElement().getStyle().setZIndex(200);
      Roles.getSeparatorRole().set(splitter_.getElement());
      Roles.getSeparatorRole().setAriaOrientationProperty(splitter_.getElement(),
         OrientationValue.HORIZONTAL);
      Roles.getSeparatorRole().setAriaLabelProperty(splitter_.getElement(), name + " splitter");
      splitter_.getElement().setTabIndex(-1);
      add(splitter_);
      setWidgetLeftRight(splitter_, 0, Style.Unit.PX, 0, Style.Unit.PX);
      setWidgetBottomHeight(splitter_,
                            splitterPos_, Style.Unit.PX,
                            splitterHeight_, Style.Unit.PX);
   }

   public void setWidgets(Widget[] widgets)
   {
      for (Widget w : widgets_)
         remove(w);

      widgets_ = widgets;
      for (Widget w : widgets)
      {
         add(w);
         setWidgetLeftRight(w, 0, Style.Unit.PX, 0, Style.Unit.PX);
         setWidgetTopHeight(w, 0, Style.Unit.PX, 100, Style.Unit.PX);
         w.setVisible(false);
         AnimationHelper.setParentZindex(w, -10);
      }

      if (top_ >= 0)
         setTopWidget(top_, true);
      if (bottom_ >= 0)
         setBottomWidget(bottom_, true);
   }

   @Override
   protected void onAttach()
   {
      super.onAttach();
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         public void execute()
         {
            offsetHeight_ = getOffsetHeight();
         }
      });
   }

   public HandlerRegistration addSplitterBeforeResizeHandler(
         SplitterBeforeResizeHandler handler)
   {
      return addHandler(handler, SplitterBeforeResizeEvent.TYPE);
   }

   public HandlerRegistration addSplitterResizedHandler(
         SplitterResizedHandler handler)
   {
      return addHandler(handler, SplitterResizedEvent.TYPE);
   }

   public void setTopWidget(Widget widget, boolean manageVisibility)
   {
      if (widget == null)
      {
         setTopWidget(-1, manageVisibility);
         return;
      }

      for (int i = 0; i < widgets_.length; i++)
         if (widgets_[i] == widget)
         {
            setTopWidget(i, manageVisibility);
            return;
         }

      assert false;
   }

   public void setTopWidget(int widgetIndex, boolean manageVisibility)
   {
      if (manageVisibility && top_ >= 0)
         widgets_[top_].setVisible(false);

      top_ = widgetIndex;
      if (bottom_ == top_)
         setBottomWidget(-1, manageVisibility);

      if (manageVisibility && top_ >= 0)
         widgets_[top_].setVisible(true);

      updateLayout();
   }

   public void setBottomWidget(Widget widget, boolean manageVisibility)
   {
      if (widget == null)
      {
         setBottomWidget(-1, manageVisibility);
         return;
      }

      for (int i = 0; i < widgets_.length; i++)
         if (widgets_[i] == widget)
         {
            setBottomWidget(i, manageVisibility);
            return;
         }

      assert false;
   }

   public void setBottomWidget(int widgetIndex, boolean manageVisibility)
   {
      if (manageVisibility && bottom_ >= 0)
         widgets_[bottom_].setVisible(false);

      bottom_ = widgetIndex;
      if (top_ == bottom_)
         setTopWidget(-1, manageVisibility);

      if (manageVisibility && bottom_ >= 0)
         widgets_[bottom_].setVisible(true);

      updateLayout();
   }

   public boolean isSplitterVisible()
   {
      return splitter_.isVisible();
   }

   public void setSplitterVisible(boolean visible)
   {
      splitter_.setVisible(visible);
   }

   public void setSplitterPos(int splitterPos, boolean fromTop)
   {
      if (isVisible() && isAttached() && splitter_.isVisible())
      {
         splitterPos = Math.min(getOffsetHeight() - splitterHeight_,
                                splitterPos);
      }

      if (splitter_.isVisible())
         splitterPos = Math.max(splitterHeight_, splitterPos);

      if (splitterPos_ == splitterPos
          && topIsFixed_ == fromTop
          && offsetHeight_ == getOffsetHeight())
      {
         return;
      }

      splitterPos_ = splitterPos;
      topIsFixed_ = fromTop;
      offsetHeight_ = getOffsetHeight();
      if (topIsFixed_)
      {
         setWidgetTopHeight(splitter_,
                            splitterPos_,
                            Style.Unit.PX,
                            splitterHeight_,
                            Style.Unit.PX);
      }
      else
      {
         setWidgetBottomHeight(splitter_,
                               splitterPos_,
                               Style.Unit.PX,
                               splitterHeight_,
                               Style.Unit.PX);
      }

      updateLayout();
   }

   public int getSplitterBottom()
   {
      assert !topIsFixed_;
      return splitterPos_;
   }

   private void updateLayout()
   {
      if (topIsFixed_)
      {
         if (top_ >= 0)
            setWidgetTopHeight(widgets_[top_],
                               0,
                               Style.Unit.PX,
                               splitterPos_,
                               Style.Unit.PX);

         if (bottom_ >= 0)
            setWidgetTopBottom(widgets_[bottom_],
                               splitterPos_ + splitterHeight_,
                               Style.Unit.PX,
                               0,
                               Style.Unit.PX);
      }
      else
      {
         if (top_ >= 0)
            setWidgetTopBottom(widgets_[top_],
                               0,
                               Style.Unit.PX,
                               splitterPos_ + splitterHeight_,
                               Style.Unit.PX);

         if (bottom_ >= 0)
            setWidgetBottomHeight(widgets_[bottom_],
                                  0,
                                  Style.Unit.PX,
                                  splitterPos_,
                                  Style.Unit.PX);
      }

      // Not sure why, but onResize() doesn't seem to get called unless we
      // do this manually. This matters for ShellPane scroll position updating.
      animate(0, new Layout.AnimationCallback()
      {
         public void onAnimationComplete()
         {
            onResize();
         }

         public void onLayout(Layout.Layer layer, double progress)
         {
         }
      });
   }

   @Override
   public void onResize()
   {
      super.onResize();
      // getOffsetHeight() > 0 is to deal with Firefox tab tear-off, which
      // causes us to be resized to 0 (bug 1586)
      if (offsetHeight_ > 0 && splitter_.isVisible() && getOffsetHeight() > 0)
      {
         double pct = ((double)splitterPos_ / offsetHeight_);
         int newPos = (int) Math.round(getOffsetHeight() * pct);
         setSplitterPos(newPos, topIsFixed_);
      }
   }

   public void onMouseDown(MouseDownEvent event)
   {
      resizing_ = true;
      Event.setCapture(splitter_.getElement());
      event.preventDefault();
      event.stopPropagation();
      fireEvent(new SplitterBeforeResizeEvent());
   }

   public void onMouseMove(MouseMoveEvent event)
   {
      if (event.getNativeButton() == 0)
         resizing_ = false;

      if (!resizing_)
         return;

      event.preventDefault();
      event.stopPropagation();
      if (topIsFixed_)
         setSplitterPos(event.getRelativeY(getElement()), true);
      else
         setSplitterPos(getOffsetHeight() - event.getRelativeY(getElement()),
                        false);
   }

   public void onMouseUp(MouseUpEvent event)
   {
      if (resizing_)
      {
         resizing_ = false;
         Event.releaseCapture(splitter_.getElement());
         fireEvent(new SplitterResizedEvent());
      }
   }

   public void onKeyDown(KeyDownEvent event)
   {
      if (!isVisible())
         return;

      int delta = 0;
      switch (event.getNativeKeyCode())
      {
         case KeyCodes.KEY_UP:
            delta = KEYBOARD_MOVE_SIZE;
            break;

         case KeyCodes.KEY_DOWN:
            delta = -KEYBOARD_MOVE_SIZE;
            break;
      }
      if (delta == 0)
         return;

      event.preventDefault();
      event.stopPropagation();

      // use Shift key with arrows to make small adjustments
      if (event.getNativeEvent().getShiftKey())
         delta = delta < 0 ? -1 : 1; 
      fireEvent(new SplitterBeforeResizeEvent());
      setSplitterPos(splitterPos_ + delta, topIsFixed_);
      fireEvent(new SplitterResizedEvent());
   }

   public void onBlur(BlurEvent event)
   {
      splitter_.removeStyleDependentName("focused");
   }

   public void onFocus(FocusEvent event)
   {
      splitter_.addStyleDependentName("focused");
   }

   public void focusSplitter()
   {
      if (isSplitterVisible())
         splitter_.getElement().focus();
   }  

   public int getSplitterHeight()
   {
      return splitterHeight_;
   }

   private int top_;
   private int bottom_;

   private HTML splitter_;
   private int splitterPos_;
   private int splitterHeight_;
   // If true, then bottom widget should scale and top widget should stay
   // fixed. If false, then vice versa.
   private boolean topIsFixed_ = true;
   private Widget[] widgets_;
   private boolean resizing_;
   private int offsetHeight_;
}
