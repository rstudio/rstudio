/*
 * SourceWindowParams.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.model;

import com.google.gwt.core.client.JavaScriptObject;

public class SourceWindowParams extends JavaScriptObject
{
   protected SourceWindowParams()
   {
   }
   
   public final static native SourceWindowParams create(String windowId, 
         SourceDocument document) /*-{
      return {
         source_window_id: windowId,
         doc: document
      };
   }-*/;
   
   public final native String getSourceWindowId() /*-{
      return this.source_window_id;
   }-*/;
   
   public final native SourceDocument getDoc() /*-{
      return this.doc;
   }-*/;
}
