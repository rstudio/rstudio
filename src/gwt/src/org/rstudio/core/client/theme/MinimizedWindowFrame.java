/*
 * MinimizedWindowFrame.java
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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.events.HasWindowStateChangeHandlers;
import org.rstudio.core.client.events.WindowStateChangeEvent;
import org.rstudio.core.client.events.WindowStateChangeHandler;
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

   public MinimizedWindowFrame(String title)
   {
      this(title, null);
   }

   public MinimizedWindowFrame(String title, Widget extraWidget)
   {
      ThemeStyles themeStyles = ThemeResources.INSTANCE.themeStyles();

      layout_ = new ClickDockLayoutPanel(Style.Unit.PX);
      layout_.setStylePrimaryName(themeStyles.minimizedWindow());
      layout_.addStyleName(themeStyles.minimizedWindowObject());

      int leftPadding = title != null ? 8 : 4;
      layout_.addWest(createDiv(themeStyles.left()), leftPadding);
      layout_.addEast(createDiv(themeStyles.right()), 8);

      HorizontalPanel inner = new HorizontalPanel();
      inner.setWidth("100%");
      inner.setStylePrimaryName(themeStyles.center());

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

      HTML minimize = createDiv(themeStyles.minimize() + " " +
                                ThemeStyles.INSTANCE.handCursor());
      minimize.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            event.preventDefault();
            event.stopPropagation();
            fireEvent(new WindowStateChangeEvent(WindowState.MINIMIZE));
         }
      });
      inner.add(minimize);

      HTML maximize = createDiv(themeStyles.maximize() + " " + 
                                themeStyles.handCursor());
      maximize.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            event.preventDefault();
            event.stopPropagation();
            fireEvent(new WindowStateChangeEvent(WindowState.MAXIMIZE));
         }
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
         WindowStateChangeHandler handler)
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
