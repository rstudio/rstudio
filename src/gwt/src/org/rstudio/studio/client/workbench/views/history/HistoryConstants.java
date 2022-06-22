/*
 * HistoryConstants.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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

public interface HistoryConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Error While Retrieving History".
     *
     * @return translated "Error While Retrieving History"
     */
    @DefaultMessage("Error While Retrieving History")
    @Key("errorRetrievingHistoryCaption")
    String errorRetrievingHistoryCaption();

    /**
     * Translated "Load History".
     *
     * @return translated "Load History"
     */
    @DefaultMessage("Load History")
    @Key("loadHistoryCaption")
    String loadHistoryCaption();

    /**
     * Translated "Save History As".
     *
     * @return translated "Save History As"
     */
    @DefaultMessage("Save History As")
    @Key("saveHistoryAsCaption")
    String saveHistoryAsCaption();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    /**
     * Translated "No history entries currently selected.".
     *
     * @return translated "No history entries currently selected."
     */
    @DefaultMessage("No history entries currently selected.")
    @Key("noHistoryEntriesSelectedMessage")
    String noHistoryEntriesSelectedMessage();

    /**
     * Translated "Confirm Remove Entries".
     *
     * @return translated "Confirm Remove Entries"
     */
    @DefaultMessage("Confirm Remove Entries")
    @Key("confirmRemoveEntriesCaption")
    String confirmRemoveEntriesCaption();

    /**
     * Translated "Are you sure you want to remove the selected entries from the history?".
     *
     * @return translated "Are you sure you want to remove the selected entries from the history?"
     */
    @DefaultMessage("Are you sure you want to remove the selected entries from the history?")
    @Key("confirmRemoveEntriesMessage")
    String confirmRemoveEntriesMessage();

    /**
     * Translated "Removing items...".
     *
     * @return translated "Removing items..."
     */
    @DefaultMessage("Removing items...")
    @Key("removingItemsProgressMessage")
    String removingItemsProgressMessage();

    /**
     * Translated "Confirm Clear History".
     *
     * @return translated "Confirm Clear History"
     */
    @DefaultMessage("Confirm Clear History")
    @Key("confirmClearHistoryCaption")
    String confirmClearHistoryCaption();

    /**
     * Translated "Are you sure you want to clear all history entries?".
     *
     * @return translated "Are you sure you want to clear all history entries?"
     */
    @DefaultMessage("Are you sure you want to clear all history entries?")
    @Key("confirmClearHistoryMessage")
    String confirmClearHistoryMessage();

    /**
     * Translated "Clearing history...".
     *
     * @return translated "Clearing history..."
     */
    @DefaultMessage("Clearing history...")
    @Key("clearingHistoryProgressMessage")
    String clearingHistoryProgressMessage();

    /**
     * Translated "History".
     *
     * @return translated "History"
     */
    @DefaultMessage("History")
    @Key("historyTitle")
    String historyTitle();

    /**
     * Translated "Show command in original context".
     *
     * @return translated "Show command in original context"
     */
    @DefaultMessage("Show command in original context")
    @Key("showCommandTitle")
    String showCommandTitle();


    /**
     * Translated "Load more entries...".
     *
     * @return translated "Load more entries..."
     */
    @DefaultMessage("Load more entries...")
    @Key("loadMoreEntriesText")
    String loadMoreEntriesText();

    /**
     * Translated "Load {0} more entries".
     *
     * @return translated "Load {0} more entries"
     */
    @DefaultMessage("Load {0} more entries")
    @Key("setMoreCommandsText")
    String setMoreCommandsText(long commands);

    /**
     * Translated "Search results: {0}".
     *
     * @return translated "Search results: {0}"
     */
    @DefaultMessage("Search results: {0}")
    @Key("searchResultsText")
    String searchResultsText(String commands);

    /**
     * Translated "Showing command in context".
     *
     * @return translated "Showing command in context"
     */
    @DefaultMessage("Showing command in context")
    @Key("showingCommandInContext")
    String showingCommandInContext();

    /**
     * Translated "Filter command history".
     *
     * @return translated "Filter command history"
     */
    @DefaultMessage("Filter command history")
    @Key("filterCommandHistoryLabel")
    String filterCommandHistoryLabel();

    /**
     * Translated "History Tab".
     *
     * @return translated "History Tab"
     */
    @DefaultMessage("History Tab")
    @Key("historyTabLabel")
    String historyTabLabel();

    /**
     * Translated "History Entry Table".
     *
     * @return translated "History Entry Table"
     */
    @DefaultMessage("History Entry Table")
    @Key("historyEntryTableText")
    String historyEntryTableText();

}
