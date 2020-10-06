/*
 * HistoryServerOperations.java
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
package org.rstudio.studio.client.workbench.views.history.model;

import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.JsArrayNumber;

public interface HistoryServerOperations
{  
   /*
    *  getRecentHistory -- return all of the available history items (up to max)
    */
   void getRecentHistory(
         long maxItems,
         ServerRequestCallback<RpcObjectList<HistoryEntry>> requestCallback);
   
   /*
    *  getHistoryItems -- get a subset of history items
    */
   void getHistoryItems(
         long startIndex, // inclusive
         long endIndex,    // exclusive
         ServerRequestCallback<RpcObjectList<HistoryEntry>> requestCallback);
   
   /*
    *  removeHistoryItems -- indexes of items to remove 
    */
   void removeHistoryItems(JsArrayNumber itemIndexes, 
                           ServerRequestCallback<Void> requestCallback);
   
   
   /*
    *  searchHistory - search the project history for the query  (return up to
    *  maxEntries). the search is conducted beginning with the most recent
    *  history items and returned in descending order (i.e. newest ones
    *  first)
    */
   void searchHistory (
         String query,
         long maxEntries,
         ServerRequestCallback<RpcObjectList<HistoryEntry>> requestCallback);

   /*
    *  clearHistory -- clear the entire history 
    */
   void clearHistory(ServerRequestCallback<Void> requestCallback);
   
   
   /*
    *  getHistoryArchiveItems -- return a range of history archive items 
    *  (note that startIndex is inclusive and endIndex is exclusive)
    */
   void getHistoryArchiveItems(
         long startIndex, // inclusive
         long endIndex,   // exclusive
         ServerRequestCallback<RpcObjectList<HistoryEntry>> requestCallback);
   
  
   /*
    *  searchHistoryDatabase - search the history archive for the query 
    *  (return up to maxEntries). the search is conducted beginning with the
    *  most recent history items and returned in index descending order i.e. newest
    *  ones first)
    */
   void searchHistoryArchive(
         String query,  
         long maxEntries,
         ServerRequestCallback<RpcObjectList<HistoryEntry>> requestCallback);
   
   /*
    *  searchHistoryArchiveByPrefix - search the history for items with the 
    *  specified prefix (return up to maxEntries. the search is conducted
    *  beginning with the most recent history items and returned in index
    *  descending order i.e. newest ones first)
    */
   void searchHistoryArchiveByPrefix(
         String prefix,
         long maxEntries,
         boolean uniqueOnly,
         ServerRequestCallback<RpcObjectList<HistoryEntry>> requestCallback);
}
