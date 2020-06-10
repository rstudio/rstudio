/*
 * CompileNotebookv2Options.java
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
package org.rstudio.studio.client.notebookv2;

import com.google.gwt.core.client.JavaScriptObject;

public class CompileNotebookv2Options extends JavaScriptObject
{
   public static final String FORMAT_DEFAULT = "html_document";
   public static final String FORMAT_HTML = "html_document";
   public static final String FORMAT_PDF = "pdf_document";
   public static final String FORMAT_WORD = "word_document";
  
   public static native CompileNotebookv2Options create(String format)
   /*-{
      return {
         format: format
      };
   }-*/;

   protected CompileNotebookv2Options()
   {}

   public native final String getFormat() /*-{
      return this.format;
   }-*/;
}
