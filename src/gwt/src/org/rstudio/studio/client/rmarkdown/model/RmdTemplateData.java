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
            format_options: [ "toc", "self_contained", "smart", "theme", 
                              "highlight", "fig_width", "fig_height" ],
            format_notes: "HTML is recommended for authoring. You can switch to PDF or Word anytime."
            },
            {
            format_name: "pdf_document", 
            format_ui_name: "PDF",
            format_options: [ "toc", "number_sections", "highlight", 
                              "latex_engine", "fig_width", "fig_height" ]
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
            option_default: "true"
            },
            {
            option_name: "theme",
            option_ui_name: "Theme", 
            option_type: "choice", 
            option_default: "default",
            option_list: [ "default", "cerulean", "journal", "flatly",
                           "readable", "spacelab", "united", "yeti", "cosmo"]
            },
            {
            option_name: "highlight",
            option_ui_name: "Syntax highlighting", 
            option_type: "choice", 
            option_default: "default",
            option_list: [ "default", "tango", "pygments", "kate", "monochrome",
                           "espresso", "zenburn", "haddock", "textmate" ]
            },
            {
            option_name: "smart",
            option_ui_name: "Use smart punctuation", 
            option_type: "boolean", 
            option_default: "true"
            },
            {
            option_name: "number_sections",
            option_ui_name: "Number section headings", 
            option_type: "boolean", 
            option_default: "false"
            },
            {
            option_name: "latex_engine",
            option_ui_name: "LaTeX Engine", 
            option_type: "choice", 
            option_default: "pdflatex",
            option_list: [ "pdflatex", "lualatex", "xelatex" ]
            },
            {
            option_name: "fig_width",
            option_ui_name: "Default figure width in inches", 
            option_type: "float", 
            option_default: "7"
            },
            {
            option_name: "fig_height",
            option_ui_name: "Default figure height in inches", 
            option_type: "float", 
            option_default: "5"
            },
            {
            option_name: "fig_width",
            option_format: "pdf_document",
            option_ui_name: "Default figure width in inches", 
            option_type: "float", 
            option_default: "6"
            },
            {
            option_name: "fig_height",
            option_format: "pdf_document",
            option_ui_name: "Default figure height in inches", 
            option_type: "float", 
            option_default: "4.5"
            }
         ]
         },
         {
         template_name: "Presentation",
         template_formats: [
            {
            format_name: "revealjs_presentation",
            format_ui_name: "RevealJS",
            format_options: ["center", "incremental", "self_contained", "smart",
                             "fig_caption", "theme", "transition" ]
            },
            {
            format_name: "ioslides_presentation",
            format_ui_name: "IOSlides",
            format_options: [ "incremental", "smart", "widescreen", "smaller",
                              "fig_caption" ]
            },
            {
            format_name: "beamer_presentation",
            format_ui_name: "Beamer",
            format_options: [ "incremental", "fig_caption" ]
            }
         ],
         template_options: [
            {
            option_name: "center",
            option_ui_name: "Vertically center content on slides", 
            option_type: "boolean", 
            option_default: "false"
            },
            {
            option_name: "incremental",
            option_ui_name: "Render slide bullets incrementally", 
            option_type: "boolean", 
            option_default: "false"
            },
            {
            option_name: "self_contained",
            option_ui_name: "Create a standalone HTML presentation", 
            option_type: "boolean", 
            option_default: "true"
            },
            {
            option_name: "theme",
            option_ui_name: "Visual theme", 
            option_type: "choice", 
            option_default: "default",
            option_list: [ "default", "simple", "sky", "beige", "serif", 
                           "solarized" ]
            },
            {
            option_name: "transition",
            option_ui_name: "Slide transition", 
            option_type: "choice", 
            option_default: "default",
            option_list: [ "default", "cube", "page", "concave", "zoom", 
                           "linear", "fade", "none" ]
            },
            {
            option_name: "smart",
            option_ui_name: "Use smart punctuation", 
            option_type: "boolean", 
            option_default: "true"
            },
            {
            option_name: "widescreen",
            option_ui_name: "Display presentation with wider dimensions", 
            option_type: "boolean", 
            option_default: "false"
            },
            {
            option_name: "smaller",
            option_ui_name: "Use smaller text on all slides", 
            option_type: "boolean", 
            option_default: "false"
            },
            {
            option_name: "fig_caption",
            option_ui_name: "Render figures with captions", 
            option_type: "boolean", 
            option_default: "false"
            },
         ]
         }
   ];
   }-*/;
}
