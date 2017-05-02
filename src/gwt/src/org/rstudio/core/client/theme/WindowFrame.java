/*
 * WindowFrame.java
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
package org.rstudio.core.client.theme;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;

import java.util.HashMap;

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
              EnsureVisibleHandler,
              EnsureHeightHandler
{  
   public WindowFrame(Widget mainWidget)
   {
      this();
      setMainWidget(mainWidget);
   }
   
   public WindowFrame()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      final ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();

      border_ = new ShadowBorder();
      border_.setSize("100%", "100%");

      borderPositioner_ = new SimplePanel();
      borderPositioner_.add(border_);

      HTML maximize = new HTML();
      maximize.setStylePrimaryName(styles.maximize());
      maximize.addStyleName(ThemeStyles.INSTANCE.handCursor());
      maximize.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            maximize();
         }
      });

      HTML minimize = new HTML();
      minimize.setStylePrimaryName(styles.minimize());
      minimize.addStyleName(ThemeStyles.INSTANCE.handCursor());
      minimize.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            minimize();
         }
      });

      frame_ = new LayoutPanel();
      frame_.setStylePrimaryName(styles.windowframe());
      frame_.addStyleName(styles.windowFrameObject());

      frame_.add(borderPositioner_);
      frame_.setWidgetTopBottom(borderPositioner_, 0, Style.Unit.PX,
                                                   0, Style.Unit.PX);
      frame_.setWidgetLeftRight(borderPositioner_, 0, Style.Unit.PX,
                                                   0, Style.Unit.PX);

      frame_.add(maximize);
      frame_.setWidgetTopHeight(maximize,
                                ShadowBorder.TOP_SHADOW_WIDTH + 4, Style.Unit.PX,
                                14, Style.Unit.PX);
      frame_.setWidgetRightWidth(maximize,
                                 ShadowBorder.RIGHT_SHADOW_WIDTH + 7, Style.Unit.PX,
                                 14, Style.Unit.PX);

      frame_.add(minimize);
      frame_.setWidgetTopHeight(minimize,
                                ShadowBorder.TOP_SHADOW_WIDTH + 4, Style.Unit.PX,
                                14, Style.Unit.PX);
      frame_.setWidgetRightWidth(minimize,
                                 ShadowBorder.RIGHT_SHADOW_WIDTH + 25, Style.Unit.PX,
                                 14, Style.Unit.PX);
      
      buttonsArea_ = new FlowPanel();
      frame_.add(buttonsArea_);
      
      initWidget(frame_);
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
         WindowStateChangeHandler handler)
   {
      return addHandler(handler, WindowStateChangeEvent.TYPE);
   }
   
   public HandlerRegistration addEnsureHeightHandler(
         EnsureHeightHandler handler)
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
            ensureHeightRegistration_ =
               ((HasEnsureHeightHandlers)main_).addEnsureHeightHandler(this);
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

   private final LayoutPanel frame_;
   private final ShadowBorder border_;
   private final SimplePanel borderPositioner_;
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
   
}
