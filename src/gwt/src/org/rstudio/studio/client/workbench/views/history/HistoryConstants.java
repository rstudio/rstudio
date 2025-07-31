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

    @Key("errorRetrievingHistoryCaption")
    String errorRetrievingHistoryCaption();

    @Key("loadHistoryCaption")
    String loadHistoryCaption();

    @Key("saveHistoryAsCaption")
    String saveHistoryAsCaption();

    @Key("errorCaption")
    String errorCaption();

    @Key("noHistoryEntriesSelectedMessage")
    String noHistoryEntriesSelectedMessage();

    @Key("confirmRemoveEntriesCaption")
    String confirmRemoveEntriesCaption();

    @Key("confirmRemoveEntriesMessage")
    String confirmRemoveEntriesMessage();

    @Key("removingItemsProgressMessage")
    String removingItemsProgressMessage();

    @Key("confirmClearHistoryCaption")
    String confirmClearHistoryCaption();

    @Key("confirmClearHistoryMessage")
    String confirmClearHistoryMessage();

    @Key("clearingHistoryProgressMessage")
    String clearingHistoryProgressMessage();

    @Key("historyTitle")
    String historyTitle();

    @Key("showCommandTitle")
    String showCommandTitle();

    @Key("loadMoreEntriesText")
    String loadMoreEntriesText();

    @Key("setMoreCommandsText")
    String setMoreCommandsText(long commands);

    @Key("searchResultsText")
    String searchResultsText(String commands);

    @Key("showingCommandInContext")
    String showingCommandInContext();

    @Key("filterCommandHistoryLabel")
    String filterCommandHistoryLabel();

    @Key("historyTabLabel")
    String historyTabLabel();

    @Key("historyEntryTableText")
    String historyEntryTableText();

}
