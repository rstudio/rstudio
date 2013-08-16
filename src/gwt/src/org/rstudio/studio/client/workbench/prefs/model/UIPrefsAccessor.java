/*
 * UIPrefsAccessor.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.prefs.model;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.notebook.CompileNotebookPrefs;
import org.rstudio.studio.client.workbench.ui.PaneConfig;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.SavePlotAsPdfOptions;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceThemes;

import com.google.gwt.core.client.JsArrayString;

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

   public PrefValue<Boolean> blinkingCursor()
   {
      return bool("blinking_cursor", true);
   }
   
   public PrefValue<Integer> printMarginColumn()
   {
      return integer("print_margin_column", 80);
   }
   
   public PrefValue<Boolean> showInvisibles()
   {
      return bool("show_invisibles", false);
   }

   public PrefValue<Boolean> showIndentGuides()
   {
      return bool("show_indent_guides", false);
   }
   
   public PrefValue<Boolean> useVimMode()
   {
      return bool("use_vim_mode", false);
   }
   
   public PrefValue<Boolean> insertMatching()
   {
      return bool("insert_matching", true);
   }
   
   public PrefValue<Boolean> reindentOnPaste()
   {
      return bool("reindent_on_paste", true);
   }
   
   public PrefValue<Boolean> verticallyAlignArgumentIndent()
   {
      return bool("valign_argument_indent", true);
   }

   public PrefValue<Boolean> softWrapRFiles()
   {
      return bool("soft_wrap_r_files", false);
   }
   
   public PrefValue<Boolean> focusConsoleAfterExec()
   {
      return bool("focus_console_after_exec", false);
   }
   
   public PrefValue<Boolean> syntaxColorConsole()
   {
      return bool("syntax_color_console", false);
   }
   
   public PrefValue<Boolean> saveAllBeforeBuild()
   {
      return bool("save_files_before_build", false);
   }

   public PrefValue<Double> fontSize()
   {
      return dbl("font_size_points", 10.0);
   }

   public PrefValue<String> theme()
   {
      return string("theme", null);
   }
   
   public String getThemeErrorClass()
   {    
      if ((theme().getValue() == null) ||
          AceThemes.TEXTMATE.equals(theme().getValue()))
         return " ace_constant";
      else  
         return " ace_constant ace_language";
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
   
   public PrefValue<Boolean> clearHidden()
   {
      return bool("clear_hidden", false);
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
   
   public PrefValue<CompileNotebookPrefs> compileNotebookOptions()
   {
      return object("compile_notebook_options",
                    CompileNotebookPrefs.createDefault());
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
   
   public PrefValue<String> rootDocument()
   {
      return string("root_document", "");
   }
   
   public PrefValue<Boolean> useRoxygen()
   {
      return bool("use_roxygen", false);
   }
   
   public static final String PDF_PREVIEW_NONE = "none";
   public static final String PDF_PREVIEW_RSTUDIO = "rstudio";
   public static final String PDF_PREVIEW_DESKTOP_SYNCTEX = "desktop-synctex";
   public static final String PDF_PREVIEW_SYSTEM = "system";
   
   public PrefValue<String> pdfPreview()
   {
      return string("pdf_previewer", getDefaultPdfPreview());
   }
   
   public PrefValue<Boolean> alwaysEnableRnwConcordance()
   {
      return bool("always_enable_concordance", true);
   }
   
   public PrefValue<Boolean> insertNumberedLatexSections()
   {
      return bool("insert_numbered_latex_sections", false);
   }
  
   public PrefValue<String> spellingDictionaryLanguage()
   {
      return string("spelling_dictionary_language", "en_US");
   }
   
   public PrefValue<JsArrayString> spellingCustomDictionaries()
   {
      return object("spelling_custom_dictionaries", 
                    JsArrayString.createArray().<JsArrayString>cast());
   }
   
   public PrefValue<Boolean> ignoreWordsInUppercase()
   {
      return bool("ignore_uppercase_words", true);
   }
   
   public PrefValue<Boolean> ignoreWordsWithNumbers()
   {
      return bool("ignore_words_with_numbers", true);
   }  
   
   public PrefValue<Boolean> navigateToBuildError()
   {
      return bool("navigate_to_build_error", true);
   }
   
   public PrefValue<Boolean> packagesPaneEnabled()
   {
      return bool("packages_pane_enabled", true);
   }
   
   public PrefValue<Boolean> useRcppTemplate()
   {
      return bool("use_rcpp_template", true);
   }
   
   public PrefValue<Boolean> restoreSourceDocuments()
   {
      return bool("restore_source_documents", true);
   }
   
   public PrefValue<Boolean> handleErrorsInUserCodeOnly()
   {
      return bool("handle_errors_in_user_code_only", true);
   }
   
   public PrefValue<Boolean> autoExpandErrorTracebacks()
   {
      return bool("auto_expand_error_tracebacks", false);
   }
   
   private String getDefaultPdfPreview()
   {
      if (Desktop.isDesktop())
      {
         // if there is a desktop synctex viewer available then default to it
         if (Desktop.getFrame().getDesktopSynctexViewer().length() > 0)
         {
            return PDF_PREVIEW_DESKTOP_SYNCTEX;
         }
         
         // otherwise default to the system viewer on linux and the internal 
         // viewer on mac (windows will always have a desktop synctex viewer)
         else
         {
            if (BrowseCap.isLinux())
            {
               return PDF_PREVIEW_SYSTEM;
            }
            else
            {
               return PDF_PREVIEW_RSTUDIO;
            }
         }
      }
      
      // web mode -- always default to internal viewer
      else
      {
         return PDF_PREVIEW_RSTUDIO;
      }
   }
}
