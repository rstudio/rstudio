/*
 * MinimizedModuleTabLayoutPanel.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.theme;

import java.util.ArrayList;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.aria.client.SelectedValue;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;

/**
 * This is the minimized version of the Data|File|Plot|Package|Help group
 */
public class MinimizedModuleTabLayoutPanel
      extends MinimizedWindowFrame
   implements HasSelectionHandlers<Integer>
{
   public MinimizedModuleTabLayoutPanel(String accessibleName)
   {
      super(null, accessibleName, new FlowPanel());
      addStyleName(ThemeResources.INSTANCE.themeStyles().moduleTabPanel());
      addStyleName(ThemeResources.INSTANCE.themeStyles().minimized());

      // The surrounding region is already named "<pane> minimized"; name the
      // tablist after the pane alone, as the expanded tab strip is.
      FlowPanel tabList = getTabList();
      tabList.addStyleName(ThemeResources.INSTANCE.themeStyles().minimizedTabList());
      Roles.getTablistRole().set(tabList.getElement());
      Roles.getTablistRole().setAriaLabelProperty(tabList.getElement(), accessibleName);
   }

   /**
    * Replace the tabs. Follow with setSelectedIndex(), which marks the
    * selected tab and makes it the strip's tab stop.
    */
   public void setTabs(String[] tabNames)
   {
      FlowPanel tabList = getTabList();
      tabList.clear();
      tabs_.clear();

      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();
      for (int i = 0; i < tabNames.length; i++)
      {
         ModuleTabLayoutPanel.ModuleTab tab
               = new ModuleTabLayoutPanel.ModuleTab(tabNames[i], styles, false, true /*minimized*/);
         tab.addStyleName("gwt-TabLayoutPanelTab");
         tab.getElement().setId(ElementIds.getUniqueElementId(tab.getTabId()));
         Roles.getTabRole().set(tab.getElement());
         final int thisIndex = i;
         tab.addClickHandler(event ->
         {
            event.preventDefault();
            SelectionEvent.fire(
                  MinimizedModuleTabLayoutPanel.this,
                  thisIndex);
         });
         tab.addDomHandler(event -> onTabKeyDown(event, thisIndex), KeyDownEvent.getType());
         tab.addDomHandler(this::onTabBlur, BlurEvent.getType());
         tabList.add(tab);
         tabs_.add(tab);
      }
   }

   /**
    * Mark the tab the pane will show when restored. Selection here doesn't
    * restore the pane; it mirrors the expanded tab panel's selection.
    */
   public void setSelectedIndex(int index)
   {
      selectedIndex_ = index;
      for (int i = 0; i < tabs_.size(); i++)
      {
         Roles.getTabRole().setAriaSelectedState(tabs_.get(i).getElement(),
               i == index ? SelectedValue.TRUE : SelectedValue.FALSE);
      }
      setTabStop(selectedIndex_);
   }

   public HandlerRegistration addSelectionHandler(SelectionHandler<Integer> handler)
   {
      return addHandler(handler, SelectionEvent.getType());
   }

   // Arrows only move focus: activating a tab restores the pane, so it waits
   // for Enter or Space (manual activation).
   private void onTabKeyDown(KeyDownEvent event, int index)
   {
      NativeEvent nativeEvent = event.getNativeEvent();
      if (nativeEvent.getAltKey() || nativeEvent.getCtrlKey() ||
          nativeEvent.getMetaKey() || nativeEvent.getShiftKey())
         return;

      int count = tabs_.size();
      switch (event.getNativeKeyCode())
      {
         case KeyCodes.KEY_LEFT:
            focusTab((index - 1 + count) % count);
            break;
         case KeyCodes.KEY_RIGHT:
            focusTab((index + 1) % count);
            break;
         case KeyCodes.KEY_HOME:
            focusTab(0);
            break;
         case KeyCodes.KEY_END:
            focusTab(count - 1);
            break;
         case KeyCodes.KEY_ENTER:
         case KeyCodes.KEY_SPACE:
            SelectionEvent.fire(this, index);
            restore();
            break;
         default:
            return;
      }
      event.preventDefault();
      event.stopPropagation();
   }

   // Coming back to the strip should land on the selected tab, not on the
   // one arrowed to last.
   private void onTabBlur(BlurEvent event)
   {
      EventTarget next = event.getNativeEvent().getRelatedEventTarget();
      if (next == null || !Element.is(next) ||
          !getTabList().getElement().isOrHasChild(Element.as(next)))
         setTabStop(selectedIndex_);
   }

   private void focusTab(int index)
   {
      setTabStop(index);
      tabs_.get(index).getElement().focus();
   }

   // The roving tab stop: only one tab is in the Tab order. With no selected
   // tab, the first one is, so the strip stays reachable.
   private void setTabStop(int index)
   {
      int stop = index >= 0 && index < tabs_.size() ? index : 0;
      for (int i = 0; i < tabs_.size(); i++)
         tabs_.get(i).getElement().setTabIndex(i == stop ? 0 : -1);
   }

   private FlowPanel getTabList()
   {
      return (FlowPanel) getExtraWidget();
   }

   private final ArrayList<ModuleTabLayoutPanel.ModuleTab> tabs_ = new ArrayList<>();
   private int selectedIndex_ = -1;
}
