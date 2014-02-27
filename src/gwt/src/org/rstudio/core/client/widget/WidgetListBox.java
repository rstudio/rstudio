/*
 * WidgetListBox.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.core.client.widget;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

// A list box that can contain arbitrary GWT widgets as options.
public class WidgetListBox extends FocusPanel implements HasChangeHandlers
{
   private class ClickableHTMLPanel extends HTMLPanel
      implements HasClickHandlers
   {
      public ClickableHTMLPanel()
      {
         super("");
      }

      public HandlerRegistration addClickHandler(ClickHandler handler)
      {
         return addDomHandler(handler, ClickEvent.getType());
      }
   }

   public interface ListStyle extends CssResource
   {
      String selectedItem();
      String anyItem();
      String listPanel();
      String outerPanel();
   }

   public interface Resources extends ClientBundle
   {
      @Source("WidgetListBox.css")
      ListStyle listStyle();
   }
   
   public WidgetListBox()
   {
      super();
      resources_ = GWT.create(Resources.class);
      style_ = resources_.listStyle();
      style_.ensureInjected();
      panel_ = new FlowPanel();
      add(panel_);
      panel_.addStyleName(style_.listPanel());
      addStyleName(style_.outerPanel());
      addKeyPressHandler(new KeyPressHandler()
      {
         @Override
         public void onKeyPress(KeyPressEvent event)
         {
            if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_DOWN &&
                selectedIdx_ < maxIdx_)
            {
               setSelectedIndex(selectedIdx_+1, true);
            }
            else if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_UP &
                selectedIdx_ > 0)
            {
               setSelectedIndex(selectedIdx_-1, true);
            }
         }
      });
   }

   @Override
   public HandlerRegistration addChangeHandler(final ChangeHandler handler)
   {
      return handlerManager_.addHandler(ChangeEvent.getType(), handler);    
   }
   
   public <T extends IsWidget> void addItem(T item)
   {
      // wrap the widget in a panel that can receive click events, indicate
      // selection, etc.
      final Widget w = item.asWidget();
      final int itemIdx = maxIdx_++;
      ClickableHTMLPanel panel = new ClickableHTMLPanel();
      panel.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            setSelectedIndex(itemIdx, true);
         }
      });
      
      panel.add(w);

      // add the panel to our root layout panel
      options_.add(panel);
      panel_.add(panel);
      
      panel.addStyleName(style_.anyItem());

      // if it's the first item, select it
      if (options_.size() == 1)
         setSelectedIndex(0);
   }
   
   public void setSelectedIndex(int itemIdx)
   {
      setSelectedIndex(itemIdx, false);
   }

   private void setSelectedIndex(int itemIdx, boolean fireEvent)
   {
      String selectedStyle = resources_.listStyle().selectedItem();
      options_.get(selectedIdx_).removeStyleName(selectedStyle);
      selectedIdx_ = itemIdx;
      options_.get(selectedIdx_).addStyleName(selectedStyle);
      if (fireEvent)
      {
         DomEvent.fireNativeEvent(Document.get().createChangeEvent(),
                                  handlerManager_);
      }
   }

   public int getSelectedIndex()
   {
      return selectedIdx_;
   }
   
   private int maxIdx_ = 0;
   private int selectedIdx_ = 0;

   private FlowPanel panel_;
   private List<Widget> options_ = new ArrayList<Widget>();

   private Resources resources_;
   private ListStyle style_;

   HandlerManager handlerManager_ = new HandlerManager(this);
}
