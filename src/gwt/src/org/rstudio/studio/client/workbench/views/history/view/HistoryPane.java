/*
 * HistoryPane.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.history.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.layout.client.Layout.AnimationCallback;
import com.google.gwt.layout.client.Layout.Layer;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.history.HasHistory;
import org.rstudio.studio.client.workbench.views.history.History.SearchBoxDisplay;
import org.rstudio.studio.client.workbench.views.history.events.FetchCommandsEvent;
import org.rstudio.studio.client.workbench.views.history.events.FetchCommandsHandler;
import org.rstudio.studio.client.workbench.views.history.model.HistoryEntry;
import org.rstudio.studio.client.workbench.views.history.view.HistoryEntryItemCodec.TimestampMode;

import java.util.ArrayList;

public class HistoryPane extends WorkbenchPane
      implements org.rstudio.studio.client.workbench.views.history.History.Display,
                 HasSelectionCommitHandlers<Void>
{
   interface Resources extends ClientBundle
   {
      @Source("HistoryPane.css")
      Styles styles();

      @Source("searchResultsContextButton_2x.png")
      ImageResource searchResultsContextButton2x();

      @Source("searchResultsContextButton2_2x.png")
      ImageResource searchResultsContextButton22x();
   }

   interface Styles extends CssResource
   {
      String selected();
      String loadMore();
      String historyTable();

      String command();
      String timestamp();
      String disclosure();

      String inboundFocus();
      String fakeFocus();
   }

   public static void ensureStylesInjected()
   {
      ((Resources) GWT.create(Resources.class)).styles().ensureInjected();
   }

   @Inject
   public HistoryPane(Commands commands)
   {
      super("History");
      commands_ = commands;
      ensureWidget();
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      scrollToBottom();
   }

   @Override
   public void onBeforeUnselected()
   {
      super.onBeforeUnselected();
      recentScrollPanel_.saveScrollPosition();
   }

   @Override
   public void onSelected()
   {
      super.onSelected();
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            recentScrollPanel_.restoreScrollPosition();
         }
      });
   }
   
   @Override
   public void focusSearch()
   {
      FocusHelper.setFocusDeferred(searchWidget_);
   }

   @Override
   protected Widget createMainWidget()
   {
      mainPanel_ = new LayoutPanel();

      mainPanel_.addStyleName("ace_editor");

      VerticalPanel vpanel = new VerticalPanel();
      vpanel.setSize("100%", "100%");

      loadMore_ = new Anchor("Load more entries...", "javascript:return false");
      loadMore_.setWidth("100%");
      loadMore_.setVisible(false);
      loadMore_.setStyleName(styles_.loadMore());
      vpanel.add(loadMore_);
      loadMore_.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            fireEvent(new FetchCommandsEvent());
         }
      });

      commandList_ = createHistoryTable(TimestampMode.NONE);
      vpanel.add(commandList_);

      recentScrollPanel_ = new BottomScrollPanel() {
         @Override
         protected void onLoad()
         {
            super.onLoad();
         }
      };
      recentScrollPanel_.getElement().getStyle().setProperty("overflowX", "hidden");
      recentScrollPanel_.setWidget(vpanel);
      commandList_.setOwningScrollPanel(recentScrollPanel_);

      mainPanel_.add(recentScrollPanel_);
      mainPanel_.setWidgetTopBottom(recentScrollPanel_, 0, Unit.PX, 0, Unit.PX);
      mainPanel_.setWidgetLeftRight(recentScrollPanel_, 0, Unit.PX, 0, Unit.PX);

      searchLabel_ = new Label();
      searchLabel_.setHeight("");
      searchResults_ = new HistoryTableWithToolbar(
            createHistoryTable(TimestampMode.ITEM),
            new Widget[] {
                  searchLabel_
            },
            new Widget[] {
                  new SmallButton(commands_.historyDismissResults())
            });
      mainPanel_.add(searchResults_);
      mainPanel_.setWidgetTopBottom(searchResults_, 0, Unit.PX, 0, Unit.PX);
      mainPanel_.setWidgetLeftRight(searchResults_, 0, Unit.PX, 0, Unit.PX);

      contextLabel_ = new Label();
      contextResults_ = new HistoryTableWithToolbar(
            createHistoryTable(TimestampMode.GROUP),
            new Widget[] {
                  new SmallButton(commands_.historyDismissContext()),
                  contextLabel_
            },
            new Widget[] {
                  new SmallButton(commands_.historyDismissResults())
            });
      mainPanel_.add(contextResults_);
      mainPanel_.setWidgetTopBottom(contextResults_, 0, Unit.PX, 0, Unit.PX);
      mainPanel_.setWidgetLeftRight(contextResults_, 0, Unit.PX, 0, Unit.PX);

      setMode(Mode.Recent);
      setVisible(searchResults_, searchResults_.getFocusTarget(), false);
      setVisible(contextResults_, contextResults_.getFocusTarget(), false);

      return mainPanel_;
   }

   public Mode getMode()
   {
      return mode_;
   }

   protected void setMode(Mode mode)
   {
      if (mode != mode_)
      {
         Widget current = getWidgetForMode(mode_);
         Widget target = getWidgetForMode(mode);

         boolean focus = DomUtils.hasFocus(getActiveHistory().getFocusTarget());
         HasHistory focusCurrent = getHistoryForMode(mode_);
         HasHistory focusTarget = getHistoryForMode(mode);

         boolean rightToLeft = mode_.getValue() < mode.getValue();

         if (mode != Mode.Recent && mode_ != Mode.Recent)
            animate(current,
                    target,
                    rightToLeft,
                    focusCurrent,
                    focusTarget,
                    focus);
         else
         {
            setVisible(current, focusCurrent.getFocusTarget(), false);
            setVisible(target, focusTarget.getFocusTarget(), true);
            if (focus)
            {
               DomUtils.setActive(focusTarget.getFocusTarget());
            }
         }
         mode_ = mode;
         
         // enable/disable commands
         boolean enableRemoveCommands = mode_ == Mode.Recent;
         commands_.historyRemoveEntries().setEnabled(enableRemoveCommands);
         commands_.clearHistory().setEnabled(enableRemoveCommands);
      }
   }

   private HasHistory getHistoryForMode(Mode mode)
   {
      return (mode == Mode.Recent) ? commandList_ :
             (mode == Mode.SearchResults) ? searchResults_ :
             (mode == Mode.CommandContext) ? contextResults_ : null;
   }

   private Widget getWidgetForMode(Mode mode)
   {
      return (mode == Mode.Recent) ? recentScrollPanel_ :
      (mode == Mode.SearchResults) ? searchResults_ :
      (mode == Mode.CommandContext) ? contextResults_ : null;
   }

   private void animate(final Widget from,
                        final Widget to,
                        boolean rightToLeft,
                        final HasHistory fromFocus,
                        final HasHistory toFocus,
                        final boolean focus)
   {
      assert from != to;

      if (focus)
         toFocus.getFocusTarget().addClassName(styles_.inboundFocus());

      setVisible(to, toFocus.getFocusTarget(), true);

      int width = getOffsetWidth();

      mainPanel_.setWidgetLeftWidth(from,
                                    0, Unit.PX,
                                    width, Unit.PX);
      mainPanel_.setWidgetLeftWidth(to,
                                    rightToLeft ? width : -width, Unit.PX,
                                    width, Unit.PX);
      mainPanel_.forceLayout();

      mainPanel_.setWidgetLeftWidth(from,
                                    rightToLeft ? -width : width, Unit.PX,
                                    width, Unit.PX);
      mainPanel_.setWidgetLeftWidth(to,
                                    0, Unit.PX,
                                    width, Unit.PX);

      mainPanel_.animate(300, new AnimationCallback()
      {
         public void onAnimationComplete()
         {
            setVisible(from, fromFocus.getFocusTarget(), false);
            mainPanel_.setWidgetLeftRight(to, 0, Unit.PX, 0, Unit.PX);
            mainPanel_.forceLayout();
            if (focus)
            {
               DomUtils.setActive(toFocus.getFocusTarget());
               toFocus.getFocusTarget().removeClassName(styles_.inboundFocus());
            }
         }

         public void onLayout(Layer layer, double progress)
         {
         }
      });
   }

   private void setVisible(Widget widget,
                           Element focusTarget,
                           boolean visible)
   {
      //mainPanel_.getWidgetContainerElement(widget).getStyle().setDisplay(
      //      visible ? Display.BLOCK : Display.NONE);
      if (visible)
      {
         if (focusTarget.getTabIndex() != 0)
         {
            focusTarget.setTabIndex(0);
            mainPanel_.setWidgetLeftRight(widget, 0, Unit.PX, 0, Unit.PX);
         }
      }
      else
      {
         if (focusTarget.getTabIndex() != -1)
         {
            focusTarget.setTabIndex(-1);
            if (DomUtils.hasFocus(focusTarget))
               focusTarget.blur();
            mainPanel_.setWidgetLeftRight(widget, -5000, Unit.PX, 5000, Unit.PX);
         }
      }
      mainPanel_.forceLayout();
   }

   public void scrollToBottom()
   {
      assert mode_ == Mode.Recent;
      recentScrollPanel_.scrollToBottom();
   }

   private HistoryTable createHistoryTable(TimestampMode timestampMode)
   {
      HistoryTable table = new HistoryTable(
            styles_.command(),
            styles_.timestamp(),
            styles_.selected(),
            timestampMode,
            commands_);
      table.setSize("100%", "100%");
      table.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            if (doubleClick_.checkForDoubleClick(event.getNativeEvent()))
            {
               if (event.getNativeEvent().getShiftKey())
                  commands_.historySendToSource().execute();
               else
                  commands_.historySendToConsole().execute();
            }
         }
         private final DoubleClickState doubleClick_ = new DoubleClickState();
      });

      return table;
   }

   public void setRecentCommands(ArrayList<HistoryEntry> entries,
                                 boolean scrollToBottom)
   {
      commandList_.clear();
      commandList_.addItems(entries, true);
      if (scrollToBottom)
      {
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            public void execute()
            {
               recentScrollPanel_.scrollToBottom();
            }
         });
      }
   }
   
   public void truncateRecentCommands(int maxCommands)
   {
      commandList_.removeTopRows(
            Math.max(0, commandList_.getRowCount() - maxCommands));
   }
   
   public ArrayList<Integer> getRecentCommandsSelectedRowIndexes()
   {
      return commandList_.getSelectedRowIndexes();
   }
   
   public int getRecentCommandsRowsDisplayed()
   {
      return commandList_.getRowCount();
   }

   public void setMoreCommands(long moreCommands)
   {
      if (moreCommands <= 0)
         loadMore_.setVisible(false);
      else
      {
         loadMore_.setVisible(true);
         loadMore_.setText("Load " + moreCommands + " more entries");
      }
   }

   public void addRecentCommands(ArrayList<HistoryEntry> entries, boolean top)
   {
      TableRowElement topRow = null;
      if (top)
         topRow = commandList_.getTopRow();

      commandList_.addItems(entries, top);

      if (top)
      {
         if (topRow == null)
            recentScrollPanel_.scrollToBottom();
         else
            recentScrollPanel_.setVerticalScrollPosition(topRow.getOffsetTop());
      }
      else
         recentScrollPanel_.onContentSizeChanged();
   }

   public ArrayList<String> getSelectedCommands()
   {
      return getActiveHistory().getSelectedValues();
   }

   public ArrayList<Long> getSelectedCommandIndexes()
   {
      return getActiveHistory().getSelectedCommandIndexes();
   }

   public SearchBoxDisplay getSearchBox()
   {
      return new SearchBoxDisplay()
      {
         public String getText()
         {
            return searchWidget_.getText();
         }

         public void setText(String text)
         {
            searchWidget_.setText(text, true);
         }

         public HandlerRegistration addValueChangeHandler(
               ValueChangeHandler<String> handler)
         {
            return searchWidget_.addValueChangeHandler(handler);
         }

         public void fireEvent(GwtEvent<?> event)
         {
            searchWidget_.fireEvent(event);
         }
      };
   }
   
   public int getRecentCommandsScrollPosition()
   {
      return recentScrollPanel_.getVerticalScrollPosition();
   }
   
   public void setRecentCommandsScrollPosition(int scrollPosition)
   {
      recentScrollPanel_.setVerticalScrollPosition(scrollPosition);
   }

   private HasHistory getActiveHistory()
   {
      switch (mode_)
      {
         case Recent:
            return commandList_;
         case SearchResults:
            return searchResults_;
         case CommandContext:
            return contextResults_;
         default:
            assert false : "Unknown mode";
            return null;
      }
   }

   public void dismissSearchResults()
   {
      setMode(Mode.Recent);
      //searchResults_.clear();
      //contextResults_.clear();
      searchWidget_.setText("");
   }

   public void showSearchResults(String query,
                                 ArrayList<HistoryEntry> entries)
   {
      searchLabel_.setText("Search results: " + query);
      setMode(Mode.SearchResults);
      contextResults_.clear();
      searchResults_.clear();
      searchResults_.addItems(entries, true);
      if (entries.size() > 0)
         searchResults_.highlightRows(0, 1);
   }

   public void dismissContext()
   {
      setMode(Mode.SearchResults);
      DomUtils.setActive(searchResults_.getFocusTarget());
      //contextResults_.clear();
   }

   public HasHistory getRecentCommandsWidget()
   {
      return commandList_;
   }

   public HasHistory getSearchResultsWidget()
   {
      return searchResults_;
   }

   public HasHistory getCommandContextWidget()
   {
      return contextResults_;
   }

   public boolean isCommandTableFocused()
   {
      return DomUtils.hasFocus(getActiveHistory().getFocusTarget());
   }

   public void showContext(String command,
                           ArrayList<HistoryEntry> entries,
                           long highlightOffset,
                           long highlightLength)
   {
      contextLabel_.setText("Showing command in context");
      setMode(Mode.CommandContext);
      contextResults_.clear();
      contextResults_.addItems(entries, true);
      contextResults_.highlightRows((int)highlightOffset, (int)highlightLength);
   }

   public HandlerRegistration addFetchCommandsHandler(FetchCommandsHandler handler)
   {
      return addHandler(handler, FetchCommandsEvent.TYPE);
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      searchWidget_ = new SearchWidget(new SuggestOracle()
      {
         @Override
         public void requestSuggestions(Request request,
                                        Callback callback)
         {
            callback.onSuggestionsReady(
                  request,
                  new Response(new ArrayList<Suggestion>()));
         }
      });
      searchWidget_.addKeyDownHandler(new KeyDownHandler()
      {
         public void onKeyDown(KeyDownEvent event)
         {
            getActiveHistory().getKeyTarget().fireEvent(event);
         }
      });
      searchWidget_.addSelectionCommitHandler(new SelectionCommitHandler<String>()
      {
         public void onSelectionCommit(SelectionCommitEvent<String> event)
         {
            if (mode_ == Mode.SearchResults)
               fireEvent(event);
         }
      });
      searchWidget_.addFocusHandler(new FocusHandler()
      {
         public void onFocus(FocusEvent event)
         {
            commandList_.getFocusTarget().addClassName(styles_.fakeFocus());
            searchResults_.getFocusTarget().addClassName(styles_.fakeFocus());
            contextResults_.getFocusTarget().addClassName(styles_.fakeFocus());
         }
      });
      searchWidget_.addBlurHandler(new BlurHandler()
      {
         public void onBlur(BlurEvent event)
         {
            commandList_.getFocusTarget().removeClassName(styles_.fakeFocus());
            searchResults_.getFocusTarget().removeClassName(styles_.fakeFocus());
            contextResults_.getFocusTarget().removeClassName(styles_.fakeFocus());
         }
      });

      Toolbar toolbar = new Toolbar();
      toolbar.addLeftWidget(commands_.loadHistory().createToolbarButton());
      toolbar.addLeftWidget(commands_.saveHistory().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.historySendToConsole().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.historySendToSource().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.historyRemoveEntries().createToolbarButton());
      toolbar.addLeftWidget(commands_.clearHistory().createToolbarButton());
      
      toolbar.addRightWidget(searchWidget_);
      
      return toolbar;
   }

   public HandlerRegistration addSelectionCommitHandler(
         SelectionCommitHandler<Void> handler)
   {
      return addHandler(handler, SelectionCommitEvent.getType());
   }

   private HistoryTable commandList_;
   private BottomScrollPanel recentScrollPanel_;

   private Label searchLabel_;
   private Label contextLabel_;
   private HistoryTableWithToolbar contextResults_;
   private HistoryTableWithToolbar searchResults_;
   private final Commands commands_;
   private Anchor loadMore_;
   private SearchWidget searchWidget_;
   private Styles styles_ = ((Resources) GWT.create(Resources.class)).styles();
   private LayoutPanel mainPanel_;
   private Mode mode_ = Mode.Recent;
  
}
