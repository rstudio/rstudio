/*
 * RmdOutputFormat.java
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

public class RmdOutputFormat extends JavaScriptObject
{
   protected RmdOutputFormat() 
   {
   }
   
   public native final String getFormatName() /*-{
      return this.format_name;
   }-*/;
   
   public native final boolean isSelfContained() /*-{
      return this.self_contained;
   }-*/;

   // output format name strings from the rmarkdown package (not exhaustive)
   public final static String OUTPUT_HTML_DOCUMENT = "html_document";
   public final static String OUTPUT_HTML_NOTEBOOK = "html_notebook";
   public final static String OUTPUT_BEAMER_PRESENTATION = "beamer_presentation";
   public final static String OUTPUT_REVEALJS_PRESENTATION = "revealjs_presentation";
   public final static String OUTPUT_IOSLIDES_PRESENTATION = "ioslides_presentation";
   public final static String OUTPUT_SLIDY_PRESENTATION = "slidy_presentation";
   public final static String OUTPUT_PPT_PRESENTATION = "powerpoint_presentation";
   public final static String OUTPUT_PRESENTATION_SUFFIX = "_presentation";
   public final static String OUTPUT_DASHBOARD_SUFFIX = "_dashboard";
   public final static String OUTPUT_WORD_DOCUMENT = "word_document";
   public final static String OUTPUT_PDF_DOCUMENT = "pdf_document";
}

