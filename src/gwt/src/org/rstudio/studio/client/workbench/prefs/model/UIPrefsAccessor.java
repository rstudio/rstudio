/*
 * UIPrefsAccessor.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.prefs.model;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.workbench.ui.PaneConfig;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.SavePlotAsPdfOptions;

public class UIPrefsAccessor extends Prefs
{
   public UIPrefsAccessor(JsObject uiPrefs, JsObject projectUiPrefs)
   {
      super(uiPrefs, projectUiPrefs);
   }

   public PrefValue<Boolean> showLineNumbers()
   {
      return bool("show_line_numbers", true);
   }

   public PrefValue<Boolean> highlightSelectedWord()
   {
      return bool("highlight_selected_word", true);
   }

   public PrefValue<Boolean> highlightSelectedLine()
   {
      return bool("highlight_selected_line", false);
   }

   public PrefValue<PaneConfig> paneConfig()
   {
      return object("pane_config");
   }

   // NOTE: UserSettings.cpp depends on the name of this value
   public PrefValue<Boolean> useSpacesForTab()
   {
      return bool("use_spaces_for_tab", true);
   }

   // NOTE: UserSettings.cpp depends on the name of this value
   public PrefValue<Integer> numSpacesForTab()
   {
      return integer("num_spaces_for_tab", 2);
   }

   public PrefValue<Boolean> showMargin()
   {
      return bool("show_margin", false);
   }

   public PrefValue<Integer> printMarginColumn()
   {
      return integer("print_margin_column", 80);
   }

   public PrefValue<Boolean> insertMatching()
   {
      return bool("insert_matching", true);
   }

   public PrefValue<Boolean> softWrapRFiles()
   {
      return bool("soft_wrap_r_files", false);
   }

   public PrefValue<Boolean> syntaxColorConsole()
   {
      return bool("syntax_color_console", false);
   }

   public PrefValue<Double> fontSize()
   {
      return dbl("font_size_points", 10.0);
   }

   public PrefValue<String> theme()
   {
      return string("theme", null);
   }

   // NOTE: UserSettings.cpp depends on the name of this value
   public PrefValue<String> defaultEncoding()
   {
      return string("default_encoding", "");
   }
   
   public PrefValue<String> defaultProjectLocation()
   {
      return string("default_project_location", FileSystemItem.HOME_PATH);
   }
   
   public PrefValue<Boolean> toolbarVisible()
   {
      return bool("toolbar_visible", true);
   }
   
   public PrefValue<Boolean> sourceWithEcho()
   {
      return bool("source_with_echo", false);
   }
   
   public PrefValue<ExportPlotOptions> exportPlotOptions()
   {
      return object("export_plot_options", ExportPlotOptions.createDefault());
   }
   
   public PrefValue<SavePlotAsPdfOptions> savePlotAsPdfOptions()
   {
      return object("save_plot_as_pdf_options",
                    SavePlotAsPdfOptions.createDefault());
   }
   
   public PrefValue<Boolean> newProjGitInit()
   {
      return bool("new_proj_git_init", false);
   }
   
   public PrefValue<String> defaultSweaveEngine()
   {
      return string("default_sweave_engine", "Sweave");
   }
   
   public PrefValue<String> defaultLatexProgram()
   {
      return string("default_latex_program", "pdfLaTeX");
   }
   
   public PrefValue<Boolean> useTexi2Dvi()
   {
      return bool("use_texi2dvi", true);
   }
   
}
