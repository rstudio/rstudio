/*
 * WorkbenchTabPanel.java
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

package org.rstudio.studio.client.workbench.ui;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.events.*;
import org.rstudio.core.client.layout.LogicalWindow;
import org.rstudio.core.client.theme.ModuleTabLayoutPanel;
import org.rstudio.core.client.theme.WindowFrame;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.model.ProvidesBusy;

import java.util.ArrayList;

class WorkbenchTabPanel
      extends Composite
      implements RequiresResize,
                 ProvidesResize,
                 HasSelectionHandlers<Integer>,
                 HasEnsureVisibleHandlers,
                 HasEnsureHeightHandlers
{
   public WorkbenchTabPanel(WindowFrame owner, LogicalWindow parentWindow, String tabListName)
   {
      final int UTILITY_AREA_SIZE = 52;
      panel_ = new LayoutPanel();

      parentWindow_ = parentWindow;

      tabPanel_ = new ModuleTabLayoutPanel(owner, tabListName);
      panel_.add(tabPanel_);
      panel_.setWidgetTopBottom(tabPanel_, 0, Unit.PX, 0, Unit.PX);
      panel_.setWidgetLeftRight(tabPanel_, 0, Unit.PX, 0, Unit.PX);

      tabPanel_.setSize("100%", "100%");
      tabPanel_.addStyleDependentName("Workbench");

      utilPanel_ = new HTML();
      utilPanel_.setStylePrimaryName(ThemeStyles.INSTANCE.multiPodUtilityArea());
      utilPanel_.addStyleName(ThemeStyles.INSTANCE.rstheme_multiPodUtilityTabArea());
      panel_.add(utilPanel_);
      panel_.setWidgetRightWidth(utilPanel_,
                                 0, Unit.PX,
                                 UTILITY_AREA_SIZE, Unit.PX);
      panel_.setWidgetTopHeight(utilPanel_, 0, Unit.PX, 22, Unit.PX);

      initWidget(panel_);
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();

      releaseOnUnload_.add(tabPanel_.addBeforeSelectionHandler(beforeSelectionEvent ->
      {
         if (clearing_)
            return;

         if (getSelectedIndex() >= 0)
         {
            int unselectedTab = getSelectedIndex();
            if (unselectedTab < tabs_.size())
            {
               WorkbenchTab lastTab = tabs_.get(unselectedTab);
               lastTab.onBeforeUnselected();
            }
         }

         int selectedTab = beforeSelectionEvent.getItem().intValue();
         if (selectedTab < tabs_.size())
         {
            WorkbenchTab tab = tabs_.get(selectedTab);
            tab.onBeforeSelected();
         }
      }));
      releaseOnUnload_.add(tabPanel_.addSelectionHandler(selectionEvent ->
      {
         if (clearing_)
            return;

         WorkbenchTab pane = tabs_.get(selectionEvent.getSelectedItem().intValue());
         pane.onSelected();
      }));

      int selectedIndex = tabPanel_.getSelectedIndex();
      if (selectedIndex >= 0)
      {
         WorkbenchTab tab = tabs_.get(selectedIndex);
         tab.onBeforeSelected();
         tab.onSelected();
      }
   }

   @Override
   protected void onUnload()
   {
      releaseOnUnload_.removeHandler();

      super.onUnload();
   }

   public void setTabs(ArrayList<WorkbenchTab> tabs)
   {
      if (areTabsIdentical(tabs))
         return;

      tabPanel_.clear();
      tabs_.clear();

      for (WorkbenchTab tab : tabs)
         add(tab);
   }

   private boolean areTabsIdentical(ArrayList<WorkbenchTab> tabs)
   {
      if (tabs_.size() != tabs.size())
         return false;

      // In case tab panels were removed implicitly (such as Console)
      if (tabPanel_.getWidgetCount() != tabs.size())
         return false;

      for (int i = 0; i < tabs.size(); i++)
         if (tabs_.get(i) != tabs.get(i))
            return false;

      return true;
   }

   @SuppressWarnings("unused")
   private void add(final WorkbenchTab tab)
   {
      if (tab.isSuppressed())
         return;

      tabs_.add(tab);
      final Widget widget = tab.asWidget();
      tabPanel_.add(widget, tab.getTitle(), false, !tab.closeable() ? null : new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            tab.confirmClose(() -> tab.ensureHidden());
         }
      },
      tab instanceof ProvidesBusy ? (ProvidesBusy) tab : null);

      tab.addEnsureVisibleHandler(ensureVisibleEvent ->
      {
         if (!neverVisible_)
         {
            // First ensure that we ourselves are visible
            int myInt = tabPanel_.getWidgetCount();
            LogicalWindow window = getParentWindow();
            fireEvent(new EnsureVisibleEvent(ensureVisibleEvent.getActivate()));
            if (ensureVisibleEvent.getActivate())
               tabPanel_.selectTab(widget);
         }
      });

      tab.addEnsureHeightHandler(ensureHeightEvent -> fireEvent(ensureHeightEvent));
   }

   public void selectNextTab()
   {
      selectTabRelative(1);
   }

   public void selectPreviousTab()
   {
      selectTabRelative(-1);
   }

   public void selectTabRelative(int offset)
   {
      int index = (getSelectedIndex() + offset) % tabs_.size();
      selectTab(index);
   }

   public void selectTab(int tabIndex)
   {
      if (tabPanel_.getSelectedIndex() == tabIndex)
      {
         // if it's already selected then we still want to fire the
         // onBeforeSelected and onSelected methods (so that actions
         // like auto-focus are always taken)
         int selected = getSelectedIndex();
         if (selected != -1)
         {
            WorkbenchTab tab = tabs_.get(selected);
            tab.onBeforeSelected();
            tab.onSelected();
         }

         return;
      }

      // deal with migrating from n+1 to n tabs, and with -1 values
      int safeIndex = Math.min(Math.max(0, tabIndex), tabs_.size() - 1);
      if (safeIndex >= 0)
         tabPanel_.selectTab(safeIndex);
      else
         Debug.logToConsole("Attempted to select tab in empty tab panel.");
   }

   public void selectTab(WorkbenchTab pane)
   {
      int index = tabs_.indexOf(pane);
      if (index != -1)
         selectTab(index);
      else
      {
         String title = pane.getTitle();
         for (int i = 0; i < tabs_.size(); i++)
         {
            WorkbenchTab tab = tabs_.get(i);
            if (tab.getTitle() == title)
            {
               selectTab(i);
               return;
            }
         }
      }
   }

   public boolean isEmpty()
   {
      return tabs_.isEmpty();
   }

   public WorkbenchTab getTab(int index)
   {
      return tabs_.get(index);
   }

   public WorkbenchTab getSelectedTab()
   {
      return tabs_.get(getSelectedIndex());
   }

   public int getSelectedIndex()
   {
      return tabPanel_.getSelectedIndex();
   }

   public int getWidgetCount()
   {
      return tabPanel_.getWidgetCount();
   }

   public HandlerRegistration addSelectionHandler(SelectionHandler<Integer> integerSelectionHandler)
   {
      return tabPanel_.addSelectionHandler(integerSelectionHandler);
   }

   public void onResize()
   {
      Widget w = getWidget();
      if (w instanceof RequiresResize)
         ((RequiresResize)w).onResize();
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleEvent.Handler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }

   @Override
   public HandlerRegistration addEnsureHeightHandler(EnsureHeightEvent.Handler handler)
   {
      return addHandler(handler, EnsureHeightEvent.TYPE);
   }

   public void clear()
   {
      clearing_ = true;
      tabPanel_.clear();
      tabs_.clear();
      clearing_ = false;
   }

   public LogicalWindow getParentWindow()
   {
      return parentWindow_;
   }

   public void setNeverVisible(boolean value)
   {
      neverVisible_ = value;
   }

   private ModuleTabLayoutPanel tabPanel_;
   private ArrayList<WorkbenchTab> tabs_ = new ArrayList<>();
   private final LogicalWindow parentWindow_;
   private final HandlerRegistrations releaseOnUnload_ = new HandlerRegistrations();
   private boolean clearing_ = false;
   private boolean neverVisible_ = false;
   private LayoutPanel panel_;
   private HTML utilPanel_;
}
