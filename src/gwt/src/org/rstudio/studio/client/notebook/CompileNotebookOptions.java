/*
 * CompileNotebookOptions.java
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
package org.rstudio.studio.client.notebook;

import com.google.gwt.core.client.JavaScriptObject;

public class CompileNotebookOptions extends JavaScriptObject
{
   public static final String TYPE_DEFAULT = "default";
   public static final String TYPE_STITCH = "stitch";
   public static final String TYPE_SPIN = "spin";
   
   public static native CompileNotebookOptions create(String id,
                                                      String prefix,
                                                      String suffix,
                                                      boolean sessionInfo,
                                                      String notebookTitle,
                                                      String notebookAuthor,
                                                      String notebookType)
   /*-{
      return {
         id: id,
         prefix: prefix,
         suffix: suffix,
         session_info: sessionInfo,
         notebook_title: notebookTitle,
         notebook_author: notebookAuthor,
         notebook_type: notebookType
      };
   }-*/;

   protected CompileNotebookOptions()
   {}

   public native final String getId() /*-{
      return this.id;
   }-*/;

   public native final String getPrefix() /*-{
      return this.prefix;
   }-*/;

   public native final String getSuffix() /*-{
      return this.suffix;
   }-*/;

   public native final boolean getSessionInfo() /*-{
      return this.session_info;
   }-*/;

   public native final String getNotebookTitle() /*-{
      return this.notebook_title;
   }-*/;

   public native final String getNotebookAuthor() /*-{
      return this.notebook_author;
   }-*/;
   
   public native final String getNotebookType() /*-{
      return this.notebook_type;
   }-*/;
}
