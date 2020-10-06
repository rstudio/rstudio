/*
 * History.java
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
package org.rstudio.studio.client.workbench.views.history;

import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperation;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.ConsoleDispatcher;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.StringStateValue;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleResetHistoryEvent;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.history.events.FetchCommandsEvent;
import org.rstudio.studio.client.workbench.views.history.events.FetchCommandsHandler;
import org.rstudio.studio.client.workbench.views.history.events.HistoryEntriesAddedEvent;
import org.rstudio.studio.client.workbench.views.history.events.HistoryEntriesAddedHandler;
import org.rstudio.studio.client.workbench.views.history.model.HistoryEntry;
import org.rstudio.studio.client.workbench.views.history.model.HistoryServerOperations;
import org.rstudio.studio.client.workbench.views.source.events.InsertSourceEvent;

import java.util.ArrayList;

public class History extends BasePresenter implements SelectionCommitEvent.Handler<Void>,
                                                      FetchCommandsHandler
{
   public interface SearchBoxDisplay extends HasValueChangeHandlers<String>
   {
      String getText();
      public void setText(String text);
   }

   public interface Display extends WorkbenchView,
                                    HasSelectionCommitHandlers<Void>
   {
      public enum Mode
      {
         Recent(0),
         SearchResults(1),
         CommandContext(2);

         Mode(int value)
         {
            value_ = value;
         }

         public int getValue()
         {
            return value_;
         }

         private final int value_;
      }

      void setRecentCommands(ArrayList<HistoryEntry> commands,
                             boolean scrollToBottom);
      void addRecentCommands(ArrayList<HistoryEntry> entries, boolean top);

      int getRecentCommandsScrollPosition();
      void setRecentCommandsScrollPosition(int scrollPosition);

      ArrayList<Integer> getRecentCommandsSelectedRowIndexes();
      int getRecentCommandsRowsDisplayed();

      void truncateRecentCommands(int maxCommands);

      ArrayList<String> getSelectedCommands();
      ArrayList<Long> getSelectedCommandIndexes();
      HandlerRegistration addFetchCommandsHandler(FetchCommandsHandler handler);
      void setMoreCommands(long moreCommands);
      SearchBoxDisplay getSearchBox();
      Mode getMode();
      void scrollToBottom();
      void setFocus();

      void dismissSearchResults();
      void showSearchResults(String query,
                             ArrayList<HistoryEntry> entries);
      void showContext(String command,
                       ArrayList<HistoryEntry> entries,
                       long highlightOffset,
                       long highlightLength);
      void dismissContext();

      HasHistory getRecentCommandsWidget();
      HasHistory getSearchResultsWidget();
      HasHistory getCommandContextWidget();

      boolean isCommandTableFocused();


   }

   public interface Binder extends CommandBinder<Commands, History>
   {}


   class SearchCommand extends TimeBufferedCommand implements ValueChangeHandler<String>
   {
      SearchCommand(Session session)
      {
         super(200);
      }

      @Override
      protected void performAction(boolean shouldSchedulePassive)
      {
         final String query = searchQuery_;
         if (searchQuery_ != null && searchQuery_.length() > 0)
         {
            server_.searchHistoryArchive(
                  searchQuery_, COMMAND_CHUNK_SIZE,
                  new SimpleRequestCallback<RpcObjectList<HistoryEntry>>()
                  {
                     @Override
                     public void onResponseReceived(
                           RpcObjectList<HistoryEntry> response)
                     {
                        if (!StringUtil.equals(query, searchQuery_))
                           return;

                        ArrayList<HistoryEntry> entries = toList(response);
                        view_.showSearchResults(query, entries);
                     }
                  });
         }
      }

      public void onValueChange(ValueChangeEvent<String> event)
      {
         String query = event.getValue();
         searchQuery_ = query;
         if (searchQuery_ == "")
         {
            view_.dismissSearchResults();
         }
         else
         {
            nudge();
         }
      }

      public void dismissResults()
      {
         view_.dismissSearchResults();
         searchQuery_ = null;
      }

      private String searchQuery_;
   }

   @Inject
   public History(final Display view,
                  HistoryServerOperations server,
                  final GlobalDisplay globalDisplay,
                  ConsoleDispatcher consoleDispatcher,
                  EventBus events,
                  final Session session,
                  Commands commands,
                  Binder binder)
   {
      super(view);
      view_ = view;
      events_ = events;
      globalDisplay_ = globalDisplay;
      consoleDispatcher_ = consoleDispatcher;
      searchCommand_ = new SearchCommand(session);
      session_ = session;

      binder.bind(commands, this);

      view_.addSelectionCommitHandler(this);
      view_.addFetchCommandsHandler(this);

      server_ = server;
      events_.addHandler(ConsoleResetHistoryEvent.TYPE, new ConsoleResetHistoryEvent.Handler()
      {
         @Override
         public void onConsoleResetHistory(ConsoleResetHistoryEvent event)
         {
            view_.bringToFront();

            // convert to HistoryEntry
            ArrayList<HistoryEntry> commands = toRecentCommandsList(
                                                         event.getHistory());

            // determine entries to add
            int preservedScrollPos = -1;
            int startIndex = Math.max(0, commands.size() - COMMAND_CHUNK_SIZE);

            // if we are updating an existing context then preserve the
            // history position and the scroll position
            if (event.getPreserveUIContext())
            {
               preservedScrollPos = view_.getRecentCommandsScrollPosition();

               if (historyPosition_ < commands.size())
                  startIndex = Long.valueOf(historyPosition_).intValue();
            }

            // set recent commands
            ArrayList<HistoryEntry> subList = new ArrayList<HistoryEntry>();
            subList.addAll(commands.subList(startIndex, commands.size()));
            boolean scrollToBottom = preservedScrollPos == -1;
            setRecentCommands(subList, scrollToBottom);

            // restore scroll position if requested
            if (preservedScrollPos != -1)
            {
               final int scrollPos = preservedScrollPos;
               Scheduler.get().scheduleDeferred(new ScheduledCommand()
               {
                  public void execute()
                  {
                     view_.setRecentCommandsScrollPosition(scrollPos);
                  }
               });
            }
         }
      });

      events_.addHandler(HistoryEntriesAddedEvent.TYPE, new HistoryEntriesAddedHandler()
      {
         public void onHistoryEntriesAdded(HistoryEntriesAddedEvent event)
         {
            view_.addRecentCommands(toList(event.getEntries()), false);
            view_.truncateRecentCommands(
                        session_.getSessionInfo().getConsoleHistoryCapacity());
         }
      });

      view_.getSearchBox().addValueChangeHandler(searchCommand_);

      view_.getRecentCommandsWidget().getKeyTarget().addKeyDownHandler(
            new KeyHandler(commands.historySendToConsole(),
                           commands.historySendToSource(),
                           null,
                           null));
      view_.getSearchResultsWidget().getKeyTarget().addKeyDownHandler(
            new KeyHandler(commands.historySendToConsole(),
                           commands.historySendToSource(),
                           commands.historyDismissResults(),
                           commands.historyShowContext()));
      view_.getCommandContextWidget().getKeyTarget().addKeyDownHandler(
            new KeyHandler(commands.historySendToConsole(),
                           commands.historySendToSource(),
                           commands.historyDismissContext(),
                           null));

      new StringStateValue("history", "query", ClientState.TEMPORARY,
                           session.getSessionInfo().getClientState())
      {
         @Override
         protected void onInit(String value)
         {
            if (value != null && value.length() != 0)
            {
               view_.getSearchBox().setText(value);
            }
         }

         @Override
         protected String getValue()
         {
            return view_.getSearchBox().getText();
         }
      };

      server_.getRecentHistory(
            COMMAND_CHUNK_SIZE,
            new ServerRequestCallback<RpcObjectList<HistoryEntry>>()
      {
         @Override
         public void onResponseReceived(RpcObjectList<HistoryEntry> response)
         {
            ArrayList<HistoryEntry> result = toRecentCommandsList(response);
            setRecentCommands(result, true);
         }

         @Override
         public void onError(ServerError error)
         {
            globalDisplay_.showErrorMessage("Error While Retrieving History",
                                           error.getUserMessage());
         }
      });
   }


   private void setRecentCommands(ArrayList<HistoryEntry> commands,
                                  boolean scrollToBottom)
   {
      view_.setRecentCommands(commands, scrollToBottom);

      if (commands.size() > 0)
         historyPosition_ = commands.get(0).getIndex();
      else
         historyPosition_ = 0;

      view_.setMoreCommands(Math.min(historyPosition_, COMMAND_CHUNK_SIZE));
   }


   private class KeyHandler implements KeyDownHandler
   {
      private KeyHandler(Command accept,
                         Command shiftAccept,
                         Command left,
                         Command right)
      {
         this.accept_ = accept;
         this.shiftAccept_ = shiftAccept;
         this.left_ = left;
         this.right_ = right;
      }

      public void onKeyDown(KeyDownEvent event)
      {
         if (!view_.isCommandTableFocused())
            return;

         boolean handled = false;

         if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
         {
            if (event.isShiftKeyDown())
            {
               if (shiftAccept_ != null)
                  shiftAccept_.execute();
               handled = true;
            }
            else if (!event.isAnyModifierKeyDown())
            {
               if (accept_ != null)
                  accept_.execute();
               handled = true;
            }
         }
         else if (!event.isAnyModifierKeyDown())
         {
            switch (event.getNativeKeyCode())
            {
               case KeyCodes.KEY_ESCAPE:
               case KeyCodes.KEY_LEFT:
                  if (left_ != null)
                     left_.execute();
                  handled = true;
                  break;
               case KeyCodes.KEY_RIGHT:
                  if (right_ != null)
                     right_.execute();
                  handled = true;
                  break;
            }
         }

         if (handled)
         {
            event.preventDefault();
            event.stopPropagation();
         }
      }

      private final Command accept_;
      private final Command shiftAccept_;
      private final Command left_;
      private final Command right_;
   }

   @Override
   public void onSelected()
   {
      super.onSelected();
      view_.setFocus();
   }

   private String getSelectedCommands()
   {
      ArrayList<String> commands = view_.getSelectedCommands();
      StringBuilder cmd = new StringBuilder();
      for (String command : commands)
      {
         cmd.append(command);
         cmd.append("\n");
      }
      String commandString = cmd.toString();
      return commandString;
   }

   @Handler
   void onHistorySendToConsole()
   {
      String commandString = getSelectedCommands();
      commandString = StringUtil.chomp(commandString);
      if (commandString.length() > 0 )
         events_.fireEvent(new SendToConsoleEvent(commandString, false));
   }

   @Handler
   void onHistorySendToSource()
   {
      String commandString = getSelectedCommands();
      if (commandString.length() > 0)
         events_.fireEvent(new InsertSourceEvent(commandString, true));
   }

   void onLoadHistory()
   {
      view_.bringToFront();

      consoleDispatcher_.chooseFileThenExecuteCommand("Load History",
                                                      "loadhistory");
   }

   void onSaveHistory()
   {
      view_.bringToFront();

      consoleDispatcher_.saveFileAsThenExecuteCommand("Save History As",
                                                      ".Rhistory",
                                                      false,
                                                      "savehistory");
   }

   @Handler
   void onHistoryRemoveEntries()
   {
      // get selected indexes (bail if there is no selection)
      final ArrayList<Integer> selectedRowIndexes =
                              view_.getRecentCommandsSelectedRowIndexes();
      if (selectedRowIndexes.size() < 1)
      {
         globalDisplay_.showErrorMessage(
                              "Error",
                              "No history entries currently selected.");
         return;
      }

      // bring view to front
      view_.bringToFront();

      globalDisplay_.showYesNoMessage(
            GlobalDisplay.MSG_QUESTION,
            "Confirm Remove Entries",
            "Are you sure you want to remove the selected entries from " +
            "the history?",

            new ProgressOperation() {
               public void execute(final ProgressIndicator indicator)
               {
                  indicator.onProgress("Removing items...");

                  // for each selected row index we need to calculate
                  // the offset from the bottom
                  int rowCount = view_.getRecentCommandsRowsDisplayed();
                  JsArrayNumber bottomIndexes = (JsArrayNumber)
                                             JsArrayNumber.createArray();
                  for (int i = 0; i<selectedRowIndexes.size(); i++)
                     bottomIndexes.push(rowCount - selectedRowIndexes.get(i) - 1);

                  server_.removeHistoryItems(
                                    bottomIndexes,
                                    new VoidServerRequestCallback(indicator));
               }
            },

            true
         );


   }

   @Handler
   void onClearHistory()
   {
      view_.bringToFront();

      globalDisplay_.showYesNoMessage(
         GlobalDisplay.MSG_WARNING,
         "Confirm Clear History",
         "Are you sure you want to clear all history entries?",

         new ProgressOperation() {
            public void execute(final ProgressIndicator indicator)
            {
               indicator.onProgress("Clearing history...");
               server_.clearHistory(
                     new VoidServerRequestCallback(indicator));
            }
         },

         true
      );
   }

   @Handler
   void onHistoryDismissResults()
   {
      searchCommand_.dismissResults();
   }

   @Handler
   void onHistoryDismissContext()
   {
      view_.dismissContext();
   }

   @Handler
   void onHistoryShowContext()
   {
      ArrayList<Long> indexes = view_.getSelectedCommandIndexes();
      if (indexes.size() != 1)
         return;

      final String command = view_.getSelectedCommands().get(0);
      final Long min = indexes.get(0);
      final long max = indexes.get(indexes.size() - 1) + 1;
      final long start = Math.max(0, min - CONTEXT_LINES);
      final long end = max + CONTEXT_LINES;

      server_.getHistoryArchiveItems(
            start,
            end,
            new SimpleRequestCallback<RpcObjectList<HistoryEntry>>() {
               @Override
               public void onResponseReceived(RpcObjectList<HistoryEntry> response)
               {
                  ArrayList<HistoryEntry> entries = toList(response);
                  view_.showContext(command,
                                    entries,
                                    min - start,
                                    max - min);
               }
            });
   }

   private ArrayList<HistoryEntry> toList(RpcObjectList<HistoryEntry> response)
   {
      ArrayList<HistoryEntry> entries = new ArrayList<HistoryEntry>();
      for (int i = 0; i < response.length(); i++)
         entries.add(response.get(i));
      return entries;
   }

   private ArrayList<HistoryEntry> toRecentCommandsList(
                                             JsArrayString jsCommands)
   {
      ArrayList<HistoryEntry> commands = new ArrayList<HistoryEntry>();
      for (int i=0; i<jsCommands.length(); i++)
         commands.add(HistoryEntry.create(i, jsCommands.get(i)));
      return commands;
   }

   private ArrayList<HistoryEntry> toRecentCommandsList(
                                 RpcObjectList<HistoryEntry> response)
   {
      ArrayList<HistoryEntry> entries = new ArrayList<HistoryEntry>();
      for (int i = 0; i < response.length(); i++)
         entries.add(response.get(i));
      return entries;
   }



   public void onSelectionCommit(SelectionCommitEvent<Void> e)
   {
      onHistorySendToConsole();
   }

   public void onFetchCommands(FetchCommandsEvent event)
   {
      if (fetchingMoreCommands_)
         return;

      if (historyPosition_ == 0)
      {
         // This should rarely/never happen
         return;
      }

      long startIndex = Math.max(0, historyPosition_ - COMMAND_CHUNK_SIZE);
      long endIndex = historyPosition_;
      server_.getHistoryItems(startIndex, endIndex,
            new SimpleRequestCallback<RpcObjectList<HistoryEntry>>()
            {
               @Override
               public void onResponseReceived(RpcObjectList<HistoryEntry> response)
               {
                  ArrayList<HistoryEntry> entries =
                                                toRecentCommandsList(response);
                  view_.addRecentCommands(entries, true);
                  fetchingMoreCommands_ = false;

                  if (response.length() > 0)
                     historyPosition_ = response.get(0).getIndex();
                  else
                     historyPosition_ = 0; // this shouldn't happen

                  view_.setMoreCommands(Math.min(historyPosition_,
                                                 COMMAND_CHUNK_SIZE));
               }

               @Override
               public void onError(ServerError error)
               {
                  super.onError(error);
                  fetchingMoreCommands_ = false;
               }
            });
   }



   // This field indicates how far into the history stream we have reached.
   // When this value becomes 0, that means there is no more history to go
   // fetch.
   private long historyPosition_ = 0;

   private static final int COMMAND_CHUNK_SIZE = 300;
   private static final int CONTEXT_LINES = 50;
   private boolean fetchingMoreCommands_ = false;
   private final Display view_;
   private final EventBus events_;
   private final GlobalDisplay globalDisplay_;
   private final SearchCommand searchCommand_;
   private HistoryServerOperations server_;
   private final Session session_;
   private final ConsoleDispatcher consoleDispatcher_;
}
