/*
 * RmdFrontMatter.java
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
import com.google.gwt.core.client.JsArrayString;

public class RmdFrontMatter extends JavaScriptObject
{
   protected RmdFrontMatter()
   {
   }
   
   public static final native RmdFrontMatter create() /*-{
      return {
         output: {}
      };
   }-*/;
   
   public final native void setAuthor(String author) /*-{
      this.author = author;
   }-*/;
   
   public final native void setTitle(String title) /*-{
      this.title = title;
   }-*/;
   
   public final native void setRuntime(String runtime) /*-{
      this.runtime = runtime;
   }-*/;

   public final native void addDate() /*-{
      // We use JavaScript to create a date string so the document picks up the
      // system locale's mechanism for formatting dates.
      //
      // IE 11 adds unprintable Unicode characters to the date string that we
      // need to remove for R Markdown. See case 4300 for details, including a
      // link to the issue reported against IE in early 2014 (unresolved as of
      // 3/2015)
      var date = (new Date()).toLocaleDateString().replace(/\u200e/g, "");
      
      // Remove periods as they interfere with rendering to PDF.
      // see: https://github.com/rstudio/rmarkdown/issues/145#issuecomment-47415718
      date = date.replace(/\./g, " ");
      date = date.replace(/\s+/g, " ");
      
      this.date = date;
   }-*/;
   
   public final native void addResourceFile(String file) /*-{
      if (typeof this.resource_files === "undefined")
         this.resource_files = [];
      else if (typeof this.resource_files === "string")
         this.resource_files = [ this.resource_files ];
      this.resource_files.push(file);
   }-*/;
   
   public final native JsArrayString getResourceFiles() /*-{
      if (typeof this.resource_files === "undefined")
         return [];
      else if (typeof this.resource_files === "string")
         return [ this.resource_files ];
      return this.resource_files;
   }-*/;
   
   public final native JsArrayString getFormatList() /*-{
      if (typeof this.output === "undefined")
         return [ "html_document" ];
      if (typeof this.output === "string")
         return [ this.output ];
      else
         return Object.getOwnPropertyNames(this.output);
   }-*/;

   public final native RmdFrontMatterOutputOptions getOutputOption(
         String format) /*-{
     if (typeof this.output === "undefined" || 
         this.output === format)
        return {}
        
     var options = this.output[format];
     if (typeof options === "undefined")
        return null;
     else if (options === "default")
        return {}
     else
        return options;
   }-*/;
   
   public final native void setOutputOption(String format, 
         JavaScriptObject options) /*-{
     
     // handle missing output type 
     if (typeof this.output === "undefined")
        this.output = {};
        
     // default format options
     if (Object.getOwnPropertyNames(options).length === 0)
     {
        if (typeof this.output === "string")
        {
           // already have a default output format -- convert to list
           var prevFormat = this.output;
           this.output = {};
           this.output[prevFormat] = "default";
        }
        if (typeof this.output === "object") 
        {
           if (Object.getOwnPropertyNames(this.output).length === 0)
           {
              // no existing output format, use this one with defaults
              this.output = format;
           }
           else
           {
              this.output[format] = "default";
           }
        }
     }
     else
     {
        if (typeof this.output === "string")
           this.output = {};
        this.output[format] = options;
     }
   }-*/;
   
   public final void applyCreateOptions(String author, String title, 
                                        String format, boolean isShiny)
   {
      setTitle(title);
      if (author.length() > 0)
      {
         setAuthor(author);
         addDate();
      }
      if (isShiny)
      {
         setRuntime(SHINY_RUNTIME);
      }
      if (format != null)
      {
         setOutputOption(format, RmdFrontMatterOutputOptions.create());
      }
   }
   
   public final static String OUTPUT_KEY = "output";
   public final static String RUNTIME_KEY = "runtime";
   public final static String KNIT_KEY = "knit";

   public final static String DEFAULT_FORMAT = "default";
   public final static String SHINY_RUNTIME = "shiny";
   public final static String SHINY_PRERENDERED_RUNTIME = "shiny_prerendered";
   public final static String FRONTMATTER_SEPARATOR = "---\n";
}
