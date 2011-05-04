/*
 * StatusBarElementWidget.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.status;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuItem;
import org.rstudio.core.client.widget.ToolbarPopupMenu;

import java.util.ArrayList;

public class StatusBarElementWidget extends FlowPanel
      implements StatusBarElement, HasSelectionHandlers<String>
{
   interface Resources extends ClientBundle
   {
      ImageResource upDownArrow();
      ImageResource statusBarSeparator();
   }

   public StatusBarElementWidget()
   {
      options_ = new ArrayList<String>();
      label_ = new Label();
      add(label_);

      addDomHandler(new MouseDownHandler()
      {
         public void onMouseDown(MouseDownEvent event)
         {
            event.preventDefault();
            event.stopPropagation();

            if (options_.size() == 0)
               return;

            ToolbarPopupMenu menu = new ToolbarPopupMenu();
            menu.getElement().getStyle().setFontSize(10, Unit.PX);
            for (final String option : options_)
               menu.addItem(new MenuItem(option, new Command()
               {
                  public void execute()
                  {
                     SelectionEvent.fire(StatusBarElementWidget.this, option);
                  }
               }));
            menu.showRelativeTo(label_);
         }
      }, MouseDownEvent.getType());
   }

   public void setValue(String value)
   {
      label_.setText(value);
   }

   public String getValue()
   {
      return label_.getText();
   }

   public void addOptionValue(String option)
   {
      options_.add(option);
   }

   public void clearOptions()
   {
      options_.clear();
   }

   public void setShowArrows(boolean showArrows)
   {
      if (showArrows ^ arrows_ != null)
      {
         if (showArrows)
         {
            Resources res = GWT.create(Resources.class);
            arrows_ = new Image(res.upDownArrow());
            add(arrows_);
         }
         else
         {
            arrows_.removeFromParent();
            arrows_ = null;
         }
      }
   }

   public void setShowSeparator(boolean showSeparator)
   {
      if (showSeparator ^ separator_ != null)
      {
         if (showSeparator)
         {
            Resources res = GWT.create(Resources.class);
            separator_ = new Image(res.statusBarSeparator());
            add(separator_);
         }
         else
         {
            separator_.removeFromParent();
            separator_ = null;
         }
      }
   }

   public String getText()
   {
      return label_.getText();
   }

   public void setText(String s)
   {
      label_.setText(s);
   }

   public HandlerRegistration addSelectionHandler(SelectionHandler<String> handler)
   {
      return addHandler(handler, SelectionEvent.getType());
   }

   public HandlerRegistration addMouseDownHandler(MouseDownHandler handler)
   {
      return addDomHandler(handler, MouseDownEvent.getType());
   }

   private final ArrayList<String> options_;
   private final Label label_;
   private Image arrows_;
   private Image separator_;
}
