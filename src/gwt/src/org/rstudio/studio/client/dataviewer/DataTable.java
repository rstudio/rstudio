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
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.events.NativeKeyDownEvent;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.CheckableMenuItem;
import org.rstudio.core.client.widget.LatchingToolbarButton;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarLabel;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.workbench.codesearch.ui.CodeSearchResources;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.data.DataEditingTargetWidget.DataViewerCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.MenuItem;
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
      searchWidget_.setPlaceholderText(constants_.searchPlaceholder());
      searchWidget_.addValueChangeHandler(new ValueChangeHandler<String>() {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            applySearch(getWindow(), event.getValue());
         }
      });

      // Refresh + options -- the options (gear) button sits to the left
      // of the refresh button; its dropdown contains the preference toggles.
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
      refreshButton_.getElement().getStyle().setMarginRight(8, Unit.PX);

      // Options (gear) button -- positioned to the left of the refresh
      // button.  A labeled ToolbarButton ("Settings") with the existing
      // options2x icon, which opens a ToolbarPopupMenu containing the
      // preference toggles and a "Reset View" action.
      optionsGearButton_ = new ToolbarButton(
              constants_.optionsButtonLabel(),
              constants_.optionsButtonTitle(),
              new ImageResource2x(StandardIcons.INSTANCE.options2x()),
              new ClickHandler() {
                 public void onClick(ClickEvent event)
                 {
                    optionsMenu_.showRelativeTo(optionsGearButton_);
                 }
              });
      // Stable class id so e2e tests and styling can target the gear button
      // (the 4-arg ToolbarButton constructor takes no class id, unlike the
      // sibling sidebar button).
      ClassIds.assignClassId(optionsGearButton_, ClassIds.DATA_TABLE_OPTIONS);

      // Refresh the show-summary and show-filters items' checked states
      // every time the popup is about to open, in case the pref was
      // changed elsewhere (global Options dialog, another open data viewer)
      // since we last showed it.
      optionsMenu_ = new ToolbarPopupMenu()
      {
         @Override
         public void getDynamicPopupMenu(ToolbarPopupMenu.DynamicPopupMenuCallback callback)
         {
            if (showSummaryItem_ != null) showSummaryItem_.onStateChanged();
            if (showFiltersItem_ != null) showFiltersItem_.onStateChanged();
            if (useOverlayScrollbarsItem_ != null) useOverlayScrollbarsItem_.onStateChanged();
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

      // Filter bar preference -- mirrors the summary panel toggle.
      showFiltersItem_ = new CheckableMenuItem(constants_.optionsShowFiltersDefault())
      {
         @Override
         public String getLabel()
         {
            return constants_.optionsShowFiltersDefault();
         }

         @Override
         public boolean isChecked()
         {
            return RStudioGinjector.INSTANCE.getUserPrefs()
                  .dataViewerShowFilters().getGlobalValue();
         }

         @Override
         public void onInvoked()
         {
            UserPrefs prefs = RStudioGinjector.INSTANCE.getUserPrefs();
            prefs.dataViewerShowFilters().setGlobalValue(!isChecked());
            prefs.writeUserPrefs();
            onStateChanged();
         }
      };
      // Scrollbar preference -- toggles between the custom overlay scrollbars
      // and the native scrollbars. Unlike the summary/filter toggles (which
      // only affect newly opened viewers via URL params), this is pushed into
      // the live grid: a value-change handler registered below calls
      // setUseOverlayScrollbars so the open table switches modes immediately.
      useOverlayScrollbarsItem_ = new CheckableMenuItem(constants_.optionsUseOverlayScrollbars())
      {
         @Override
         public String getLabel()
         {
            return constants_.optionsUseOverlayScrollbars();
         }

         @Override
         public boolean isChecked()
         {
            return RStudioGinjector.INSTANCE.getUserPrefs()
                  .dataViewerUseOverlayScrollbars().getGlobalValue();
         }

         @Override
         public void onInvoked()
         {
            UserPrefs prefs = RStudioGinjector.INSTANCE.getUserPrefs();
            prefs.dataViewerUseOverlayScrollbars().setGlobalValue(!isChecked());
            prefs.writeUserPrefs();
            onStateChanged();
         }
      };

      // Push preference changes into the open grid so the scrollbar mode
      // updates live -- whether the change came from the toggle above, the
      // global Options dialog, or another open data viewer.
      scrollbarPrefReg_ = RStudioGinjector.INSTANCE.getUserPrefs().dataViewerUseOverlayScrollbars()
            .addValueChangeHandler(new ValueChangeHandler<Boolean>() {
               @Override
               public void onValueChange(ValueChangeEvent<Boolean> event)
               {
                  setUseOverlayScrollbars(getWindow(), event.getValue());
               }
            });

      optionsMenu_.addItem(showSummaryItem_);
      optionsMenu_.addItem(showFiltersItem_);
      optionsMenu_.addItem(useOverlayScrollbarsItem_);
      optionsMenu_.addSeparator();

      // Format the label through formatMenuLabel (with a null icon) so it
      // carries the same 25px icon column that AppCommand- and
      // CheckableMenuItem-backed items use; otherwise this plain MenuItem's
      // text starts flush left and looks offset from the summary toggle below.
      optionsMenu_.addItem(new MenuItem(
            AppCommand.formatMenuLabel(constants_.optionsResetView()),
            true,
            () -> refreshAndReset()));

      // Right-side layout:
      //    [search] | [summary] | [options] | [refresh]
      // Search widget anchors the right edge. The summary toggle sits
      // in the middle on its own.  The gear (options) button sits next
      // to the refresh button -- clicking it opens the preference popup.
      toolbar.addRightWidget(searchWidget_);
      searchWidget_.setVisible(!isPreview);

      Widget summarySeparator = toolbar.addRightSeparator();
      toolbar.addRightWidget(sidebarButton_);
      sidebarButton_.setVisible(!isPreview);

      Widget optionsSeparator = toolbar.addRightSeparator();
      toolbar.addRightWidget(optionsGearButton_);
      optionsGearButton_.setVisible(!isPreview);

      Widget refreshSeparator = toolbar.addRightSeparator();
      toolbar.addRightWidget(refreshButton_);
      refreshButton_.setVisible(!isPreview);

      summarySeparator.setVisible(!isPreview);
      optionsSeparator.setVisible(!isPreview);
      refreshSeparator.setVisible(!isPreview);

      if (isPreview)
      {
         ToolbarLabel label =
            new ToolbarLabel(constants_.toolbarLabel());
         label.addStyleName(ThemeStyles.INSTANCE.toolbarInfoLabel());
         toolbar.addRightWidget(label);
      }
   }

   // A single column match from the grid's window.matchColumns: the
   // column's (1-based) index, its name, and whether the entry is a direct
   // index jump offered for a numeric query.
   private static class ColumnMatch extends JavaScriptObject
   {
      protected ColumnMatch() {}
      public final native String getName() /*-{ return this.name || ""; }-*/;
      public final native int getIndex() /*-{ return this.idx || 0; }-*/;
      public final native boolean getIsIndexJump() /*-{ return !!this.isIndexJump; }-*/;
   }

   private static class ColumnSuggestion implements SuggestOracle.Suggestion
   {
      ColumnSuggestion(ColumnMatch match)
      {
         match_ = match;
      }

      public int getIndex()
      {
         return match_.getIndex();
      }

      @Override
      public String getDisplayString()
      {
         String name = match_.getName();
         String label = match_.getIsIndexJump()
               ? "Column " + match_.getIndex() + (name.isEmpty() ? "" : ": " + name)
               : name;
         return SafeHtmlUtils.htmlEscape(label) +
               " <span style=\"opacity: 0.6;\">#" + match_.getIndex() + "</span>";
      }

      @Override
      public String getReplacementString()
      {
         return match_.getIsIndexJump()
               ? String.valueOf(match_.getIndex())
               : match_.getName();
      }

      private final ColumnMatch match_;
   }

   private void addColumnControls(Toolbar toolbar)
   {
      // A "Go to column" typeahead in the spirit of Go to File/Function:
      // type a column name or 1-based index, pick a suggestion (or press
      // Enter for the top match) to jump there. The grid scrolls
      // continuously through every column, so this jump is the only piece
      // of column navigation that still needs a control: a scrollbar can't
      // land on a specific column precisely in wide frames.
      SuggestOracle oracle = new SuggestOracle()
      {
         @Override
         public void requestSuggestions(Request request, Callback callback)
         {
            // A fresh keystroke starts a new query, so clear any selection
            // guard left over from a mouse-picked suggestion (which fires a
            // SelectionEvent but no SelectionCommitEvent to consume the flag).
            // Without this the next Enter would be swallowed by the stale flag.
            gotoColumnSelectionHandled_ = false;

            matchColumns(getWindow(), request.getQuery(), (matches) ->
            {
               ArrayList<Suggestion> suggestions = new ArrayList<>();
               if (matches != null)
               {
                  for (int i = 0; i < matches.length(); i++)
                     suggestions.add(new ColumnSuggestion(matches.get(i)));
               }
               callback.onSuggestionsReady(request, new Response(suggestions));
            });
         }

         @Override
         public boolean isDisplayStringHTML()
         {
            return true;
         }
      };

      gotoColumnWidget_ = new SearchWidget(constants_.goToColumnTitle(), oracle);
      gotoColumnWidget_.setPlaceholderText(constants_.goToColumnTitle());
      // Use the "go to" arrow (shared with Go to File/Function) rather than
      // the search magnifying glass, so the box reads as navigation and is
      // visually distinct from the data-table search box beside it.
      gotoColumnWidget_.setIcon(new ImageResource2x(CodeSearchResources.INSTANCE.gotoFunction2x()));
      gotoColumnWidget_.getElement().setId("data-viewer-goto-column");
      gotoColumnWidget_.setVisible(false);

      // A suggestion picked from the list (mouse, or Enter on a highlighted
      // entry) jumps directly and consumes the deferred Enter commit below.
      gotoColumnWidget_.addSelectionHandler((event) ->
      {
         SuggestOracle.Suggestion suggestion = event.getSelectedItem();
         if (suggestion instanceof ColumnSuggestion)
         {
            gotoColumnSelectionHandled_ = true;
            jumpToColumn(((ColumnSuggestion) suggestion).getIndex());
         }
      });

      // Enter without an explicit selection jumps to the top match; for a
      // numeric query with no matches (e.g. a dead grid after a failed
      // refresh), fall back to a direct index jump, which doubles as the
      // grid's bootstrap-retry path.
      gotoColumnWidget_.addSelectionCommitHandler((event) ->
      {
         if (gotoColumnSelectionHandled_)
         {
            gotoColumnSelectionHandled_ = false;
            return;
         }

         String query = StringUtil.notNull(event.getSelectedItem()).trim();
         if (query.isEmpty())
            return;

         matchColumns(getWindow(), query, (matches) ->
         {
            if (matches != null && matches.length() > 0)
            {
               jumpToColumn(matches.get(0).getIndex());
            }
            else if (query.matches("^\\d+$"))
            {
               // The regex admits digit strings beyond int (or even long)
               // range, which Integer.parseInt would throw on. Parse as long
               // and clamp; goToColumn clamps to the real column count, and on
               // a dead grid any value just triggers the bootstrap-retry, so
               // the exact magnitude is irrelevant.
               long parsed;
               try
               {
                  parsed = Long.parseLong(query);
               }
               catch (NumberFormatException e)
               {
                  parsed = Integer.MAX_VALUE;
               }
               jumpToColumn((int) Math.min(parsed, Integer.MAX_VALUE));
            }
         });
      });

      toolbar.addLeftWidget(gotoColumnWidget_);
   }

   private void jumpToColumn(int column)
   {
      goToColumn(getWindow(), column);
      // Reset the box for the next jump; the grid takes focus itself.
      gotoColumnWidget_.setText("", false);
   }

   private void setColumnControlVisibility(boolean visible)
   {
      colsSeparator_.setVisible(visible);
      gotoColumnWidget_.setVisible(visible);
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

   public void setColumnOverflowCallback()
   {
      // The grid pushes whether its columns overflow the viewport (which can
      // change on resize, sidebar toggle, column resize, pins, or a data
      // refresh); the go-to-column box only shows when there's somewhere to
      // jump that isn't already on screen.
      setColumnOverflowCallback(getWindow(), new CommandWithArg<Boolean>() {
         public void execute(Boolean overflow) {
            setColumnControlVisibility(overflow != null && overflow);
         }
      });
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

   public void setFilterStateCallback()
   {
      // JS owns whether the filter row is shown. It can become visible without
      // a toolbar click -- restoring saved per-column filters reveals the row
      // so they aren't applied invisibly (#17830). The callback fires on
      // registration and again at the end of bootstrap, keeping filtered_ and
      // the funnel latch in sync, mirroring the sidebar pattern above.
      setFilterStateCallback(getWindow(), new CommandWithArg<Boolean>() {
         public void execute(Boolean visible) {
            filtered_ = visible != null && visible;
            if (filterButton_ != null)
               filterButton_.setLatched(filtered_);
         }
      });
   }

   public void setStateKeyCallback()
   {
      // JS reports the localStorage key holding this object's saved UI state.
      // We stash it so onDismiss can clear it host-side on an explicit close;
      // see clearSavedStateHostSide for why the host (not the iframe) clears.
      setStateKeyCallback(getWindow(), new CommandWithArg<String>() {
         public void execute(String key) {
            stateKey_ = key;
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

   public void refreshAndReset()
   {
      filtered_ = false;
      if (searchWidget_ != null)
         searchWidget_.setText("", false);
      if (filterButton_ != null)
         filterButton_.setLatched(false);

      refreshAndReset(getWindow());
   }
   
   public void onActivate()
   {
      // When activated, the tab is still hidden (not yet selected), so
      // its iframe has no layout; defer until selectTab() makes the tab
      // visible and the iframe's onActivate() can run with real dimensions
      Scheduler.get().scheduleFinally(() ->
      {
         onActivate(getWindow());
      });
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
      // Stop listening for scrollbar-mode pref changes; this viewer is going
      // away and its frame should no longer be updated.
      if (scrollbarPrefReg_ != null)
      {
         scrollbarPrefReg_.removeHandler();
         scrollbarPrefReg_ = null;
      }

      try
      {
         // Authoritative clear: the iframe is usually already detached by the
         // time an explicit close reaches us (TabClosedEvent fires after the
         // tab widget is removed from the DOM), so getWindow() is null and the
         // in-frame onDismiss below no-ops. Clearing host-side works regardless
         // (#17830).
         clearSavedStateHostSide();

         // Best-effort in-frame clear for the rare case the frame is still live.
         onDismiss(getWindow());
      }
      catch(Exception e)
      {
         // The close path must not throw, but a silent catch hides bugs;
         // log so failures show up in dev/diagnostics output.
         Debug.logException(e);
      }
   }

   private void clearSavedStateHostSide()
   {
      // The data viewer iframe is same-origin with the host page, so they share
      // localStorage. That lets us remove the saved-state entry the frame
      // reported (via stateKeyCallback) even after the frame is gone -- the
      // frame can't do it itself once detached.
      if (StringUtil.isNullOrEmpty(stateKey_))
         return;

      removeLocalStorageItem(stateKey_);
   }

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

   private static final native void refreshAndReset(WindowEx frame) /*-{
      if (!frame) return;
      if (frame.refreshAndReset)
         frame.refreshAndReset();
      else
         @org.rstudio.studio.client.dataviewer.DataTable::logMissingFrameMethod(Ljava/lang/String;)("refreshAndReset");
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

   private static final native void goToColumn(WindowEx frame, int column) /*-{
      if (!frame) return;
      if (frame.goToColumn)
         frame.goToColumn(column);
      else
         @org.rstudio.studio.client.dataviewer.DataTable::logMissingFrameMethod(Ljava/lang/String;)("goToColumn");
   }-*/;

   private static final native void matchColumns(WindowEx frame,
                                                 String query,
                                                 CommandWithArg<JsArray<ColumnMatch>> callback) /*-{
      if (!frame || !frame.matchColumns) {
         if (frame)
            @org.rstudio.studio.client.dataviewer.DataTable::logMissingFrameMethod(Ljava/lang/String;)("matchColumns");
         callback.@org.rstudio.core.client.CommandWithArg::execute(*)(null);
         return;
      }
      frame.matchColumns(query, $entry(function(matches) {
         callback.@org.rstudio.core.client.CommandWithArg::execute(*)(matches);
      }));
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

   private static final native void setColumnOverflowCallback(WindowEx frame, CommandWithArg<Boolean> overflowCallback) /*-{
      frame.setOption(
         "columnOverflowCallback",
         $entry(function(overflow) {
            overflowCallback.@org.rstudio.core.client.CommandWithArg::execute(*)(@java.lang.Boolean::valueOf(Z)(!!overflow));
         }));
   }-*/;

   private static final native void setSidebarStateCallback(WindowEx frame, CommandWithArg<Boolean> sidebarStateCallback) /*-{
      frame.setOption(
         "sidebarStateCallback",
         $entry(function(visible) {
            sidebarStateCallback.@org.rstudio.core.client.CommandWithArg::execute(*)(@java.lang.Boolean::valueOf(Z)(!!visible));
         }));
   }-*/;

   private static final native void setFilterStateCallback(WindowEx frame, CommandWithArg<Boolean> filterStateCallback) /*-{
      frame.setOption(
         "filterStateCallback",
         $entry(function(visible) {
            filterStateCallback.@org.rstudio.core.client.CommandWithArg::execute(*)(@java.lang.Boolean::valueOf(Z)(!!visible));
         }));
   }-*/;

   private static final native void setStateKeyCallback(WindowEx frame, CommandWithArg<String> stateKeyCallback) /*-{
      frame.setOption(
         "stateKeyCallback",
         $entry(function(key) {
            stateKeyCallback.@org.rstudio.core.client.CommandWithArg::execute(*)(key);
         }));
   }-*/;

   private static final native void setUseOverlayScrollbars(WindowEx frame, boolean useOverlay) /*-{
      if (!frame) return;
      if (frame.setOption)
         frame.setOption("useOverlayScrollbars", useOverlay);
      else
         @org.rstudio.studio.client.dataviewer.DataTable::logMissingFrameMethod(Ljava/lang/String;)("setOption");
   }-*/;

   private static final native void removeLocalStorageItem(String key) /*-{
      try {
         $wnd.localStorage.removeItem(key);
      } catch (e) {
         // localStorage may be unavailable (private mode / blocked); nothing
         // to clear in that case.
      }
   }-*/;
   private Host host_;
   private LatchingToolbarButton filterButton_;
   private LatchingToolbarButton sidebarButton_;
   private ToolbarButton optionsGearButton_;
   private ToolbarButton refreshButton_;
   private ToolbarPopupMenu optionsMenu_;
   private CheckableMenuItem showSummaryItem_;
   private CheckableMenuItem showFiltersItem_;
   private CheckableMenuItem useOverlayScrollbarsItem_;

   // Registration for the live scrollbar-mode pref handler; removed on dismiss
   // so a closed viewer's handler doesn't outlive its frame.
   private HandlerRegistration scrollbarPrefReg_;
   private SearchWidget gotoColumnWidget_;
   private boolean gotoColumnSelectionHandled_ = false;
   private Widget colsSeparator_;
   private SearchWidget searchWidget_;
   private boolean filtered_ = false;

   // localStorage key for this object's persisted UI state, reported by the
   // iframe via stateKeyCallback; used to clear it host-side on close (#17830).
   private String stateKey_ = null;

   private static final DataViewerConstants constants_ = GWT.create(DataViewerConstants.class);
}
