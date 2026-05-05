/*
 * DataTable.java
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

package org.rstudio.studio.client.dataviewer;

import java.util.ArrayList;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.ClassIds;
import org.rstudio.core.client.CommandWith2Args;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.events.NativeKeyDownEvent;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeStyles;

import com.google.gwt.resources.client.ImageResource;
import org.rstudio.core.client.widget.CheckableMenuItem;
import org.rstudio.core.client.widget.DataTableColumnWidget;
import org.rstudio.core.client.widget.LatchingToolbarButton;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.core.client.widget.SimpleButton;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarLabel;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.data.DataEditingTargetWidget.DataViewerCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;

public class DataTable
{
   public interface Host 
   {
      RStudioFrame getDataTableFrame();
   }

   public DataTable(Host host)
   {
      host_ = host;
   }
   
   public void initToolbar(Toolbar toolbar, boolean isPreview)
   {
      filterButton_ = new LatchingToolbarButton(
              constants_.filterButtonText(),
              ToolbarButton.NoTitle,
              false, /* textIndicatesState */
              ClassIds.DATA_TABLE_FILTER_TOGGLE,
              new ImageResource2x(DataViewerResources.INSTANCE.filterIcon2x()),
              new ClickHandler() {
                 public void onClick(ClickEvent event)
                 {
                    boolean newFilterState = !filtered_;

                    // attempt to apply the new filter state, and update state
                    // if we succeed (might fail if the filter UI is not
                    // ready/table is not initialized)
                    if (setFilterUIVisible(newFilterState))
                    {
                       filtered_ = newFilterState;
                       filterButton_.setLatched(filtered_);
                    }
                 }
              });
      // Pass null leftImage so the toolbar emits an empty <img> placeholder
      // we can swap for an inline bar-chart SVG. SVG picks up the active
      // IDE theme via fill='currentColor' and stays sharp at any device
      // pixel ratio, so a static PNG isn't worth shipping.
      sidebarButton_ = new LatchingToolbarButton(
              constants_.sidebarButtonText(),
              ToolbarButton.NoTitle,
              false, /* textIndicatesState */
              ClassIds.DATA_TABLE_SIDEBAR_TOGGLE,
              null,
              new ClickHandler() {
                 public void onClick(ClickEvent event)
                 {
                    // JS owns sidebarVisible; the latched state is updated
                    // when JS fires sidebarStateCallback back at us.
                    toggleSidebar(getWindow());
                 }
              });
      NodeList<Element> sidebarImgs =
            sidebarButton_.getElement().getElementsByTagName("img");
      if (sidebarImgs.getLength() > 0)
      {
         Element img = sidebarImgs.getItem(0);
         Element span = Document.get().createSpanElement();
         span.setClassName(img.getClassName());
         span.setInnerHTML(
            "<svg width='14' height='10' viewBox='0 0 14 12' style='vertical-align:middle;position:relative;top:-2px'>" +
            "<rect x='0.5' y='6' width='4' height='6' rx='0.5' fill='currentColor'/>" +
            "<rect x='5'   y='0' width='4' height='12' rx='0.5' fill='currentColor'/>" +
            "<rect x='9.5' y='4' width='4' height='8' rx='0.5' fill='currentColor'/>" +
            "</svg>");
         img.getParentElement().replaceChild(span, img);
      }

      toolbar.addLeftWidget(filterButton_);
      filterButton_.setVisible(!isPreview);

      colsSeparator_ = toolbar.addLeftSeparator();
      colsSeparator_.setVisible(false);
      addColumnControls(toolbar);

      searchWidget_ = new SearchWidget(constants_.searchWidgetLabel(), new SuggestOracle() {
         @Override
         public void requestSuggestions(Request request, Callback callback)
         {
            // no suggestions
            callback.onSuggestionsReady(
                  request,
                  new Response(new ArrayList<>()));
         }
      });
      searchWidget_.addValueChangeHandler(new ValueChangeHandler<String>() {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            applySearch(getWindow(), event.getValue());
         }
      });

      // Refresh + options dropdown -- mirrors the pattern used in
      // EnvironmentPane (refresh button followed by a NoText
      // ToolbarMenuButton whose dropdown arrow opens a popup of
      // related options).
      refreshButton_ = new ToolbarButton(
         ToolbarButton.NoText,
         constants_.refreshButtonTitle(),
         new ImageResource2x(DataViewerResources.INSTANCE.refreshIcon2x()),
         new ClickHandler() {
            public void onClick(ClickEvent event)
            {
               refreshData(getWindow());
            }
         });
      refreshButton_.addStyleName(ThemeStyles.INSTANCE.refreshToolbarButton());

      // Refresh the show-summary item's checked state every time the
      // popup is about to open, in case the pref was changed elsewhere
      // (global Options dialog, another open data viewer) since we last
      // showed it.
      optionsMenu_ = new ToolbarPopupMenu() {
         @Override
         public void getDynamicPopupMenu(
               ToolbarPopupMenu.DynamicPopupMenuCallback callback) {
            if (showSummaryItem_ != null) showSummaryItem_.onStateChanged();
            super.getDynamicPopupMenu(callback);
         }
      };

      // Read AND write through getGlobalValue/setGlobalValue so the user-
      // layer value the menu reflects is the same one we mutate. Reading
      // the merged value (getValue()) while writing the user layer can
      // desync the check state from what's persisted when a project layer
      // is active.
      showSummaryItem_ = new CheckableMenuItem(constants_.optionsShowSummaryDefault())
      {
         @Override
         public String getLabel()
         {
            return constants_.optionsShowSummaryDefault();
         }

         @Override
         public boolean isChecked()
         {
            return RStudioGinjector.INSTANCE.getUserPrefs()
                  .dataViewerShowSummary().getGlobalValue();
         }

         @Override
         public void onInvoked()
         {
            UserPrefs prefs = RStudioGinjector.INSTANCE.getUserPrefs();
            prefs.dataViewerShowSummary().setGlobalValue(!isChecked());
            prefs.writeUserPrefs();
            onStateChanged();
         }
      };
      optionsMenu_.addItem(showSummaryItem_);
      optionsMenuButton_ = new ToolbarMenuButton(
            ToolbarButton.NoText,
            constants_.optionsButtonTitle(),
            optionsMenu_,
            false);

      // Right-side layout:
      //    [search] | [summary] | [refresh] [options]
      // Search widget anchors the right edge. The summary toggle sits
      // in the middle on its own; the options dropdown trails the
      // refresh button so we don't have to deal with the visual
      // mismatch between a latched LatchingToolbarButton and an
      // adjacent ToolbarMenuButton.
      toolbar.addRightWidget(searchWidget_);
      searchWidget_.setVisible(!isPreview);

      Widget summarySeparator = toolbar.addRightSeparator();
      toolbar.addRightWidget(sidebarButton_);
      sidebarButton_.setVisible(!isPreview);

      Widget refreshSeparator = toolbar.addRightSeparator();
      toolbar.addRightWidget(refreshButton_);
      refreshButton_.setVisible(!isPreview);
      toolbar.addRightWidget(optionsMenuButton_);
      optionsMenuButton_.setVisible(!isPreview);

      summarySeparator.setVisible(!isPreview);
      refreshSeparator.setVisible(!isPreview);

      if (isPreview)
      {
         ToolbarLabel label =
            new ToolbarLabel(constants_.toolbarLabel());
         label.addStyleName(ThemeStyles.INSTANCE.toolbarInfoLabel());
         toolbar.addRightWidget(label);
      }
   }

   private ClickHandler[] getColumnViewClickHandlers() {
      ClickHandler handlers[] = {
         new ClickHandler()
         {
            @Override
            public void onClick(ClickEvent event)
            {
               firstColumnPage();
            }
         },
         new ClickHandler()
         {
            @Override
            public void onClick(ClickEvent event)
            {
               prevColumnPage();
            }
         },
         new ClickHandler()
         {
            @Override
            public void onClick(ClickEvent event)
            {
               nextColumnPage();
            }
         },
         new ClickHandler()
         {
            @Override
            public void onClick(ClickEvent event)
            {
               lastColumnPage();
            }
         }
      };

      return handlers;
   }
   private void addColumnControls(Toolbar toolbar)
   {
      colsLabel_ = new ToolbarLabel(constants_.colsLabel());
      colsLabel_.addStyleName(ThemeStyles.INSTANCE.toolbarInfoLabel());
      colsLabel_.setVisible(false);
      toolbar.addLeftWidget(colsLabel_);

      ClickHandler[] clickHandlers = getColumnViewClickHandlers();
      SimpleButton columnButton;
      columnViewButtons_ = new ArrayList<>();

      for (int i = 0; i < COLUMN_VIEW_BUTTONS.length; i++)
      {
         columnButton = new SimpleButton(COLUMN_VIEW_BUTTONS[i], true);
         columnButton.addClickHandler(clickHandlers[i]);
         columnButton.setVisible(false);
         toolbar.addLeftWidget(columnButton);
         columnViewButtons_.add(columnButton);

         if (i == 1)
         {
            columnTextWidget_ = new DataTableColumnWidget(this::setOffsetAndMaxColumns);
            columnTextWidget_.getElement().setId("data-viewer-column-input");
            columnTextWidget_.setVisible(false);
            toolbar.addLeftWidget(columnTextWidget_);
         }
      }
   }

   private void setColumnControlVisibility(boolean visible)
   {
      colsSeparator_.setVisible(visible);
      colsLabel_.setVisible(visible);
      for (int i = 0; i < COLUMN_VIEW_BUTTONS.length; i++)
      {
         columnViewButtons_.get(i).setVisible(visible);
      }
      columnTextWidget_.setVisible(visible);
   }

   private CommandWith2Args<Double, Double> getDataTableColumnCallback()
   {
      return (offset, max) ->
      {
         columnTextWidget_.setValue((offset + 1) + " - " + (offset + max));
         setColumnControlVisibility(isLimitedColumnFrame());
      };
   }

   private WindowEx getWindow()
   {
      IFrameElementEx frameEl = (IFrameElementEx) host_.getDataTableFrame().getElement().cast();
      return frameEl.getContentWindow();
   }
   
   public void addKeyDownHandler()
   {
      addKeyDownHandlerImpl(getWindow().getDocument().getBody());
   }
   
   private final native void addKeyDownHandlerImpl(Element body)
   /*-{
      var self = this;
      body.addEventListener("keydown", $entry(function(event) {
         self.@org.rstudio.studio.client.dataviewer.DataTable::onKeyDown(*)(event);
      }));
   }-*/;
   
   private void onKeyDown(NativeEvent event)
   {
      // Electron seems to handle keypresses in the main window even if
      // a sub-frame has focus, so this handler is not necessary there.
      //
      // On macOS, we do need to handle Ctrl + W to close the window, though.
      // 
      // https://github.com/rstudio/rstudio/issues/12029
      if (BrowseCap.isElectron())
      {
         if (BrowseCap.isMacintoshDesktop())
         {
            int modifiers = KeyboardShortcut.getModifierValue(event);
            if (modifiers == KeyboardShortcut.CTRL && event.getKeyCode() == KeyCodes.KEY_W)
            {
               RStudioGinjector.INSTANCE.getCommands().closeSourceDoc().execute();
            }
         }
         else
         {
            // Intentionally do nothing -- see notes above
         }
      }
      else
      {
         ShortcutManager.INSTANCE.onKeyDown(new NativeKeyDownEvent(event));
      }
   }

   public boolean setFilterUIVisible(boolean visible)
   {
      return setFilterUIVisible(getWindow(), visible);
   }
   
   public void setDataViewerCallback(DataViewerCallback dataCallback)
   {
      setDataViewerCallback(getWindow(), dataCallback);
   }
   
   public void setListViewerCallback(DataViewerCallback listCallback)
   {
      setListViewerCallback(getWindow(), listCallback);
   }

   public void setColumnFrameCallback()
   {
      setColumnFrameCallback(getWindow(), getDataTableColumnCallback());
   }

   public void setSidebarStateCallback()
   {
      // JS owns the canonical sidebarVisible state (URL params + saved
      // localStorage state can both influence it). On registration the JS
      // side fires the callback once with the current value, so the latched
      // state syncs without us having to track it locally.
      setSidebarStateCallback(getWindow(), new CommandWithArg<Boolean>() {
         public void execute(Boolean visible) {
            if (sidebarButton_ != null)
               sidebarButton_.setLatched(visible != null && visible);
         }
      });
   }

   public void refreshData()
   {
      filtered_= false;
      if (searchWidget_ != null)
         searchWidget_.setText("", false);
      if (filterButton_ != null)
         filterButton_.setLatched(false);

      refreshData(getWindow());
   }
   
   public void onActivate()
   {
      onActivate(getWindow());
   }
   
   public void onDeactivate()
   {
      try
      {
         onDeactivate(getWindow());
      }
      catch(Exception e)
      {
         // swallow exceptions occurring when deactivating, as they'll keep
         // users from switching tabs
      }
   }

   public void onDismiss()
   {
      try
      {
         onDismiss(getWindow());
      }
      catch(Exception e)
      {
         // The close path must not throw, but a silent catch hides bugs;
         // log so failures show up in dev/diagnostics output.
         Debug.logException(e);
      }
   }

   private boolean isLimitedColumnFrame() { return isLimitedColumnFrame(getWindow()); }

   // Surface "frame is here but the method we expected is missing" so the
   // mismatch shows up in dev logs instead of a silent no-op. The
   // frame-absent case is normal during teardown and intentionally quiet.
   private static void logMissingFrameMethod(String name)
   {
      Debug.log("DataTable: iframe missing method '" + name + "'");
   }

   private static final native void toggleSidebar(WindowEx frame) /*-{
      if (!frame) return;
      if (frame.toggleSidebar)
         frame.toggleSidebar();
      else
         @org.rstudio.studio.client.dataviewer.DataTable::logMissingFrameMethod(Ljava/lang/String;)("toggleSidebar");
   }-*/;

   private static final native boolean setFilterUIVisible (WindowEx frame, boolean visible) /*-{
      if (!frame) return false;
      if (frame.setFilterUIVisible)
         return frame.setFilterUIVisible(visible);
      @org.rstudio.studio.client.dataviewer.DataTable::logMissingFrameMethod(Ljava/lang/String;)("setFilterUIVisible");
      return false;
   }-*/;

   private static final native void refreshData(WindowEx frame) /*-{
      if (!frame) return;
      if (frame.refreshData)
         frame.refreshData();
      else
         @org.rstudio.studio.client.dataviewer.DataTable::logMissingFrameMethod(Ljava/lang/String;)("refreshData");
   }-*/;

   private static final native void applySearch(WindowEx frame, String text) /*-{
      if (!frame) return;
      if (frame.applySearch)
         frame.applySearch(text);
      else
         @org.rstudio.studio.client.dataviewer.DataTable::logMissingFrameMethod(Ljava/lang/String;)("applySearch");
   }-*/;

   private static final native void onActivate(WindowEx frame) /*-{
      if (!frame) return;
      if (frame.onActivate)
         frame.onActivate();
      else
         @org.rstudio.studio.client.dataviewer.DataTable::logMissingFrameMethod(Ljava/lang/String;)("onActivate");
   }-*/;

   private static final native void onDeactivate(WindowEx frame) /*-{
      if (!frame) return;
      if (frame.onDeactivate)
         frame.onDeactivate();
      else
         @org.rstudio.studio.client.dataviewer.DataTable::logMissingFrameMethod(Ljava/lang/String;)("onDeactivate");
   }-*/;

   private static final native void onDismiss(WindowEx frame) /*-{
      if (!frame) return;
      if (frame.onDismiss)
         frame.onDismiss();
      else
         @org.rstudio.studio.client.dataviewer.DataTable::logMissingFrameMethod(Ljava/lang/String;)("onDismiss");
   }-*/;

   private static final native boolean isLimitedColumnFrame(WindowEx frame) /*-{
      if (!frame) return false;
      if (frame.isLimitedColumnFrame)
          return frame.isLimitedColumnFrame();
      @org.rstudio.studio.client.dataviewer.DataTable::logMissingFrameMethod(Ljava/lang/String;)("isLimitedColumnFrame");
      return false;
   }-*/;

   private void nextColumnPage()
   {
      nextColumnPage(getWindow());
   }
   private void prevColumnPage()
   {
      prevColumnPage(getWindow());
   }
   private void firstColumnPage()
   {
      firstColumnPage(getWindow());
   }
   private void lastColumnPage()
   {
      lastColumnPage(getWindow());
   }
   private void setOffsetAndMaxColumns(int offset, int max)
   {
      setOffsetAndMaxColumns(getWindow(), offset, max);
   }

   private static final native void nextColumnPage(WindowEx frame) /*-{
      if (!frame) return;
      if (frame.nextColumnPage)
         frame.nextColumnPage();
      else
         @org.rstudio.studio.client.dataviewer.DataTable::logMissingFrameMethod(Ljava/lang/String;)("nextColumnPage");
   }-*/;
   private static final native void prevColumnPage(WindowEx frame) /*-{
      if (!frame) return;
      if (frame.prevColumnPage)
         frame.prevColumnPage();
      else
         @org.rstudio.studio.client.dataviewer.DataTable::logMissingFrameMethod(Ljava/lang/String;)("prevColumnPage");
   }-*/;
   private static final native void firstColumnPage(WindowEx frame) /*-{
      if (!frame) return;
      if (frame.firstColumnPage)
         frame.firstColumnPage();
      else
         @org.rstudio.studio.client.dataviewer.DataTable::logMissingFrameMethod(Ljava/lang/String;)("firstColumnPage");
   }-*/;
   private static final native void lastColumnPage(WindowEx frame) /*-{
      if (!frame) return;
      if (frame.lastColumnPage)
         frame.lastColumnPage();
      else
         @org.rstudio.studio.client.dataviewer.DataTable::logMissingFrameMethod(Ljava/lang/String;)("lastColumnPage");
   }-*/;

   private static final native void setOffsetAndMaxColumns(WindowEx frame, int offset, int max) /*-{
      if (!frame) return;
      if (frame.setOffsetAndMaxColumns)
         frame.setOffsetAndMaxColumns(offset, max);
      else
         @org.rstudio.studio.client.dataviewer.DataTable::logMissingFrameMethod(Ljava/lang/String;)("setOffsetAndMaxColumns");
   }-*/;
   private static final native void setDataViewerCallback(WindowEx frame, DataViewerCallback dataCallback) /*-{
      frame.setOption(
         "dataViewerCallback", 
         $entry(function(row, col) {
            dataCallback.@org.rstudio.studio.client.workbench.views.source.editors.data.DataEditingTargetWidget.DataViewerCallback::execute(*)(row, col);
         }));
   }-*/;
   
   private static final native void setListViewerCallback(WindowEx frame, DataViewerCallback listCallback) /*-{
      frame.setOption(
         "listViewerCallback", 
         $entry(function(row, col) {
            listCallback.@org.rstudio.studio.client.workbench.views.source.editors.data.DataEditingTargetWidget.DataViewerCallback::execute(*)(row, col);
         }));
   }-*/;

   private static final native void setColumnFrameCallback(WindowEx frame, CommandWith2Args<Double, Double> columnFrameCallback) /*-{
      frame.setOption(
         "columnFrameCallback",
         $entry(function(offset, max) {
            columnFrameCallback.@org.rstudio.core.client.CommandWith2Args::execute(*)(offset, max);
         }));
   }-*/;

   private static final native void setSidebarStateCallback(WindowEx frame, CommandWithArg<Boolean> sidebarStateCallback) /*-{
      frame.setOption(
         "sidebarStateCallback",
         $entry(function(visible) {
            sidebarStateCallback.@org.rstudio.core.client.CommandWithArg::execute(*)(@java.lang.Boolean::valueOf(Z)(!!visible));
         }));
   }-*/;
   private Host host_;
   private LatchingToolbarButton filterButton_;
   private LatchingToolbarButton sidebarButton_;
   private ToolbarButton refreshButton_;
   private ToolbarMenuButton optionsMenuButton_;
   private ToolbarPopupMenu optionsMenu_;
   private CheckableMenuItem showSummaryItem_;
   private DataTableColumnWidget columnTextWidget_;
   private Widget colsSeparator_;
   private ToolbarLabel colsLabel_;
   private ArrayList<SimpleButton> columnViewButtons_;
   private SearchWidget searchWidget_;
   private boolean filtered_ = false;

   private static String COLUMN_VIEW_BUTTONS[] = {
      "<i class=\"icon-angle-double-left \"></i>",
      "<i class=\"icon-angle-left \"></i>",
      "<i class=\"icon-angle-right \"></i>",
      "<i class=\"icon-angle-double-right \"></i>"
   };

   private static final DataViewerConstants constants_ = GWT.create(DataViewerConstants.class);
}
