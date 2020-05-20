/*
 * NotebookExecRange.java
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
package org.rstudio.studio.client.rmarkdown.model;

import com.google.gwt.core.client.JavaScriptObject;

public class NotebookExecRange extends JavaScriptObject
{
   protected NotebookExecRange()
   {
   }
   
   public final static native NotebookExecRange create(int start, int stop) /*-{
      return {
         start: start,
         stop:  stop
      };
   }-*/;
   
   public final native int getStart() /*-{
      return this.start;
   }-*/;

   public final native int getStop() /*-{
      return this.stop;
   }-*/;
   
   public final native void extendTo(NotebookExecRange other) /*-{
      this.start = Math.min(this.start, other.start);
      this.stop  = Math.max(this.stop, other.stop);
   }-*/;
}
