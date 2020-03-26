/*
 * SourcePane.java
 *
 * Copyright (C) 2009-19 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.source;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;

import org.rstudio.core.client.events.*;
import org.rstudio.core.client.layout.RequiresVisibilityChanged;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.DocTabLayoutPanel;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.BeforeShowCallback;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.AutoGlassAttacher;
import org.rstudio.studio.client.common.filetypes.FileIcon;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog;
import org.rstudio.studio.client.workbench.views.source.Source.Display;
import java.util.ArrayList;

public class SourcePane extends Composite implements Display,
                                                     HasEnsureVisibleHandlers,
                                                     HasEnsureHeightHandlers,
                                                     RequiresResize,
                                                     ProvidesResize,
                                                     BeforeShowCallback,
                                                     RequiresVisibilityChanged
{
   @Inject
   public SourcePane()
   {
      final int UTILITY_AREA_SIZE = 74;

      SourcePanel sp = new SourcePanel();
      sp.panel_ = new LayoutPanel();

      new AutoGlassAttacher(sp.panel_);

      sp.tabPanel_ =  new DocTabLayoutPane(true, 65, UTILITY_AREA_SIZE));
      sp.panel_.add(sp.tabPanel_);
      sp.panel_.setWidgetTopBottom(sp.tabPanel_, 0, Unit.PX, 0, Unit.PX);
      sp.panel_.setWidgetLeftRight(sp.tabPanel_, 0, Unit.PX, 0, Unit.PX);

      sp.utilPanel_ = new HTML();
      sp.utilPanel_.setStylePrimaryName(ThemeStyles.INSTANCE.multiPodUtilityArea());
      sp.panel_.add(sp.utilPanel_);
      sp.panel_.setWidgetRightWidth(sp.utilPanel_,
                                    0, Unit.PX,
                                    UTILITY_AREA_SIZE, Unit.PX);
      sp.panel_.setWidgetTopHeight(sp.utilPanel_, 0, Unit.PX, 22, Unit.PX);

      sp.tabOverflowPopup_ = new TabOverflowPopupPanel();
      sp.tabOverflowPopup_.addCloseHandler(new CloseHandler<PopupPanel>()
      {
         public void onClose(CloseEvent<PopupPanel> popupPanelCloseEvent)
         {
            manageChevronVisibility();
         }
      });
      sp.chevron_ = new Image(new ImageResource2x(ThemeResources.INSTANCE.chevron2x()));
      sp.chevron_.setAltText("Switch to tab");
      sp.chevron_.getElement().getStyle().setCursor(Cursor.POINTER);
      sp.chevron_.addClickHandler(event -> sp.tabOverflowPopup_.showRelativeTo(sp.chevron_));

      sp.panel_.add(sp.chevron_);
      sp.panel_.setWidgetTopHeight(sp.chevron_,
                                    8, Unit.PX,
                                    sp.chevron_.getHeight(), Unit.PX);
      sp.panel_.setWidgetRightWidth(sp.chevron_,
                                    52, Unit.PX,
                                    sp.chevron_.getWidth(), Unit.PX);
      
      initWidget(sp.panel_);
      panelList_.add(sp);
      activePanel_ = sp;
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      Scheduler.get().scheduleDeferred(() -> onResize());
   }

   public void addTab(Widget widget,
                      FileIcon icon,
                      String docId,
                      String name,
                      String tooltip,
                      Integer position,
                      boolean switchToTab)
   {
      activePanel_.tabPanel.add(widget, icon, docId, name, tooltip, position);
      if (switchToTab)
         tabPanel_.selectTab(widget);
   }

   public boolean hasTab(Widget widget)
   {
      return tabPanel_.getWidgetIndex(widget) >= 0 ? true : false;
   }

   public void closeTab(Widget child, boolean interactive)
   {
      closeTab(child, interactive, null);
   }

   public void closeTab(Widget child, boolean interactive, Command onClosed)
   {
      closeTab(tabPanel_.getWidgetIndex(child), interactive, onClosed);
   }
   
   public void closeTab(int index, boolean interactive)
   {
      closeTab(index, interactive, null);
   }

   public void closeTab(int index, boolean interactive, Command onClosed)
   {
      if (interactive)
         tabPanel_.tryCloseTab(index, onClosed);
      else
         tabPanel_.closeTab(index, onClosed);
   }
   
   public void setDirty(Widget widget, boolean dirty)
   {
      Widget tab = tabPanel_.getTabWidget(widget);
      if (dirty)
         tab.addStyleName(ThemeStyles.INSTANCE.dirtyTab());
      else
         tab.removeStyleName(ThemeStyles.INSTANCE.dirtyTab());
   }

   public void ensureVisible()
   {
      fireEvent(new EnsureVisibleEvent());
   }

   public void renameTab(Widget child,
                         FileIcon icon,
                         String value,
                         String tooltip)
   {
      tabPanel_.replaceDocName(tabPanel_.getWidgetIndex(child),
                               icon,
                               value,
                               tooltip);
   }

   public int getActiveTabIndex()
   {
      return tabPanel_.getSelectedIndex();
   }

   public void selectTab(int tabIndex)
   {
      tabPanel_.selectTab(tabIndex);
   }

   public void selectTab(Widget child)
   {
      tabPanel_.selectTab(child);
   }

   public int getTabCount()
   {
      return tabPanel_.getWidgetCount();
   }

   @Override
   public void moveTab(int index, int delta)
   {
      tabPanel_.moveTab(index, delta);
   }

   public HandlerRegistration addTabClosingHandler(TabClosingHandler handler)
   {
      return tabPanel_.addTabClosingHandler(handler);
   }

   public HandlerRegistration addTabCloseHandler(
         TabCloseHandler handler)
   {
      return tabPanel_.addTabCloseHandler(handler);
   }
   
   public HandlerRegistration addTabClosedHandler(TabClosedHandler handler)
   {
      return tabPanel_.addTabClosedHandler(handler);
   }

   @Override
   public HandlerRegistration addTabReorderHandler(TabReorderHandler handler)
   {
      return tabPanel_.addTabReorderHandler(handler);
   }
 
   public HandlerRegistration addSelectionHandler(SelectionHandler<Integer> handler)
   {
      return tabPanel_.addSelectionHandler(handler);
   }

   public HandlerRegistration addBeforeSelectionHandler(BeforeSelectionHandler<Integer> handler)
   {
      return tabPanel_.addBeforeSelectionHandler(handler);
   }

   public Widget asWidget()
   {
      return this;
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }
   
   @Override
   public HandlerRegistration addEnsureHeightHandler(
         EnsureHeightHandler handler)
   {
      return addHandler(handler, EnsureHeightEvent.TYPE);
   }

   public void onResize()
   {
      panel_.onResize();
      manageChevronVisibility();
   }

   public void manageChevronVisibility()
   {
      int tabsWidth = tabPanel_.getTabsEffectiveWidth();
      setOverflowVisible(tabsWidth > getOffsetWidth() - 50);
   }

   public void showOverflowPopup()
   {
      setOverflowVisible(true);
      tabOverflowPopup_.showRelativeTo(chevron_);
   }
   
   @Override
   public void showUnsavedChangesDialog(
         String title,
         ArrayList<UnsavedChangesTarget> dirtyTargets,
         OperationWithInput<UnsavedChangesDialog.Result> saveOperation,
         Command onCancelled)
   {
      new UnsavedChangesDialog(title, 
                               dirtyTargets,
                               saveOperation,
                               onCancelled).showModal();
   }

   private void setOverflowVisible(boolean visible)
   {
      utilPanel_.setVisible(visible);
      chevron_.setVisible(visible);
   }

   public void onBeforeShow()
   {
      fireEvent(new BeforeShowEvent());
   }

   public HandlerRegistration addBeforeShowHandler(BeforeShowHandler handler)
   {
      return addHandler(handler, BeforeShowEvent.TYPE);
   }

   public void onVisibilityChanged(boolean visible)
   {
      if (getActiveTabIndex() >= 0)
      {
         Widget w = tabPanel_.getTabWidget(getActiveTabIndex());
         if (w instanceof RequiresVisibilityChanged)
            ((RequiresVisibilityChanged)w).onVisibilityChanged(visible);
      }
   }
   
   public void cancelTabDrag()
   {
      tabPanel_.cancelTabDrag();
   }

   class SourcePanel
   {
      DocTabLayoutPanel tabPanel_;
      HTML utilPanel_;
      Image chevron_;
      LayoutPanel panel_;
      PopupPanel tabOverflowPopup_;
   };

   private SourcePanel activePanel_;
   private ArrayList<SourcePanel> panelList_ = new ArrayList<SourcePanel>();
}
