/*
 * ProjectConfig.java
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
package org.rstudio.studio.client.workbench.views.source.model;

import com.google.gwt.core.client.JavaScriptObject;

public class ProjectConfig extends JavaScriptObject
{
   protected ProjectConfig()
   {
   }
   
   public native final int getTabSize()
   /*-{
      return this["tab_size"];
   }-*/;
   
   public native final boolean useSoftTabs()
   /*-{
      return this["use_soft_tabs"];
   }-*/;
   
   public native final boolean stripTrailingWhitespace()
   /*-{
      return this["strip_trailing_whitespace"];
   }-*/;
   
   public native final boolean ensureTrailingNewline()
   /*-{
      return this["ensure_trailing_newline"];
   }-*/;

}
