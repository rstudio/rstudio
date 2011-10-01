/*
 * MultiSelectCellTable.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.NodePredicate;

public class MultiSelectCellTable<T> extends CellTable<T>
      implements HasKeyDownHandlers
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

   private void commonInit()
   {
      setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
      getElement().setTabIndex(-1);

      addDomHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            DomUtils.focus(getElement(), false);
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
   }

   @Override
   protected boolean isKeyboardNavigationSuppressed()
   {
      return true;
   }

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
            if (getVisibleItemCount() == 0)
               return;
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

            switch (event.getNativeKeyCode())
            {
               case KeyCodes.KEY_UP:
               {
                  int row = Math.max(0, min - 1);
                  if (modifiers != KeyboardShortcut.SHIFT)
                     clearSelection();
                  getSelectionModel().setSelected(getVisibleItem(row), true);
                  // TODO: scroll into view
                  break;
               }
               case KeyCodes.KEY_DOWN:
               {
                  int row = Math.min(getVisibleItemCount()-1, max + 1);
                  if (modifiers != KeyboardShortcut.SHIFT)
                     clearSelection();
                  getSelectionModel().setSelected(getVisibleItem(row), true);
                  // TODO: scroll into view
                  break;
               }
            }
            break;
      }

   }

   @Override
   public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
   {
      return addDomHandler(handler, KeyDownEvent.getType());
   }
}
