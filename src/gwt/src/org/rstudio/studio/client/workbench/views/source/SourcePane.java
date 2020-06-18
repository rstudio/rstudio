/*
 * SourcePane.java
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

public class SourcePane extends LazyPanel implements Display,
                                                     HasEnsureVisibleHandlers,
                                                     HasEnsureHeightHandlers,
                                                     ProvidesResize,
                                                     RequiresResize,
                                                     BeforeShowCallback,
                                                     RequiresVisibilityChanged
{
   @Inject
   public SourcePane()
   {
      setVisible(true);
      ensureWidget();
   }

   @Override
   protected Widget createWidget()
   {
	  final int UTILITY_AREA_SIZE = 74;
      panel_ = new LayoutPanel();

      new AutoGlassAttacher(panel_);

      tabPanel_ =  new DocTabLayoutPanel(true, 65, UTILITY_AREA_SIZE);
      panel_.setSize("100%", "100%");
      panel_.add(tabPanel_);
      panel_.setWidgetTopBottom(tabPanel_, 0, Unit.PX, 0, Unit.PX);
      panel_.setWidgetLeftRight(tabPanel_, 0, Unit.PX, 0, Unit.PX);

      utilPanel_ = new HTML();
      utilPanel_.setStylePrimaryName(ThemeStyles.INSTANCE.multiPodUtilityArea());
      panel_.add(utilPanel_);
      panel_.setWidgetRightWidth(utilPanel_,
                                    0, Unit.PX,
                                    UTILITY_AREA_SIZE, Unit.PX);
      panel_.setWidgetTopHeight(utilPanel_, 0, Unit.PX, 22, Unit.PX);

      tabOverflowPopup_ = new TabOverflowPopupPanel();
      tabOverflowPopup_.addCloseHandler(new CloseHandler<PopupPanel>()
      {
         public void onClose(CloseEvent<PopupPanel> popupPanelCloseEvent)
         {
            manageChevronVisibility();
         }
      });
      chevron_ = new Image(new ImageResource2x(ThemeResources.INSTANCE.chevron2x()));
      chevron_.setAltText("Switch to tab");
      chevron_.getElement().getStyle().setCursor(Cursor.POINTER);
      chevron_.addClickHandler(event -> tabOverflowPopup_.showRelativeTo(chevron_));

      panel_.add(chevron_);
      panel_.setWidgetTopHeight(chevron_,
                                8, Unit.PX,
                                chevron_.getHeight(), Unit.PX);
      panel_.setWidgetRightWidth(chevron_,
                                 52, Unit.PX,
                                 chevron_.getWidth(), Unit.PX);

      return panel_;
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
      tabPanel_.add(widget, icon, docId, name, tooltip, position);
      if (switchToTab)
         tabPanel_.selectTab(widget, true /* focus newly added tab */);
   }

   public int getTabCount()
   {
      return tabPanel_.getWidgetCount();
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

   @Override
   public void moveTab(int index, int delta)
   {
      tabPanel_.moveTab(index, delta);
   }

   @Override
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

   @Override
   public void setDirty(Widget widget, boolean dirty)
   {
      Widget tab = tabPanel_.getTabWidget(widget);
      if (dirty)
         tab.addStyleName(ThemeStyles.INSTANCE.dirtyTab());
      else
         tab.removeStyleName(ThemeStyles.INSTANCE.dirtyTab());
   }

   public void closeTab(boolean interactive)
   {
      closeTab(getActiveTabIndex(), interactive, null);
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

   public void ensureVisible()
   {
      fireEvent(new EnsureVisibleEvent(true));
   }

   @Override
   public Widget asWidget()
   {
      return this;
   }

   public void onResize()
   {
      panel_.onResize();
      manageChevronVisibility();
   }

   public void onBeforeShow()
   {
      for (Widget w : panel_)
         if (w instanceof BeforeShowCallback)
            ((BeforeShowCallback)w).onBeforeShow();

      fireEvent(new BeforeShowEvent());
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

   public HandlerRegistration addBeforeSelectionHandler(BeforeSelectionHandler<Integer> handler)
   {
      return tabPanel_.addBeforeSelectionHandler(handler);
   }

   public HandlerRegistration addBeforeShowHandler(BeforeShowEvent.Handler handler)
   {
      return addHandler(handler, BeforeShowEvent.TYPE);
   }

   public HandlerRegistration addEnsureHeightHandler(EnsureHeightEvent.Handler handler)
   {
      return addHandler(handler, EnsureHeightEvent.TYPE);
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleEvent.Handler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }

   public HandlerRegistration addSelectionHandler(SelectionHandler<Integer> handler)
   {
      return tabPanel_.addSelectionHandler(handler);
   }

   public HandlerRegistration addTabClosingHandler(TabClosingEvent.Handler handler)
   {
      return tabPanel_.addTabClosingHandler(handler);
   }

   public HandlerRegistration addTabCloseHandler(TabCloseEvent.Handler handler)
   {
      return tabPanel_.addTabCloseHandler(handler);
   }

   public HandlerRegistration addTabClosedHandler(TabClosedEvent.Handler handler)
   {
      return tabPanel_.addTabClosedHandler(handler);
   }

   public HandlerRegistration addTabReorderHandler(TabReorderEvent.Handler handler)
   {
      return tabPanel_.addTabReorderHandler(handler);
   }

   private void setOverflowVisible(boolean visible)
   {
      utilPanel_.setVisible(visible);
      chevron_.setVisible(visible);
   }

   private DocTabLayoutPanel tabPanel_;
   private HTML utilPanel_;
   private Image chevron_;
   private LayoutPanel panel_;
   private PopupPanel tabOverflowPopup_;

}
