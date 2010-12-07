/*
 * HistoryServerOperations.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.history.model;

import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.server.ServerRequestCallback;

public interface HistoryServerOperations
{  
   /*
    *  getHistory -- return a range of history items (note that startIndex
    *  is inclusive and endIndex is exclusive)
    */
   void getHistory(
         long startIndex, // inclusive
         long endIndex,   // exclusive
         ServerRequestCallback<RpcObjectList<HistoryEntry>> requestCallback);
   
   /*
    *  getRecentHistory - get up to maxEntries history items read from the 
    *  bottom of the history database (items are returned in index ascending
    *  order i.e. oldest ones first)
    */
   void getRecentHistory(
         long maxEntries,
         ServerRequestCallback<RpcObjectList<HistoryEntry>> requestCallback);
   
   /*
    *  searchHistory - search the history for the query (return up to
    *  maxEntries). the search is conducted beginning with the most recent
    *  history items and returned in index decsending order i.e. newest
    *  ones first)
    */
   void searchHistory(
         String query,  
         long maxEntries,
         ServerRequestCallback<RpcObjectList<HistoryEntry>> requestCallback);
   
   /*
    *  searchHistoryByPrefix - search the history for items with the 
    *  specified prefix (return up to maxEntries. the search is conducted
    *  beginning with the most recent history items and returned in index
    *  descending order i.e. newest ones first)
    */
   void searchHistoryByPrefix(
         String prefix,
         long maxEntries,
         ServerRequestCallback<RpcObjectList<HistoryEntry>> requestCallback);
}
