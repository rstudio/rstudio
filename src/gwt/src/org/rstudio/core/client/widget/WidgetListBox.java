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

import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.theme.res.ThemeResources;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

// A list box that can contain arbitrary GWT widgets as options.
public class WidgetListBox<T extends Widget> 
   extends FocusPanel 
   implements HasChangeHandlers,
              HasSelectionCommitHandlers<T>
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
      String scrollPanel();
      String outerPanel();
      String emptyMessage();
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

      // add styles to our own widget
      addStyleName(style_.outerPanel());

      // create the panel that will host the widgets
      panel_ = new VerticalPanel();
      panel_.addStyleName(style_.listPanel());

      // wrap in a scroll panel
      scrollPanel_ = new ScrollPanel();
      scrollPanel_.add(panel_);
      scrollPanel_.addStyleName(style_.scrollPanel());
      add(scrollPanel_);
     
      emptyTextLabel_ = new Label();
      emptyTextLabel_.addStyleName(style_.scrollPanel());
      emptyTextLabel_.addStyleName(style_.emptyMessage());
      
      addKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            if (event.getNativeKeyCode() == KeyCodes.KEY_DOWN &&
                selectedIdx_ < (panel_.getWidgetCount() - 1))
            {
               setSelectedIndex(selectedIdx_+1, true);
            }
            else if (event.getNativeKeyCode() == KeyCodes.KEY_UP &&
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
   

   @Override
   public HandlerRegistration addSelectionCommitHandler(
         SelectionCommitHandler<T> handler)
   {
      return addHandler(handler, SelectionCommitEvent.getType());
   }
   
   public void addItem(T item)
   {
      addItem(item, true);
   }
   
   public void addItem(final T item, boolean atEnd)
   {
      // wrap the widget in a panel that can receive click events, indicate
      // selection, etc.
      final ClickableHTMLPanel panel = new ClickableHTMLPanel();
      panel.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            setSelectedIndex(panel_.getWidgetIndex(panel), true);
         }
      });
      
      panel.addDomHandler(new DoubleClickHandler()
      {
         @Override
         public void onDoubleClick(DoubleClickEvent event)
         {
            SelectionCommitEvent.fire(WidgetListBox.this, item);
         }
      }, DoubleClickEvent.getType());
      
      panel.add(item);

      // add the panel to our root layout panel
      if (!atEnd && panel_.getWidgetCount() > 0)
      {
         panel_.insert(panel, 0);
         items_.add(0, item);
         options_.add(0, panel);
         selectedIdx_++;
      }
      else
      {
         panel_.add(panel);
         items_.add(item);
         options_.add(panel);
      }

      panel.getElement().getStyle().setPadding(itemPaddingValue_, 
                                               itemPaddingUnit_);
      
      panel.addStyleName(style_.anyItem());
      panel.addStyleName(ThemeResources.INSTANCE.themeStyles().handCursor());

      // if it's the first item, select it
      if (options_.size() == 1)
         setSelectedIndex(0);
      else if (!atEnd && getSelectedIndex() == 1 && options_.size() > 1)
         setSelectedIndex(0, true);
      
      updateEmptyText();
   }
   
   public void setSelectedIndex(int itemIdx)
   {
      setSelectedIndex(itemIdx, false);
   }

   public void setSelectedIndex(int itemIdx, boolean fireEvent)
   {
      String selectedStyle = resources_.listStyle().selectedItem();
      panel_.getWidget(selectedIdx_).removeStyleName(selectedStyle);
      selectedIdx_ = itemIdx;
      panel_.getWidget(selectedIdx_).addStyleName(selectedStyle);
      panel_.getWidget(selectedIdx_).getElement().scrollIntoView();
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
   
   public T getItemAtIdx(int idx)
   {
      if (idx < items_.size())
      {
         return items_.get(idx);
      }
      return null;
   }
   
   public List<T> getItems()
   {
      return items_;
   }
   
   public T getSelectedItem()
   {
      return getItemAtIdx(getSelectedIndex());
   }
   
   public int getItemCount()
   {
      return items_.size();
   }
   
   public void setItemPadding(double val, Style.Unit unit)
   {
      itemPaddingValue_ = val;
      itemPaddingUnit_ = unit;
   }
   
   public void clearItems()
   {
      panel_.clear();
      options_.clear();
      items_.clear();
      selectedIdx_ = 0;
      updateEmptyText();
   }
   
   public void setEmptyText(String text)
   {
      emptyTextLabel_.setText(text);
      updateEmptyText();
   }
   
   private void updateEmptyText()
   {
      if (emptyTextLabel_.getParent() == this && items_.size() > 0)
      {
         clear();
         add(scrollPanel_);
      }
      else if (emptyTextLabel_.getParent() != this && items_.size() == 0)
      {
         clear();
         add(emptyTextLabel_);
      }
   }
   
   private int selectedIdx_ = 0;

   private ScrollPanel scrollPanel_;
   private VerticalPanel panel_;
   private Label emptyTextLabel_;
   private List<HTMLPanel> options_ = new ArrayList<HTMLPanel>();
   private List<T> items_ = new ArrayList<T>();

   private Resources resources_;
   private ListStyle style_;
   
   private double itemPaddingValue_ = 5;
   private Style.Unit itemPaddingUnit_ = Style.Unit.PX;

   HandlerManager handlerManager_ = new HandlerManager(this);
}
