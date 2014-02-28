/*
 * RmdFrontMatter.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
   
   public static final native RmdFrontMatter create(String titleText) /*-{
      return {
         title: titleText,
         output: {}
      };
   }-*/;
   
   public final native void setAuthor(String author) /*-{
      this.author = author;
   }-*/;

   public final native void addDate() /*-{
      this.date = (new Date()).toLocaleDateString();
   }-*/;
   
   public final native JsArrayString getFormatList() /*-{
      if (typeof this.output === "string")
         return [ this.output ];
      else
         return Object.getOwnPropertyNames(this.output);
   }-*/;

   public final native RmdFrontMatterOutputOptions getOutputOption(
         String format) /*-{
     if (this.output === format)
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
     if (Object.getOwnPropertyNames(options).length === 0)
     {
        if (typeof this.output === "string" ||
            Object.getOwnPropertyNames(this.output).length === 0)
           this.output = format;
        else
           this.output[format] = "default"
     }
     else
     {
        if (typeof this.output === "string")
           this.output = {};
        this.output[format] = options;
     }
   }-*/;
   
   public final static String OUTPUT_KEY = "output";
   public final static String DEFAULT_FORMAT = "default";
   public final static String FRONTMATTER_SEPARATOR = "---\n";
}
