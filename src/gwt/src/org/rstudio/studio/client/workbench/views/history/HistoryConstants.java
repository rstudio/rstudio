/*
 * HistoryConstants.java
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
package org.rstudio.studio.client.workbench.views.history;

public interface HistoryConstants extends com.google.gwt.i18n.client.Messages {

    @DefaultMessage("Error While Retrieving History")
    @Key("errorRetrievingHistoryCaption")
    String errorRetrievingHistoryCaption();

    @DefaultMessage("Load History")
    @Key("loadHistoryCaption")
    String loadHistoryCaption();

    @DefaultMessage("Save History As")
    @Key("saveHistoryAsCaption")
    String saveHistoryAsCaption();

    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    @DefaultMessage("No history entries currently selected.")
    @Key("noHistoryEntriesSelectedMessage")
    String noHistoryEntriesSelectedMessage();

    @DefaultMessage("Confirm Remove Entries")
    @Key("confirmRemoveEntriesCaption")
    String confirmRemoveEntriesCaption();

    @DefaultMessage("Are you sure you want to remove the selected entries from the history?")
    @Key("confirmRemoveEntriesMessage")
    String confirmRemoveEntriesMessage();

    @DefaultMessage("Removing items...")
    @Key("removingItemsProgressMessage")
    String removingItemsProgressMessage();

    @DefaultMessage("Confirm Clear History")
    @Key("confirmClearHistoryCaption")
    String confirmClearHistoryCaption();

    @DefaultMessage("Are you sure you want to clear all history entries?")
    @Key("confirmClearHistoryMessage")
    String confirmClearHistoryMessage();

    @DefaultMessage("Clearing history...")
    @Key("clearingHistoryProgressMessage")
    String clearingHistoryProgressMessage();

    @DefaultMessage("History")
    @Key("historyTitle")
    String historyTitle();

    @DefaultMessage("Show command in original context")
    @Key("showCommandTitle")
    String showCommandTitle();

    @DefaultMessage("Load more entries...")
    @Key("loadMoreEntriesText")
    String loadMoreEntriesText();

    @DefaultMessage("Load {0} more entries")
    @Key("setMoreCommandsText")
    String setMoreCommandsText(long commands);

    @DefaultMessage("Search results: {0}")
    @Key("searchResultsText")
    String searchResultsText(String commands);

    @DefaultMessage("Showing command in context")
    @Key("showingCommandInContext")
    String showingCommandInContext();

    @DefaultMessage("Filter command history")
    @Key("filterCommandHistoryLabel")
    String filterCommandHistoryLabel();

    @DefaultMessage("History Tab")
    @Key("historyTabLabel")
    String historyTabLabel();

    @DefaultMessage("History Entry Table")
    @Key("historyEntryTableText")
    String historyEntryTableText();

}
