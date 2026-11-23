/*
 * WindowFrame.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.theme;

import org.rstudio.core.client.ClassIds;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.events.EnsureHeightEvent;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.HasEnsureHeightHandlers;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;
import org.rstudio.core.client.events.HasWindowStateChangeHandlers;
import org.rstudio.core.client.events.WindowEnsureVisibleEvent;
import org.rstudio.core.client.events.WindowStateChangeEvent;
import org.rstudio.core.client.layout.RequiresVisibilityChanged;
import org.rstudio.core.client.layout.WindowState;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.BeforeShowCallback;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class WindowFrame extends Composite
   implements HasWindowStateChangeHandlers,
              ProvidesResize,
              RequiresResize,
              EnsureVisibleEvent.Handler,
              EnsureHeightEvent.Handler
{
   public WindowFrame(String name, String accessibleName)
   {
      this(name, accessibleName, true, true);
   }

   public WindowFrame(String name, String accessibleName, boolean showMinMaxButtons)
   {
      this(name, accessibleName, showMinMaxButtons, showMinMaxButtons);
   }

   public WindowFrame(String name, String accessibleName, boolean showMaximizeButton, boolean showMinimizeButton)
   {
      this(name, accessibleName, showMaximizeButton, showMinimizeButton, false);
   }

   public WindowFrame(String name, String accessibleName,
                      boolean showMaximizeButton, boolean showMinimizeButton,
                      boolean showCloseButton)
   {
      name_ = name;

      RStudioGinjector.INSTANCE.injectMembers(this);

      final ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();

      maximizeButton_ = new WindowFrameButton(accessibleName, WindowState.MAXIMIZE);
      maximizeButton_.setClassId(ClassIds.PANEL_MAX_BTN, name);
      maximizeButton_.setStylePrimaryName(styles.maximize());
      maximizeButton_.setClickHandler(() -> maximize());

      minimizeButton_ = new WindowFrameButton(accessibleName, WindowState.MINIMIZE);
      minimizeButton_.setClassId(ClassIds.PANEL_MIN_BTN, name);
      minimizeButton_.setStylePrimaryName(styles.minimize());
      minimizeButton_.setClickHandler(() -> minimize());

      closeButton_ = new WindowFrameButton(accessibleName, WindowState.HIDE);
      closeButton_.setClassId(ClassIds.PANEL_CLOSE_BTN, name);
      closeButton_.setStylePrimaryName(styles.close());

      frame_ = new LayoutPanel();
      frame_.getElement().setId(ElementIds.getUniqueElementId(name + "_pane"));
      Roles.getRegionRole().set(frame_.getElement());
      Roles.getRegionRole().setAriaLabelProperty(frame_.getElement(), accessibleName);
      frame_.setStylePrimaryName(styles.windowframe());
      frame_.addStyleName(styles.windowFrameObject());

      if (showMinimizeButton)
         addFrameButton(minimizeButton_, 1);

      // When a close button is shown, shift maximize left to make room
      if (showMaximizeButton)
         addFrameButton(maximizeButton_, showCloseButton ? 1 : 0);

      if (showCloseButton)
         addFrameButton(closeButton_, 0);

      initWidget(frame_);
   }

   /**
    * Places a title bar button in a slot counted from the right edge.
    */
   private void addFrameButton(WindowFrameButton button, int slot)
   {
      frame_.add(button);
      frame_.setWidgetTopHeight(button,
                                FRAME_BUTTON_TOP, Style.Unit.PX,
                                FRAME_BUTTON_SIZE, Style.Unit.PX);
      frame_.setWidgetRightWidth(button,
                                 FRAME_BUTTON_RIGHT + slot * FRAME_BUTTON_SIZE, Style.Unit.PX,
                                 FRAME_BUTTON_SIZE, Style.Unit.PX);
   }

   public String getName()
   {
      return name_;
   }

   /**
    * Replace what the frame's maximize gestures do. The action can call
    * maximizeDefault() to get the built-in behavior. Pass null to restore it.
    */
   public void setMaximizeAction(Command action)
   {
      maximizeAction_ = action;
   }

   public void setCloseClickHandler(Command handler)
   {
      closeButton_.setClickHandler(handler);
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

   /**
    * The frame's maximize gesture. Four routes come here:
    *
    *  - the maximize button
    *  - a double-click on the Console title bar (PrimaryWindowFrame)
    *  - a double-click on a tabset's tab bar (ModuleTabLayoutPanel)
    *  - EnsureHeightEvent.NORMAL on a zoomed (EXCLUSIVE) window, where the
    *    gesture means "restore" (LogicalWindow.onEnsureHeight)
    *
    * So an owner intercepts all of them in one place with setMaximizeAction,
    * instead of one route at a time.
    *
    * Two other paths reach WindowState.MAXIMIZE without the frame, both
    * firing at the LogicalWindow: the button on MinimizedWindowFrame, and
    * EnsureHeightEvent.MAXIMIZED on a non-zoomed window -- deliberately, as
    * that is a vertical request and an owner's maximize action may be
    * horizontal (the sidebar's is layoutZoomSidebar). See
    * PaneManager.endZoomIfActive.
    */
   public void maximize()
   {
      if (maximizeAction_ != null)
         maximizeAction_.execute();
      else
         maximizeDefault();
   }

   /**
    * The built-in maximize behavior. A custom maximize action can call this.
    */
   public void maximizeDefault()
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
               LEFT_SHADOW_WIDTH, Style.Unit.PX,
               RIGHT_SHADOW_WIDTH, Style.Unit.PX);
         frame_.setWidgetTopBottom(main_,
               CONTENT_REGION_TOP, Style.Unit.PX,
               BOTTOM_SHADOW_WIDTH, Style.Unit.PX);
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
               LEFT_SHADOW_WIDTH, Style.Unit.PX,
               FRAME_BUTTONS_EXTENT, Style.Unit.PX);
         frame_.setWidgetTopHeight(
               header_,
               TOP_SHADOW_WIDTH, Style.Unit.PX,
               TITLEBAR_REGION_BOTTOM, Style.Unit.PX);
      }
   }

   /**
    * Puts a widget in the whole space (includes both header and main content
    * areas--i.e. everything inside the border).
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
               LEFT_SHADOW_WIDTH, Style.Unit.PX,
               RIGHT_SHADOW_WIDTH, Style.Unit.PX);
         frame_.setWidgetTopBottom(fill_,
               TOP_SHADOW_WIDTH, Style.Unit.PX,
               BOTTOM_SHADOW_WIDTH, Style.Unit.PX);
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
      // Check logicalState_ in addition to isVisible() because, during the
      // HIDE animation kicked off by closeAllSourceDocs (LastSourceDocClosedEvent
      // -> PaneManager.closeSourceWindow), the widget is still in the DOM and
      // isVisible() returns true even though the logical state is already HIDE.
      // Without this, a file-open arriving during that window silently lands in
      // a pane that finishes hiding.
      if (!isVisible() ||
          logicalState_ == WindowState.HIDE ||
          logicalState_ == WindowState.MINIMIZE)
      {
         fireEvent(WindowStateChangeEvent.forEnsureVisible());
      }
      else
      {
         fireEvent(new EnsureVisibleEvent(event.getActivate()));
      }

      events_.fireEvent(new WindowEnsureVisibleEvent(this));
   }

   /**
    * Handlers run when a tab asks to be visible and the frame is already
    * showing, so no state change was needed to satisfy it.
    */
   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleEvent.Handler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }

   public void setLogicalState(WindowState state)
   {
      logicalState_ = state;
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
   private final WindowFrameButton maximizeButton_;
   private final WindowFrameButton minimizeButton_;
   private final WindowFrameButton closeButton_;
   private Widget main_;
   private Widget header_;
   private Widget fill_;
   private HandlerRegistration ensureVisibleRegistration_;
   private HandlerRegistration ensureHeightRegistration_;
   private Widget previousHeader_;
   private WindowState logicalState_ = WindowState.NORMAL;
   private Command maximizeAction_;

   private static final int TOP_SHADOW_WIDTH = 3,
                            LEFT_SHADOW_WIDTH = 3,
                            RIGHT_SHADOW_WIDTH = 3,
                            BOTTOM_SHADOW_WIDTH = 5,
                            TITLEBAR_REGION_BOTTOM = 26,
                            CONTENT_REGION_TOP = 26;

   // Title bar buttons are 24px squares, the WCAG 2.5.8 minimum target size,
   // laid out right to left in slots; CSS centers each 14px icon in its square.
   private static final int FRAME_BUTTON_SIZE = 24,
                            FRAME_BUTTON_TOP = 2,
                            FRAME_BUTTON_RIGHT = RIGHT_SHADOW_WIDTH + 2,
                            FRAME_BUTTONS_EXTENT = FRAME_BUTTON_RIGHT + 2 * FRAME_BUTTON_SIZE;
   // Injected ----
   private EventBus events_;
   String name_;

}
