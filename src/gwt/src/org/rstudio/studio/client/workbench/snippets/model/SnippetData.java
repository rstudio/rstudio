/*
 * SnippetsChangedEvent.java
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

package org.rstudio.studio.client.workbench.snippets.model;

import com.google.gwt.core.client.JavaScriptObject;

public class SnippetData extends JavaScriptObject
{
   protected SnippetData() {}
   
   public static native final SnippetData create(String mode, String contents) /*-{ 
      return {
         mode: mode,
         contents: contents
      }; 
    }-*/;
   
   public native final String getContents() /*-{ return this.contents; }-*/;
   public native final String getMode() /*-{ return this.mode; }-*/;
}
