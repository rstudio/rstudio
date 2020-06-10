/*
 * RmdTemplateData.java
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

import com.google.gwt.core.client.JsArray;

public class RmdTemplateData
{
   public static native JsArray<RmdTemplate> getTemplates() /*-{
      return [ 
         {
         template_name: "Document", 
         template_formats: [ 
            {
            format_name: "html_notebook",
            format_ui_name: "Notebook",
            format_extension: ".nb.html", 
            format_options: [ "toc", "toc_depth", "code_folding", "highlight", 
                              "theme", "css", "fig_width", "fig_height", 
                              "fig_caption", "number_sections", "smart" ],
            format_notes: ""
            },
            {
            format_name: "html_document",
            format_ui_name: "HTML",
            format_extension: "html",
            format_options: [ "toc", "toc_depth", "highlight", "theme", "css", "fig_width", 
                              "fig_height", "fig_caption", "number_sections",
                              "smart", "self_contained", "keep_md", "df_print" ],
            format_notes: "Recommended format for authoring (you can switch to PDF or Word output anytime)."
            },
            {
            format_name: "pdf_document", 
            format_ui_name: "PDF",
            format_extension: "pdf",
            format_options: [ "toc", "toc_depth", "fig_width", "fig_height",
                              "fig_caption", "fig_crop", "number_sections", 
                              "latex_engine", "keep_tex", "highlight" ],
            format_notes: "PDF output requires TeX (MiKTeX on Windows, MacTeX 2013+ on OS X, TeX Live 2013+ on Linux)."
            },
            {
            format_name: "word_document", 
            format_ui_name: "Word",
            format_extension: "docx",
            format_options: [ "toc", "toc_depth", "highlight", "fig_width", "fig_height", 
                              "fig_caption", "keep_md" ],
            format_notes: "Previewing Word documents requires an installation of MS Word (or Libre/Open Office on Linux)."
            },
         ],
         template_options: [ 
            {
            option_name: "toc",
            option_ui_name: "Include table of contents", 
            option_type: "boolean", 
            option_transferable: true,
            option_default: "false"
            },
            {
            option_name: "toc_depth",
            option_ui_name: "Depth of headers for table of contents", 
            option_type: "integer", 
            option_transferable: true,
            option_default: "3"
            },
            {
            option_name: "self_contained",
            option_ui_name: "Create a standalone HTML document", 
            option_type: "boolean", 
            option_category: "Advanced",
            option_default: "true"
            },
            {
            option_name: "theme",
            option_ui_name: "Apply theme", 
            option_type: "choice", 
            option_default: "default",
            option_nullable: true,
            option_list: [ "default", "cerulean", "journal", "flatly", "readable", "spacelab", 
                           "united", "cosmo", "lumen", "paper", "sandstone", "simplex", "yeti" ]
            },
            {
            option_name: "highlight",
            option_ui_name: "Syntax highlighting", 
            option_type: "choice", 
            option_nullable: true,
            option_default: "default",
            option_list: [ "default", "tango", "pygments", "kate", "monochrome",
                           "espresso", "zenburn", "haddock"]
            },
            {
            option_name: "highlight",
            option_ui_name: "Syntax highlighting", 
            option_format: "html_document",
            option_type: "choice", 
            option_nullable: true,
            option_default: "default",
            option_list: [ "default", "tango", "pygments", "kate", "monochrome",
                           "espresso", "zenburn", "haddock", "textmate" ]
            },
            {
            option_name: "df_print",
            option_ui_name: "Print dataframes as",
            option_format: "html_document",
            option_type: "choice", 
            option_nullable: false,
            option_default: "paged",
            option_list: [ "default", "kable", "paged", "tibble" ],
            option_add_header: true
            },
            {
            option_name: "smart",
            option_ui_name: "Use smart punctuation", 
            option_type: "boolean", 
            option_category: "Advanced",
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
            option_for_create: false,
            option_category: "Advanced",
            option_default: "pdflatex",
            option_list: [ "pdflatex", "lualatex", "xelatex" ]
            },
            {
            option_name: "keep_md",
            option_ui_name: "Keep markdown source file", 
            option_type: "boolean", 
            option_for_create: false,
            option_category: "Advanced",
            option_default: "false",
            },
            {
            option_name: "keep_tex",
            option_ui_name: "Keep tex source file used to produce PDF", 
            option_type: "boolean", 
            option_for_create: false,
            option_category: "Advanced",
            option_default: "false",
            },
            {
            option_name: "fig_width",
            option_format: "html_document",
            option_ui_name: "Default figure width in inches", 
            option_type: "float", 
            option_category: "Figures",
            option_default: "7"
            },
            {
            option_name: "fig_height",
            option_format: "html_document",
            option_ui_name: "Default figure height in inches", 
            option_type: "float", 
            option_category: "Figures",
            option_default: "5"
            },
            {
            option_name: "fig_width",
            option_ui_name: "Default figure width in inches", 
            option_type: "float", 
            option_category: "Figures",
            option_default: "6"
            },
            {
            option_name: "fig_height",
            option_ui_name: "Default figure height in inches", 
            option_type: "float", 
            option_category: "Figures",
            option_default: "4.5"
            },
            {
            option_name: "fig_crop",
            option_ui_name: "Crop figures with pdfcrop (if available)", 
            option_type: "boolean", 
            option_category: "Figures",
            option_default: "true"
            },
            {
            option_name: "fig_caption",
            option_ui_name: "Render figures with captions", 
            option_category: "Figures",
            option_type: "boolean", 
            option_default: "false"
            },
            {
            option_name: "css",
            option_ui_name: "Apply CSS file", 
            option_type: "file", 
            option_nullable: true,
            option_default: "null"
            },
            {
            option_name: "code_folding",
            option_ui_name: "Fold R code chunks", 
            option_type: "choice", 
            option_default: "show",
            option_list: [ "none", "hide", "show" ]
            },
         ]
         },
         {
         template_name: "Presentation",
         template_formats: [
            {
            format_name: "ioslides_presentation",
            format_ui_name: "HTML (ioslides)",
            format_extension: "html",
            format_options: [ "logo", "fig_width", "fig_height", "fig_caption", 
                              "fig_retina", "incremental", "smaller",
                              "widescreen", "highlight", "transition", 
                              "self_contained", "smart", "keep_md" ],
            format_notes: "HTML presentation viewable with any browser (you can also print ioslides to PDF with Chrome)."
            },
            {
            format_name: "slidy_presentation",
            format_ui_name: "HTML (Slidy)",
            format_extension: "html",
            format_options: [ "fig_width", "fig_height", "fig_caption", 
                              "fig_retina", "incremental",
                              "highlight", "css",
                              "self_contained", "smart", "keep_md" ],
            format_notes: "HTML presentation viewable with any browser (you can also print Slidy to PDF with Chrome)."
            },
            {
            format_name: "beamer_presentation",
            format_ui_name: "PDF (Beamer)",
            format_extension: "pdf",
            format_options: [ "toc", "incremental", "theme", "colortheme", 
                              "fonttheme", "fig_width", "fig_height", 
                              "fig_crop", "fig_caption", "highlight", 
                              "keep_tex"],
            format_notes: "PDF output requires TeX (MiKTeX on Windows, MacTeX 2013+ on OS X, TeX Live 2013+ on Linux)."
            },
            {
            format_name: "powerpoint_presentation",
            format_ui_name: "PowerPoint",
            format_extension: "pptx",
            format_options: [ "toc", "fig_width", "fig_height", "fig_caption", 
                              "df_print", "smart"],
            format_notes: "PowerPoint previewing requires an installation of PowerPoint or OpenOffice."
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
            option_transferable: true,
            option_default: "false"
            },
            {
            option_name: "self_contained",
            option_ui_name: "Create a standalone HTML presentation", 
            option_type: "boolean", 
            option_category: "Advanced",
            option_default: "true"
            },
            {
            option_name: "theme",
            option_ui_name: "Theme", 
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
            option_name: "transition",
            option_ui_name: "Slide transition speed", 
            option_format: "ioslides_presentation", 
            option_type: "choice", 
            option_default: "default",
            option_list: [ "default", "slower", "faster" ]
            },
            {
            option_name: "smart",
            option_ui_name: "Use smart punctuation", 
            option_type: "boolean", 
            option_category: "Advanced",
            option_default: "true"
            },
            {
            option_name: "widescreen",
            option_ui_name: "Use widescreen dimensions", 
            option_type: "boolean", 
            option_category: "Advanced",
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
            option_category: "Figures",
            option_default: "false"
            },
            {
            option_name: "fig_width",
            option_ui_name: "Default figure width (in inches)", 
            option_category: "Figures",
            option_type: "float", 
            option_default: "7.5"
            },
            {
            option_name: "fig_height",
            option_ui_name: "Default figure height (in inches)",
            option_category: "Figures",
            option_type: "float", 
            option_default: "4.5"
            },
            {
            option_name: "fig_width",
            option_format: "beamer_presentation",
            option_ui_name: "Default figure width (in inches)", 
            option_category: "Figures",
            option_type: "float", 
            option_default: "10"
            },
            {
            option_name: "fig_height",
            option_format: "beamer_presentation",
            option_ui_name: "Default figure height (in inches)",
            option_category: "Figures",
            option_type: "float", 
            option_default: "7"
            },
            {
            option_name: "highlight",
            option_ui_name: "Syntax highlighting", 
            option_type: "choice", 
            option_transferable: true,
            option_nullable: true,
            option_default: "default",
            option_list: [ "default", "tango", "pygments", "kate", "monochrome",
                           "espresso", "zenburn", "haddock"]
            },
            {
            option_name: "toc",
            option_ui_name: "Include a table of contents", 
            option_type: "boolean", 
            option_default: "false"
            },
            {
            option_name: "fig_crop",
            option_ui_name: "Crop figures with pdfcrop (if available)", 
            option_type: "boolean", 
            option_category: "Figures",
            option_default: "true"
            },
            {
            option_name: "theme",
            option_format: "beamer_presentation",
            option_ui_name: "Theme", 
            option_type: "choice", 
            option_default: "default",
            option_list: [ "AnnArbor", "Antibes", "Bergen", "Berkeley",
                           "Berlin", "Boadilla", "boxes", "CambridgeUS", 
                           "Copenhagen", "Darmstadt", "default", "Dresden", 
                           "Frankfurt", "Goettingen", "Hannover", "Ilmenau", 
                           "JuanLesPins", "Luebeck", "Madrid", "Malmoe", 
                           "Marburg", "Montpellier", "PaloAlto", "Pittsburgh", 
                           "Rochester", "Singapore", "Szeged", "Warsaw" ]
            },
            {
            option_name: "fonttheme",
            option_ui_name: "Font theme", 
            option_type: "choice", 
            option_default: "default",
            option_list: [ "default", "professionalfonts", "serif", 
                           "structurebold", "structureitalicserif", 
                           "structuresmallcapsserif" ]
            },
            {
            option_name: "colortheme",
            option_ui_name: "Color theme", 
            option_type: "choice", 
            option_default: "default",
            option_list: [ "albatross", "beaver", "beetle", "crane", "default", 
                           "dolphin", "dove", "fly", "lily", "orchid", "rose", 
                           "seagull", "seahorse", "sidebartab", "structure", 
                           "whale", "wolverine" ]
            },
            {
            option_name: "logo",
            option_ui_name: "Show logo (square, at least 128x128)", 
            option_type: "file", 
            option_nullable: true,
            option_default: "null"
            },
            {
            option_name: "fig_retina",
            option_ui_name: "Figure scaling for Retina displays", 
            option_type: "float", 
            option_nullable: true,
            option_category: "Figures",
            option_default: "2"
            },
            {
            option_name: "keep_md",
            option_ui_name: "Keep markdown source file", 
            option_type: "boolean", 
            option_for_create: false,
            option_category: "Advanced",
            option_default: "false",
            },
            {
            option_name: "keep_tex",
            option_ui_name: "Keep tex source file used to produce PDF", 
            option_type: "boolean", 
            option_category: "Advanced",
            option_default: "false",
            },
            {
            option_name: "css",
            option_ui_name: "Apply CSS file", 
            option_type: "file", 
            option_nullable: true,
            option_default: "null"
            },
         ]
         }
      ];
   }-*/;
   
   public final static String DOCUMENT_TEMPLATE = "Document";
   public final static String PRESENTATION_TEMPLATE = "Presentation";
}
