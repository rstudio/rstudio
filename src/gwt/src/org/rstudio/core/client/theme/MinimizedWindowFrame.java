/*
 * MinimizedWindowFrame.java
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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.ClassIds;
import org.rstudio.core.client.events.HasWindowStateChangeHandlers;
import org.rstudio.core.client.events.WindowStateChangeEvent;
import org.rstudio.core.client.layout.WindowState;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;

public class MinimizedWindowFrame
      extends Composite
      implements HasWindowStateChangeHandlers
{
   private static class ClickDockLayoutPanel extends DockLayoutPanel
   {
      private ClickDockLayoutPanel(Style.Unit unit)
      {
         super(unit);
      }

      public HandlerRegistration addClickHandler(ClickHandler handler)
      {
         return addDomHandler(handler, ClickEvent.getType());
      }
   }

   public MinimizedWindowFrame(String title, String accessibleName)
   {
      this(title, accessibleName, null);
   }

   public MinimizedWindowFrame(String title, String accessibleName, Widget extraWidget)
   {
      ThemeStyles themeStyles = ThemeResources.INSTANCE.themeStyles();

      layout_ = new ClickDockLayoutPanel(Style.Unit.PX);
      layout_.setStylePrimaryName(themeStyles.minimizedWindow());
      layout_.addStyleName(themeStyles.rstheme_minimizedWindowObject());

      Roles.getRegionRole().set(layout_.getElement());
      Roles.getRegionRole().setAriaLabelProperty(layout_.getElement(), accessibleName + " minimized");

      int leftPadding = title != null ? 8 : 4;
      layout_.addWest(createDiv(themeStyles.left()), leftPadding);
      layout_.addEast(createDiv(themeStyles.right()), 8);

      HorizontalPanel inner = new HorizontalPanel();
      inner.setWidth("100%");
      inner.setStylePrimaryName(themeStyles.rstheme_center());

      if (title != null)
      {
         Label titleLabel = new Label(title);
         titleLabel.setStylePrimaryName(themeStyles.title());

         SimplePanel headerPanel = new SimplePanel();
         headerPanel.setStylePrimaryName(themeStyles.primaryWindowFrameHeader());
         headerPanel.setWidget(titleLabel);

         inner.add(headerPanel);
         if (extraWidget == null)
         {
            inner.setCellWidth(headerPanel, "100%");
         }
      }

      if (extraWidget != null)
      {
         extraWidget_ = extraWidget;
         inner.add(extraWidget);
         inner.setCellWidth(extraWidget, "100%");
      }

      WindowFrameButton minimize = new WindowFrameButton(accessibleName, WindowState.NORMAL);
      minimize.setClassId(ClassIds.PANEL_MIN_BTN, accessibleName);
      minimize.setStylePrimaryName(themeStyles.minimize());
      minimize.setClickHandler(() ->
      {
         fireEvent(new WindowStateChangeEvent(WindowState.MINIMIZE));
      });
      inner.add(minimize);

      WindowFrameButton maximize = new WindowFrameButton(accessibleName, WindowState.MAXIMIZE);
      maximize.setClassId(ClassIds.PANEL_MAX_BTN, accessibleName);
      maximize.setStylePrimaryName(themeStyles.maximize());
      maximize.setClickHandler(() ->
      {
         fireEvent(new WindowStateChangeEvent(WindowState.MAXIMIZE));
      });
      inner.add(maximize);

      layout_.add(inner);

      layout_.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            event.preventDefault();
            event.stopPropagation();
            fireEvent(new WindowStateChangeEvent(WindowState.NORMAL));
         }
      });

      initWidget(layout_);
   }

   public void showWindowFocusIndicator(boolean showFocusIndicator)
   {
   }

   protected Widget getExtraWidget()
   {
      return extraWidget_;
   }

   private HTML createDiv(String className)
   {
      HTML html = new HTML();
      html.setStylePrimaryName(className);
      return html;
   }

   public HandlerRegistration addWindowStateChangeHandler(
         WindowStateChangeEvent.Handler handler)
   {
      return addHandler(handler, WindowStateChangeEvent.TYPE);
   }

   public int getDesiredHeight()
   {
      return 30;
   }

   private ClickDockLayoutPanel layout_;
   private Widget extraWidget_;
}
