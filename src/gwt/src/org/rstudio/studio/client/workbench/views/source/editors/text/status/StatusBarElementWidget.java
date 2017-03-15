/*
 * StatusBarElementWidget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.status;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
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

import java.util.ArrayList;

import org.rstudio.core.client.resources.ImageResource2x;

public class StatusBarElementWidget extends FlowPanel
      implements StatusBarElement, HasSelectionHandlers<String>
{
   interface Resources extends ClientBundle
   {
      @Source("upDownArrow_2x.png")
      ImageResource upDownArrow2x();
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

            StatusBarPopupMenu menu = new StatusBarPopupMenu();
            for (final String option : options_)
               menu.addItem(new MenuItem(option, new Command()
               {
                  public void execute()
                  {
                     SelectionEvent.fire(StatusBarElementWidget.this, option);
                  }
               }));
            menu.showRelativeToUpward(label_, 
                  popupAlignment_ == POPUP_ALIGNMENT_RIGHT);
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

   public void click()
   {
      NativeEvent evt = Document.get().createMouseDownEvent(0, 0, 0, 0, 0,
                                                            false, false,
                                                            false, false, 0);
      ClickEvent.fireNativeEvent(evt, this);
   }
   
   public void setPopupAlignment(String alignment)
   {
      popupAlignment_ = alignment;
   }

   public void setShowArrows(boolean showArrows)
   {
      if (showArrows ^ arrows_ != null)
      {
         if (showArrows)
         {
            Resources res = GWT.create(Resources.class);
            arrows_ = new Image(new ImageResource2x(res.upDownArrow2x()));
            add(arrows_);
         }
         else
         {
            arrows_.removeFromParent();
            arrows_ = null;
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

   public HandlerRegistration addMouseDownHandler(final MouseDownHandler handler)
   {
      return addDomHandler(new MouseDownHandler()
      {
         @Override
         public void onMouseDown(MouseDownEvent event)
         {
            if (clicksEnabled_)
               handler.onMouseDown(event);
         }
      }, MouseDownEvent.getType());
   }

   public void setContentsVisible(boolean visible)
   {
      label_.setVisible(visible);
      if (arrows_ != null)
         arrows_.setVisible(visible);
   }

   public void setClicksEnabled(boolean enabled)
   {
      clicksEnabled_ = enabled;
   }

   private final ArrayList<String> options_;
   private final Label label_;
   private Image arrows_;
   private boolean clicksEnabled_ = true;
   private String popupAlignment_ = POPUP_ALIGNMENT_LEFT;
         
   public final static String POPUP_ALIGNMENT_LEFT = "left";
   public final static String POPUP_ALIGNMENT_RIGHT = "right";
}
