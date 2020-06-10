/*
 * FindInFilesState.java
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
package org.rstudio.studio.client.workbench.views.output.find.model;

import com.google.gwt.core.client.JavaScriptObject;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.jsonrpc.RpcObjectList;

public class FindInFilesState extends JavaScriptObject
{
   protected FindInFilesState()
   {
   }

   public final boolean isTabVisible()
   {
      return !StringUtil.isNullOrEmpty(getHandle());
   }

   public native final String getHandle() /*-{
      return this.handle;
   }-*/;


   public native final RpcObjectList<FindResult> getResults() /*-{
      return this.results;
   }-*/;

   public native final boolean isRunning() /*-{
      return this.running;
   }-*/;

   public native final String getInput() /*-{
      return this.input;
   }-*/;

   public native final String getPath() /*-{
      return this.path;
   }-*/;

   public native final boolean isRegex() /*-{
      return this.regex;
   }-*/;

   public native final String getReplaceText() /*-{
      return this.replace;
   }-*/;

   public native final boolean isReplaceRegex() /*-{
      return this.replaceRegex;
   }-*/;
}
