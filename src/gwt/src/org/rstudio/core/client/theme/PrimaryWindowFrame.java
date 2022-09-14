/*
 * PrimaryWindowFrame.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.theme;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.events.WindowStateChangeEvent;
import org.rstudio.core.client.layout.WindowState;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.DoubleClickState;

public class PrimaryWindowFrame extends WindowFrame
{
   private static class ClickFlowPanel extends FlowPanel implements
                                                         HasClickHandlers,
                                                         HasMouseDownHandlers
   {
      public HandlerRegistration addClickHandler(ClickHandler handler)
      {
         return addDomHandler(handler, ClickEvent.getType());
      }

      public HandlerRegistration addMouseDownHandler(MouseDownHandler handler)
      {
         return addDomHandler(handler, MouseDownEvent.getType());
      }
   }

   public PrimaryWindowFrame(String title,
                             Widget mainWidget)
   {
      super(title, title);
      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();

      panel_ = new ClickFlowPanel();
      panel_.setStylePrimaryName(styles.primaryWindowFrameHeader());

      title_ = new Label(title);
      title_.setStylePrimaryName(styles.title());
      panel_.addMouseDownHandler(new MouseDownHandler()
      {
         public void onMouseDown(MouseDownEvent event)
         {
            event.preventDefault();
         }
      });
      
      panel_.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            focus();
            event.preventDefault();
            event.stopPropagation();

            if (doubleClickState_.checkForDoubleClick(event.getNativeEvent()))
               fireEvent(new WindowStateChangeEvent(WindowState.MAXIMIZE));
         }
      });
      
      separator_ = new HTML("&centerdot;");
      separator_.addStyleName(ThemeStyles.INSTANCE.toolbarDotSeparator());

      subtitle_ = new Label();
      subtitle_.setStylePrimaryName(styles.subtitle());

      panel_.add(title_);
      panel_.add(separator_);
      panel_.add(subtitle_);

      setHeaderWidget(panel_);

      setMainWidget(mainWidget);
   }
   
   public void setTitleWidget(Widget title)
   {
      panel_.remove(title_);
      title_ = title;
      panel_.insert(title_, 0);
   }

   public void setSubtitle(String subtitle)
   {
      subtitle_.setText(subtitle);
   }

   public void addLeftWidget(Widget widget)
   {
      panel_.add(widget);
   }

   private final DoubleClickState doubleClickState_ = new DoubleClickState();
   private Widget title_;
   private final HTML separator_;
   private final Label subtitle_;
   private final ClickFlowPanel panel_;
}
