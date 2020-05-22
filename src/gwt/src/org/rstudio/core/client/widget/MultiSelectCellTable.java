/*
 * MultiSelectCellTable.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.dom.DomUtils;

public class MultiSelectCellTable<T> extends CellTable<T>
      implements HasKeyDownHandlers, HasClickHandlers, HasMouseDownHandlers,
                 HasContextMenuHandlers
{
   public MultiSelectCellTable()
   {
      commonInit();
   }

   public MultiSelectCellTable(int pageSize)
   {
      super(pageSize);
      commonInit();
   }

   public MultiSelectCellTable(ProvidesKey<T> keyProvider)
   {
      super(keyProvider);
      commonInit();
   }

   public MultiSelectCellTable(int pageSize, Resources resources)
   {
      super(pageSize, resources);
      commonInit();
   }

   public MultiSelectCellTable(int pageSize, ProvidesKey<T> keyProvider)
   {
      super(pageSize, keyProvider);
      commonInit();
   }

   public MultiSelectCellTable(int pageSize,
                               Resources resources,
                               ProvidesKey<T> keyProvider)
   {
      super(pageSize, resources, keyProvider);
      commonInit();
   }

   public MultiSelectCellTable(int pageSize,
                               Resources resources,
                               ProvidesKey<T> keyProvider,
                               Widget loadingIndicator)
   {
      super(pageSize, resources, keyProvider, loadingIndicator);
      commonInit();
   }

   @Override
   public void setFocus(boolean focused)
   {
      if (focused)
         DomUtils.focus(getElement(), false);
   }

   private void commonInit()
   {
      setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
      getElement().setTabIndex(-1);

      addDomHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            // Note: this formerly used DomUtils.focus, but the implementation
            // of DomUtils used on IE11 focuses the window afterwards, which 
            // blurs the element. Since we don't need to drive selection, we
            // now just use native focus here.
            getElement().focus();
         }
      }, ClickEvent.getType());

      addKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            MultiSelectCellTable.this.handleKeyDown(event);
         }
      });
      
      addDomHandler(new ContextMenuHandler() {
         @Override
         public void onContextMenu(ContextMenuEvent event)
         {
            MultiSelectCellTable.this.handleContextMenu(event);
         }
      }, ContextMenuEvent.getType());
   }

   @Override
   protected boolean isKeyboardNavigationSuppressed()
   {
      return true;
   }

   @SuppressWarnings("rawtypes")
   private void clearSelection()
   {
      if (getSelectionModel() instanceof MultiSelectionModel)
         ((MultiSelectionModel)getSelectionModel()).clear();
   }

   private void handleKeyDown(KeyDownEvent event)
   {
      int modifiers = KeyboardShortcut.getModifierValue(event.getNativeEvent());
      switch (event.getNativeKeyCode())
      {
         // TODO: Handle home/end, pageup/pagedown
         case KeyCodes.KEY_UP:
         case KeyCodes.KEY_DOWN:
            event.preventDefault();
            event.stopPropagation();

            switch (modifiers)
            {
               case 0:
               case KeyboardShortcut.SHIFT:
                  break;
               default:
                  return;
            }

            moveSelection(event.getNativeKeyCode() == KeyCodes.KEY_UP,
                          modifiers == KeyboardShortcut.SHIFT);

            break;
         case 'A':
            if (modifiers == (BrowseCap.hasMetaKey() ? KeyboardShortcut.META
                                                     : KeyboardShortcut.CTRL))
            {
               if (getSelectionModel() instanceof MultiSelectionModel)
               {
                  event.preventDefault();
                  event.stopPropagation();

                  for (T item : getVisibleItems())
                     getSelectionModel().setSelected(item, true);
               }
            }
            break;
      }

   }

   
   // handle context-menu clicks. this implementation (specifically the 
   // detection of table rows from dom events) is based on the code in
   // AbstractCellTable.onBrowserEvent2. we first determine if the click
   // applies to a row in the table -- if it does then we squelch the 
   // standard handling of the event and update the selection and then 
   // forward the event on to any external listeners
   private void handleContextMenu(ContextMenuEvent cmEvent)
   {
      // bail if there are no context menu handlers
      if (handlerManager_.getHandlerCount(ContextMenuEvent.getType()) == 0)
         return;
       
      // Get the event target.
      NativeEvent event = cmEvent.getNativeEvent();
      EventTarget eventTarget = event.getEventTarget();
      if (!Element.is(eventTarget))
        return;
      final Element target = event.getEventTarget().cast();

      // always squelch default handling (when there is a handler)
      event.stopPropagation();
      event.preventDefault();
      
      // find the table cell element then get its parent and cast to row
      TableCellElement tableCell = findNearestParentCell(target);
      if (tableCell == null)
         return;         
      Element trElem = tableCell.getParentElement();
      if (trElem == null) 
        return;
      TableRowElement tr = TableRowElement.as(trElem);
      
      // get the section of the row and confirm it is a tbody (as opposed
      // to a thead or tfoot)
      Element sectionElem = tr.getParentElement();
      if (sectionElem == null)
        return;
      TableSectionElement section = TableSectionElement.as(sectionElem);
      if (section != getTableBodyElement())
         return; 
      
      // determine the row/item target
      int row = tr.getSectionRowIndex();
      T item = getVisibleItem(row);
      
      // if this row isn't already selected then clear the existing selection
      if (!getSelectionModel().isSelected(item))
         clearSelection();
     
      // select the clicked on item
      getSelectionModel().setSelected(item, true);
      
      // forward the event
      DomEvent.fireNativeEvent(event, handlerManager_);
   }
   
   // forked from private AbstractCellTable.findNearestParentCell
   private TableCellElement findNearestParentCell(Element elem) {
      while ((elem != null) && (elem != getElement())) {
        // TODO: We need is() implementations in all Element subclasses.
        // This would allow us to use TableCellElement.is() -- much cleaner.
        String tagName = elem.getTagName();
        if ("td".equalsIgnoreCase(tagName) || "th".equalsIgnoreCase(tagName)) {
          return elem.cast();
        }
        elem = elem.getParentElement();
      }
      return null;
   }
  
   @Override
   public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
   {
      return addDomHandler(handler, KeyDownEvent.getType());
   }

   public void moveSelection(boolean up, boolean extend)
   {
      if (getVisibleItemCount() == 0)
         return;

      int min = getVisibleItemCount();
      int max = -1;

      for (int i = 0; i < getVisibleItemCount(); i++)
      {
         if (getSelectionModel().isSelected(getVisibleItem(i)))
         {
            max = i;
            if (min > i)
               min = i;
         }
      }

      if (up)
      {
         int row = Math.max(0, min - 1);
         if (!canSelectVisibleRow(row))
            row = min;

         if (!extend)
            clearSelection();
         getSelectionModel().setSelected(getVisibleItem(row), true);
         ensureRowVisible(row, true);
      }
      else
      {
         int row = Math.min(getVisibleItemCount()-1, max + 1);
         if (!canSelectVisibleRow(row))
            row = max;

         if (!extend)
            clearSelection();
         getSelectionModel().setSelected(getVisibleItem(row), true);
         ensureRowVisible(row, false);
      }
   }

   private void ensureRowVisible(int row, boolean alignWithTop)
   {
      Element el;
      if (row == 0 && alignWithTop)
         el = (getElement().<TableElement>cast()).getRows().getItem(0);
      else
         el = getRowElement(row);

      if (el != null)
         DomUtils.scrollIntoViewVert(el);
   }

   protected boolean canSelectVisibleRow(int visibleRow)
   {
      return true;
   }

   @Override
   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return addDomHandler(handler, ClickEvent.getType());
   }

   @Override
   public HandlerRegistration addMouseDownHandler(MouseDownHandler handler)
   {
      return addDomHandler(handler, MouseDownEvent.getType());
   }
   
   @Override
   public HandlerRegistration addContextMenuHandler(ContextMenuHandler handler)
   {
      return handlerManager_.addHandler(ContextMenuEvent.getType(), handler);
   }
   
   // we have our own HandlerManager so that we can fire the ContextMenuEvent
   // to listeners after we have done our own handling of it (specifically
   // we need to nix the browser context menu and update the selection). 
   private final HandlerManager handlerManager_ = new HandlerManager(this);
}
