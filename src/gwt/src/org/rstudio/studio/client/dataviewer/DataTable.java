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
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.data.DataEditingTargetWidget.DataViewerCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.HTML;
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
      // Use fill='currentColor' so the icon picks up the surrounding text
      // color (and thus adapts to the active IDE theme).
      sidebarButton_ = new HTML(
         "<svg width='14' height='12' viewBox='0 0 14 12' style='vertical-align:middle'>" +
         "<rect x='1' y='4' width='3' height='8' rx='0.5' fill='currentColor'/>" +
         "<rect x='5.5' y='0' width='3' height='12' rx='0.5' fill='currentColor'/>" +
         "<rect x='10' y='6' width='3' height='6' rx='0.5' fill='currentColor'/>" +
         "</svg>" +
         " <span style='font-size:11px'></span>");
      // Set the i18n text via DOM API rather than HTML concatenation so a
      // localized value containing "<" or "&" can't be interpreted as markup.
      Element sidebarLabelEl = Element.as(sidebarButton_.getElement().getLastChild());
      sidebarLabelEl.setInnerText(constants_.sidebarButtonText());
      sidebarButton_.getElement().getStyle().setCursor(Style.Cursor.POINTER);
      sidebarButton_.getElement().getStyle().setProperty("padding", "2px 5px");
      sidebarButton_.getElement().getStyle().setProperty("whiteSpace", "nowrap");
      sidebarButton_.addClickHandler(new ClickHandler() {
         public void onClick(ClickEvent event)
         {
            toggleSidebar(getWindow());
         }
      });
      toolbar.addLeftWidget(sidebarButton_);
      sidebarButton_.setVisible(!isPreview);

      // Gear icon — opens a popup of data viewer options. Currently houses
      // the "show Summary panel by default" toggle; other per-viewer or
      // per-user preferences can hang off this menu in future.
      optionsMenu_ = new ToolbarPopupMenu();
      optionsMenu_.addItem(new CheckableMenuItem(
            constants_.optionsShowSummaryDefault())
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
                  .dataViewerShowSummary().getValue();
         }

         @Override
         public void onInvoked()
         {
            UserPrefs prefs = RStudioGinjector.INSTANCE.getUserPrefs();
            prefs.dataViewerShowSummary().setGlobalValue(!isChecked());
            prefs.writeUserPrefs();
            onStateChanged();
         }
      });

      optionsButton_ = new HTML(
         "<svg width='14' height='14' viewBox='0 0 24 24' " +
         "style='vertical-align:middle' fill='currentColor'>" +
         "<path d='M19.14,12.94c0.04-0.3,0.06-0.61,0.06-0.94c0-0.32-0.02-0.64-0.07-0.94" +
         "l2.03-1.58c0.18-0.14,0.23-0.41,0.12-0.61l-1.92-3.32c-0.12-0.22-0.37-0.29-0.59-0.22" +
         "l-2.39,0.96c-0.5-0.38-1.03-0.7-1.62-0.94L14.4,2.81c-0.04-0.24-0.24-0.41-0.48-0.41" +
         "h-3.84c-0.24,0-0.43,0.17-0.47,0.41L9.25,5.35C8.66,5.59,8.12,5.92,7.63,6.29" +
         "L5.24,5.33c-0.22-0.08-0.47,0-0.59,0.22L2.74,8.87C2.62,9.08,2.66,9.34,2.86,9.48" +
         "l2.03,1.58C4.84,11.36,4.8,11.69,4.8,12s0.02,0.64,0.07,0.94l-2.03,1.58" +
         "c-0.18,0.14-0.23,0.41-0.12,0.61l1.92,3.32c0.12,0.22,0.37,0.29,0.59,0.22" +
         "l2.39-0.96c0.5,0.38,1.03,0.7,1.62,0.94l0.36,2.54c0.05,0.24,0.24,0.41,0.48,0.41" +
         "h3.84c0.24,0,0.44-0.17,0.47-0.41l0.36-2.54c0.59-0.24,1.13-0.56,1.62-0.94" +
         "l2.39,0.96c0.22,0.08,0.47,0,0.59-0.22l1.92-3.32c0.12-0.22,0.07-0.47-0.12-0.61" +
         "L19.14,12.94z M12,15.6c-1.98,0-3.6-1.62-3.6-3.6s1.62-3.6,3.6-3.6s3.6,1.62,3.6,3.6" +
         "S13.98,15.6,12,15.6z'/>" +
         "</svg>");
      optionsButton_.getElement().getStyle().setCursor(Style.Cursor.POINTER);
      optionsButton_.getElement().getStyle().setProperty("padding", "2px 5px");
      optionsButton_.setTitle(constants_.optionsButtonTitle());
      optionsButton_.addClickHandler(new ClickHandler() {
         public void onClick(ClickEvent event)
         {
            optionsMenu_.showRelativeTo(optionsButton_);
         }
      });
      toolbar.addLeftWidget(optionsButton_);
      optionsButton_.setVisible(!isPreview);

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

      toolbar.addRightWidget(searchWidget_);
      searchWidget_.setVisible(!isPreview);
      
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
         // swallow — close path must not throw
      }
   }

   private boolean isLimitedColumnFrame() { return isLimitedColumnFrame(getWindow()); }

   private static final native void toggleSidebar(WindowEx frame) /*-{
      if (frame && frame.toggleSidebar)
         frame.toggleSidebar();
   }-*/;

   private static final native boolean setFilterUIVisible (WindowEx frame, boolean visible) /*-{
      if (frame && frame.setFilterUIVisible)
         return frame.setFilterUIVisible(visible);
      return false;
   }-*/;

   private static final native void refreshData(WindowEx frame) /*-{
      if (frame && frame.refreshData)
         frame.refreshData();
   }-*/;

   private static final native void applySearch(WindowEx frame, String text) /*-{
      if (frame && frame.applySearch)
         frame.applySearch(text);
   }-*/;
   
   private static final native void onActivate(WindowEx frame) /*-{
      if (frame && frame.onActivate)
         frame.onActivate();
   }-*/;

   private static final native void onDeactivate(WindowEx frame) /*-{
      if (frame && frame.onDeactivate)
         frame.onDeactivate();
   }-*/;

   private static final native void onDismiss(WindowEx frame) /*-{
      if (frame && frame.onDismiss)
         frame.onDismiss();
   }-*/;

   private static final native boolean isLimitedColumnFrame(WindowEx frame) /*-{
       if (frame && frame.isLimitedColumnFrame)
           return frame.isLimitedColumnFrame();
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
      if (frame && frame.nextColumnPage)
          frame.nextColumnPage();
   }-*/;
   private static final native void prevColumnPage(WindowEx frame) /*-{
       if (frame && frame.prevColumnPage)
           frame.prevColumnPage();
   }-*/;
   private static final native void firstColumnPage(WindowEx frame) /*-{
       if (frame && frame.firstColumnPage)
           frame.firstColumnPage();
   }-*/;
   private static final native void lastColumnPage(WindowEx frame) /*-{
       if (frame && frame.lastColumnPage)
           frame.lastColumnPage();
   }-*/;

   private static final native void setOffsetAndMaxColumns(WindowEx frame, int offset, int max) /*-{
       if (frame && frame.setOffsetAndMaxColumns) {
           frame.setOffsetAndMaxColumns(offset, max);
       }
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
   private Host host_;
   private LatchingToolbarButton filterButton_;
   private HTML sidebarButton_;
   private HTML optionsButton_;
   private ToolbarPopupMenu optionsMenu_;
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
