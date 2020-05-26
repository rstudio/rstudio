/*
 * VCSApplicationParams.java
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
package org.rstudio.studio.client.vcs;

import java.util.ArrayList;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.common.vcs.StatusAndPath;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import org.rstudio.studio.client.common.vcs.StatusAndPathInfo;

public class VCSApplicationParams extends JavaScriptObject
{
   protected VCSApplicationParams()
   {
   }
   
   public final static VCSApplicationParams create(
                                          boolean showHistory,
                                          FileSystemItem historyFileFilter,
                                          ArrayList<StatusAndPath> selected)
   {
      JsArray<StatusAndPathInfo> jsSelected =
            JavaScriptObject.createArray().cast();
      
      jsSelected.setLength(selected.size());
      
      for (int i=0; i<selected.size(); i++)
         jsSelected.set(i, selected.get(i).toInfo());
      
      return createNative(showHistory, historyFileFilter, jsSelected);
   }
   
   private final static native VCSApplicationParams createNative(
                                       boolean showHistory,
                                       FileSystemItem historyFileFilter,
                                       JsArray<StatusAndPathInfo> selected) /*-{
      var params = new Object();
      params.show_history = showHistory;
      params.history_file_filter = historyFileFilter;
      params.selected = selected;
      return params;
   }-*/; 
   
  
   
   public final native boolean getShowHistory() /*-{
      return this.show_history;
   }-*/;

   public final native FileSystemItem getHistoryFileFilter() /*-{
      return this.history_file_filter;
   }-*/;
   
   public final ArrayList<StatusAndPath> getSelected()
   {
      return StatusAndPath.fromInfos(getSelectedNative());
   }
   
   private final native JsArray<StatusAndPathInfo> getSelectedNative() /*-{
      return this.selected;
   }-*/;


}
