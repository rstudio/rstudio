/*
 * CodeSearchResults.java
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
package org.rstudio.studio.client.workbench.codesearch.model;

import org.rstudio.core.client.jsonrpc.RpcObjectList;

import com.google.gwt.core.client.JavaScriptObject;

public class CodeSearchResults extends JavaScriptObject
{
   protected CodeSearchResults()
   {
      
   }

   public final native RpcObjectList<FileItem> getFileItems() /*-{
      return this.file_items;
   }-*/;
   
   public final native RpcObjectList<SourceItem> getSourceItems() /*-{
      return this.source_items;
   }-*/;

   public final native boolean getMoreAvailable() /*-{
      return this.more_available;
   }-*/;

}
