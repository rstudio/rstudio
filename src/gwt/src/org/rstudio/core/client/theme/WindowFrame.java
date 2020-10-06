/*
 * WindowFrame.java
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
package org.rstudio.core.client.theme;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;

import java.util.HashMap;

import org.rstudio.core.client.ClassIds;
import org.rstudio.core.client.events.*;
import org.rstudio.core.client.layout.RequiresVisibilityChanged;
import org.rstudio.core.client.layout.WindowState;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.BeforeShowCallback;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;

public class WindowFrame extends Composite
   implements HasWindowStateChangeHandlers,
              ProvidesResize,
              RequiresResize,
              EnsureVisibleEvent.Handler,
              EnsureHeightEvent.Handler
{
   public WindowFrame(String name, String accessibleName)
   {
      name_ = name;

      RStudioGinjector.INSTANCE.injectMembers(this);

      final ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();

      border_ = new ShadowBorder();
      border_.setSize("100%", "100%");

      borderPositioner_ = new SimplePanel();
      borderPositioner_.add(border_);

      maximizeButton_ = new WindowFrameButton(accessibleName, WindowState.MAXIMIZE);
      maximizeButton_.setClassId(ClassIds.PANEL_MAX_BTN, name);
      maximizeButton_.setStylePrimaryName(styles.maximize());
      maximizeButton_.setClickHandler(() -> maximize());

      minimizeButton_ = new WindowFrameButton(accessibleName, WindowState.MINIMIZE);
      minimizeButton_.setClassId(ClassIds.PANEL_MIN_BTN, name);
      minimizeButton_.setStylePrimaryName(styles.minimize());
      minimizeButton_.setClickHandler(() -> minimize());

      frame_ = new LayoutPanel();
      Roles.getRegionRole().set(frame_.getElement());
      Roles.getRegionRole().setAriaLabelProperty(frame_.getElement(), accessibleName);
      frame_.setStylePrimaryName(styles.windowframe());
      frame_.addStyleName(styles.windowFrameObject());

      frame_.add(borderPositioner_);
      frame_.setWidgetTopBottom(borderPositioner_, 0, Style.Unit.PX,
                                                   0, Style.Unit.PX);
      frame_.setWidgetLeftRight(borderPositioner_, 0, Style.Unit.PX,
                                                   0, Style.Unit.PX);

      frame_.add(minimizeButton_);
      frame_.setWidgetTopHeight(minimizeButton_,
            ShadowBorder.TOP_SHADOW_WIDTH + 4, Style.Unit.PX,
            14, Style.Unit.PX);
      frame_.setWidgetRightWidth(minimizeButton_,
            ShadowBorder.RIGHT_SHADOW_WIDTH + 25, Style.Unit.PX,
            14, Style.Unit.PX);

      frame_.add(maximizeButton_);
      frame_.setWidgetTopHeight(maximizeButton_,
                                ShadowBorder.TOP_SHADOW_WIDTH + 4, Style.Unit.PX,
                                14, Style.Unit.PX);
      frame_.setWidgetRightWidth(maximizeButton_,
                                 ShadowBorder.RIGHT_SHADOW_WIDTH + 7, Style.Unit.PX,
                                 14, Style.Unit.PX);

      buttonsArea_ = new FlowPanel();
      frame_.add(buttonsArea_);

      initWidget(frame_);
   }

   public String getName()
   {
      return name_;
   }

   @Inject
   private void initialize(EventBus events)
   {
      events_ = events;
   }

   @Override
   public void setVisible(boolean visible)
   {
      super.setVisible(visible);

      if (main_ instanceof RequiresVisibilityChanged)
         ((RequiresVisibilityChanged)main_).onVisibilityChanged(visible);
      if (fill_ instanceof RequiresVisibilityChanged)
         ((RequiresVisibilityChanged)fill_).onVisibilityChanged(visible);
      if (header_ instanceof RequiresVisibilityChanged)
         ((RequiresVisibilityChanged)header_).onVisibilityChanged(visible);
   }

   private void maximize()
   {
      fireEvent(new WindowStateChangeEvent(WindowState.MAXIMIZE));
   }

   private void minimize()
   {
      fireEvent(new WindowStateChangeEvent(WindowState.MINIMIZE));
   }

   public HandlerRegistration addWindowStateChangeHandler(
         WindowStateChangeEvent.Handler handler)
   {
      return addHandler(handler, WindowStateChangeEvent.TYPE);
   }

   public HandlerRegistration addEnsureHeightHandler(EnsureHeightEvent.Handler handler)
   {
      return addHandler(handler, EnsureHeightEvent.TYPE);
   }

   /**
    * Puts a widget in the main content area (under the title bar).
    */
   public void setMainWidget(Widget widget)
   {
      if (widget != null)
         setFillWidget(null);
      if (header_ == null)
         setHeaderWidget(previousHeader_);

      if (main_ != null)
      {
         frame_.remove(main_);
         main_ = null;

         if (ensureVisibleRegistration_ != null)
         {
            ensureVisibleRegistration_.removeHandler();
            ensureVisibleRegistration_ = null;
         }

         if (ensureHeightRegistration_ != null)
         {
            ensureHeightRegistration_.removeHandler();
            ensureHeightRegistration_ = null;
         }
      }

      main_ = widget;

      if (main_ != null)
      {
         if (main_ instanceof HasEnsureVisibleHandlers)
         {
            ensureVisibleRegistration_ =
                ((HasEnsureVisibleHandlers)main_).addEnsureVisibleHandler(this);
         }

         if (main_ instanceof HasEnsureHeightHandlers)
         {
            ensureHeightRegistration_ = ((HasEnsureHeightHandlers)main_).addEnsureHeightHandler(this);
         }

         final ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();
         main_.addStyleName(styles.windowFrameWidget());

         frame_.add(main_);
         frame_.setWidgetLeftRight(
               main_,
               ShadowBorder.LEFT_SHADOW_WIDTH, Style.Unit.PX,
               ShadowBorder.RIGHT_SHADOW_WIDTH, Style.Unit.PX);
         frame_.setWidgetTopBottom(main_,
               ShadowBorder.CONTENT_REGION_TOP, Style.Unit.PX,
               ShadowBorder.BOTTOM_SHADOW_WIDTH, Style.Unit.PX);
      }
   }

   /**
    * Puts a widget in the header area (the title bar).
    */
   public void setHeaderWidget(Widget widget)
   {
      if (widget != null)
         setFillWidget(null);

      if (header_ != null)
      {
         frame_.remove(header_);
         previousHeader_ = header_;
         header_ = null;
      }

      header_ = widget;

      if (header_ != null)
      {
         frame_.add(header_);
         frame_.setWidgetLeftRight(
               header_,
               ShadowBorder.LEFT_SHADOW_WIDTH, Style.Unit.PX,
               ShadowBorder.RIGHT_SHADOW_WIDTH + 39, Style.Unit.PX);
         frame_.setWidgetTopHeight(
               header_,
               ShadowBorder.TOP_SHADOW_WIDTH, Style.Unit.PX,
               ShadowBorder.TITLEBAR_REGION_BOTTOM, Style.Unit.PX);
      }
   }

   /**
    * Puts a widget in the whole space (includes both header and main content
    * areas--i.e. everything inside the shadow border).
    */
   public void setFillWidget(Widget widget)
   {
      if (widget != null)
      {
         setHeaderWidget(null);
         setMainWidget(null);
      }

      if (fill_ != null)
      {
         frame_.remove(fill_);
         fill_ = null;

         if (ensureVisibleRegistration_ != null)
         {
            ensureVisibleRegistration_.removeHandler();
            ensureVisibleRegistration_ = null;
         }

         if (ensureHeightRegistration_ != null)
         {
            ensureHeightRegistration_.removeHandler();
            ensureHeightRegistration_ = null;
         }
      }

      fill_ = widget;

      if (fill_ != null)
      {
         if (fill_ instanceof HasEnsureVisibleHandlers)
         {
            ensureVisibleRegistration_ =
                ((HasEnsureVisibleHandlers)fill_).addEnsureVisibleHandler(this);
         }

         if (fill_ instanceof HasEnsureHeightHandlers)
         {
            ensureHeightRegistration_ =
               ((HasEnsureHeightHandlers)fill_).addEnsureHeightHandler(this);
         }

         frame_.add(fill_);
         frame_.setWidgetLeftRight(fill_,
               ShadowBorder.LEFT_SHADOW_WIDTH, Style.Unit.PX,
               ShadowBorder.RIGHT_SHADOW_WIDTH, Style.Unit.PX);
         frame_.setWidgetTopBottom(fill_,
               ShadowBorder.TOP_SHADOW_WIDTH, Style.Unit.PX,
               ShadowBorder.BOTTOM_SHADOW_WIDTH, Style.Unit.PX);
      }
   }

   public void setContextButton(Widget button, int width, int height, int position)
   {
      if (contextButtons_.containsKey(position) && contextButtons_.get(position) != null)
      {
         contextButtons_.get(position).removeFromParent();
         contextButtons_.put(position, null);
      }

      if (button != null)
      {
         contextButtons_.put(position, button);
         button.getElement().getStyle().setFloat(Float.RIGHT);

         buttonsArea_.add(button);
         frame_.setWidgetRightWidth(buttonsArea_, 48, Unit.PX, width * contextButtons_.size(), Unit.PX);
         frame_.setWidgetTopHeight(buttonsArea_, 3, Unit.PX, height, Unit.PX);
         // Without z-index, the header widget will obscure the context button
         // if the former is set after the latter.
         frame_.getWidgetContainerElement(buttonsArea_).getStyle().setZIndex(10);

         button.getElement().setAttribute("display", "inline-block");
         button.getElement().setAttribute("float", "right");
      }
   }

   public void onResize()
   {
      if (frame_ != null)
         frame_.onResize();
   }

   public void focus()
   {
      if (main_ != null)
      {
         if (main_ instanceof CanFocus)
            ((CanFocus)main_).focus();
      }
      else if (fill_ != null)
      {
         if (fill_ instanceof CanFocus)
            ((CanFocus)fill_).focus();
      }
   }

   public void showWindowFocusIndicator(boolean showFocusIndicator)
   {
      final ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();
      if (showFocusIndicator)
         frame_.addStyleName(styles.focusedWindowFrameObject());
      else
         frame_.removeStyleName(styles.focusedWindowFrameObject());
   }

   public void onEnsureVisible(EnsureVisibleEvent event)
   {
      if (!isVisible())
         fireEvent(new WindowStateChangeEvent(WindowState.NORMAL));

      events_.fireEvent(new WindowEnsureVisibleEvent(this));
   }

   @Override
   public void onEnsureHeight(EnsureHeightEvent event)
   {
      fireEvent(event);
   }

   public void onBeforeShow()
   {
      if (main_ instanceof BeforeShowCallback)
         ((BeforeShowCallback)main_).onBeforeShow();
      if (fill_ instanceof BeforeShowCallback)
         ((BeforeShowCallback)fill_).onBeforeShow();
   }

   public Widget getFillWidget()
   {
      return fill_;
   }

   public void setMaximizedDependentState(WindowState state)
   {
      if (state == WindowState.MAXIMIZE)
      {
         addStyleDependentName("maximized");
      }
      else
      {
         removeStyleDependentName("maximized");
      }
      maximizeButton_.setMaximized(state == WindowState.MAXIMIZE);
   }

   public void setExclusiveDependentState(WindowState state)
   {
      if (state == WindowState.EXCLUSIVE)
         addStyleDependentName("exclusive");
      else
         removeStyleDependentName("exclusive");
      maximizeButton_.setExclusive(state == WindowState.EXCLUSIVE);
   }

   private final LayoutPanel frame_;
   private final ShadowBorder border_;
   private final SimplePanel borderPositioner_;
   private final WindowFrameButton maximizeButton_;
   private final WindowFrameButton minimizeButton_;
   private Widget main_;
   private Widget header_;
   private Widget fill_;
   private HashMap<Integer, Widget> contextButtons_ = new HashMap<Integer, Widget>();
   private HandlerRegistration ensureVisibleRegistration_;
   private HandlerRegistration ensureHeightRegistration_;
   private Widget previousHeader_;
   private FlowPanel buttonsArea_;

   // Injected ----
   private EventBus events_;
   String name_;

}
