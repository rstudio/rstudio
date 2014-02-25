/*
 * RmdTemplateData.java
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

import com.google.gwt.core.client.JsArray;

public class RmdTemplateData
{
   public static native JsArray<RmdTemplate> getTemplates() /*-{
      return [ 
         {
         template_name: "Document", 
         template_formats: [ 
            {
            format_name: "html_document",
            format_ui_name: "HTML",
            format_options: [ "toc", "self_contained" ]
            },
            {
            format_name: "pdf_document", 
            format_ui_name: "PDF",
            format_options: [ "toc" ]
            } 
         ],
         template_options: [ 
            {
            option_name: "toc",
            option_ui_name: "Include table of contents", 
            option_type: "boolean", 
            option_default: "false"
            },
            {
            option_name: "self_contained",
            option_ui_name: "Create a standalone HTML document", 
            option_type: "boolean", 
            option_default: "false"
            }
         ]
         },
         {
         template_name: "Presentation",
         template_formats: [
            {
            format_name: "ioslides_presentation",
            format_ui_name: "IOSlides",
            format_options: []
            }
         ],
         template_options: []
         }
   ];
   }-*/;
}
