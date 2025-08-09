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

    /**
     * Translated "Error While Retrieving History".
     *
     * @return translated "Error While Retrieving History"
     */
    String errorRetrievingHistoryCaption();

    /**
     * Translated "Load History".
     *
     * @return translated "Load History"
     */
    String loadHistoryCaption();

    /**
     * Translated "Save History As".
     *
     * @return translated "Save History As"
     */
    String saveHistoryAsCaption();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    String errorCaption();

    /**
     * Translated "No history entries currently selected.".
     *
     * @return translated "No history entries currently selected."
     */
    String noHistoryEntriesSelectedMessage();

    /**
     * Translated "Confirm Remove Entries".
     *
     * @return translated "Confirm Remove Entries"
     */
    String confirmRemoveEntriesCaption();

    /**
     * Translated "Are you sure you want to remove the selected entries from the history?".
     *
     * @return translated "Are you sure you want to remove the selected entries from the history?"
     */
    String confirmRemoveEntriesMessage();

    /**
     * Translated "Removing items...".
     *
     * @return translated "Removing items..."
     */
    String removingItemsProgressMessage();

    /**
     * Translated "Confirm Clear History".
     *
     * @return translated "Confirm Clear History"
     */
    String confirmClearHistoryCaption();

    /**
     * Translated "Are you sure you want to clear all history entries?".
     *
     * @return translated "Are you sure you want to clear all history entries?"
     */
    String confirmClearHistoryMessage();

    /**
     * Translated "Clearing history...".
     *
     * @return translated "Clearing history..."
     */
    String clearingHistoryProgressMessage();

    /**
     * Translated "History".
     *
     * @return translated "History"
     */
    String historyTitle();

    /**
     * Translated "Show command in original context".
     *
     * @return translated "Show command in original context"
     */
    String showCommandTitle();


    /**
     * Translated "Load more entries...".
     *
     * @return translated "Load more entries..."
     */
    String loadMoreEntriesText();

    /**
     * Translated "Load {0} more entries".
     *
     * @return translated "Load {0} more entries"
     */
    String setMoreCommandsText(long commands);

    /**
     * Translated "Search results: {0}".
     *
     * @return translated "Search results: {0}"
     */
    String searchResultsText(String commands);

    /**
     * Translated "Showing command in context".
     *
     * @return translated "Showing command in context"
     */
    String showingCommandInContext();

    /**
     * Translated "Filter command history".
     *
     * @return translated "Filter command history"
     */
    String filterCommandHistoryLabel();

    /**
     * Translated "History Tab".
     *
     * @return translated "History Tab"
     */
    String historyTabLabel();

    /**
     * Translated "History Entry Table".
     *
     * @return translated "History Entry Table"
     */
    String historyEntryTableText();

}
