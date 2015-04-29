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

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.notebook.CompileNotebookPrefs;
import org.rstudio.studio.client.notebookv2.CompileNotebookv2Prefs;
import org.rstudio.studio.client.rmarkdown.RmdOutput;
import org.rstudio.studio.client.rsconnect.model.RSConnectAccount;
import org.rstudio.studio.client.shiny.model.ShinyViewerType;
import org.rstudio.studio.client.workbench.exportplot.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.ui.PaneConfig;
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
   
   public PrefValue<Boolean> continueCommentsOnNewline()
   {
      return bool("continue_comments_on_newline", false);
   }
   
   public PrefValue<Boolean> useVimMode()
   {
      return bool("use_vim_mode", false);
   }
   
   public PrefValue<Boolean> insertMatching()
   {
      return bool("insert_matching", true);
   }
   
   public static final String COMPLETION_ALWAYS = "always";
   public static final String COMPLETION_WHEN_TRIGGERED = "triggered";
   public static final String COMPLETION_MANUAL = "manual";
   
   public PrefValue<Boolean> allowTabMultilineCompletion()
   {
      return bool("tab_multiline_completion", false);
   }
   
   public PrefValue<Boolean> enableSnippets()
   {
      return bool("enable_snippets", true);
   }
 
   public PrefValue<String> codeComplete()
   {
      return string("code_complete", COMPLETION_ALWAYS);
   }
   
   public PrefValue<String> codeCompleteOther()
   {
      return string("code_complete_other", COMPLETION_ALWAYS);
   }
   
   public PrefValue<Boolean> alwaysCompleteInConsole()
   {
      return bool("always_complete_console", true);
   }
   
   public PrefValue<Integer> alwaysCompleteDelayMs()
   {
      return integer("always_complete_delay", 250);
   }
   
   public PrefValue<Integer> alwaysCompleteCharacters()
   {
      return integer("always_complete_characters", 3);
   }
   
   public PrefValue<Boolean> insertParensAfterFunctionCompletion()
   {
      return bool("insert_parens_after_function_completion", true);
   }
   
   public PrefValue<Boolean> insertSpacesAroundEquals()
   {
      return bool("insert_spaces_around_equals", true);
   }
   
   public PrefValue<Boolean> showSignatureTooltips()
   {
      return bool("show_signature_tooltips", true);
   }
   
   public PrefValue<Boolean> showDiagnosticsR()
   {
      return bool("show_diagnostics_r", true);
   }
   
   public PrefValue<Boolean> showDiagnosticsCpp()
   {
      return bool("show_diagnostics_cpp", true);
   }
   
   public PrefValue<Boolean> showDiagnosticsOther()
   {
      return bool("show_diagnostics_other", true);
   }
   
   public PrefValue<Boolean> enableStyleDiagnostics()
   {
      return bool("enable_style_diagnostics", false);
   }
   
   public PrefValue<Boolean> diagnosticsOnSave()
   {
      return bool("diagnostics_on_save", true);
   }
   
   public PrefValue<Boolean> enableBackgroundDiagnostics()
   {
      return bool("enable_background_diagnostics", true);
   }
   
   public PrefValue<Integer> backgroundDiagnosticsDelayMs()
   {
      return integer("background_diagnostics_delay_ms", 2000);
   }
   
   public PrefValue<Boolean> diagnosticsInRFunctionCalls()
   {
      return bool("diagnostics_in_function_calls", true);
   }
   
   public PrefValue<Boolean> checkArgumentsToRFunctionCalls()
   {
      return bool("check_arguments_to_r_function_calls", false);
   }
   
   public PrefValue<Boolean> warnIfNoSuchVariableInScope()
   {
      return bool("warn_if_no_such_variable_in_scope", false);
   }
   
   public PrefValue<Boolean> warnIfVariableDefinedButNotUsed()
   {
      return bool("warn_if_variable_defined_but_not_used", false);
   }
   
   public PrefValue<Boolean> autoAppendNewline()
   {
      return bool("auto_append_newline", false);
   }
   
   public PrefValue<Boolean> stripTrailingWhitespace()
   {
      return bool("strip_trailing_whitespace", false);
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
   
   public PrefValue<Boolean> saveBeforeSourcing()
   {
      return bool("save_before_sourcing", true);
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
   
   public PrefValue<ExportPlotOptions> exportViewerOptions()
   {
      return object("export_viewer_options", ExportPlotOptions.createDefault());
   }
   
   public PrefValue<CompileNotebookPrefs> compileNotebookOptions()
   {
      return object("compile_notebook_options",
                    CompileNotebookPrefs.createDefault());
   }
   
   public PrefValue<CompileNotebookv2Prefs> compileNotebookv2Options()
   {
      return object("compile_notebookv2_options",
                    CompileNotebookv2Prefs.createDefault());
   }
   
   public PrefValue<Boolean> newProjGitInit()
   {
      return bool("new_proj_git_init", false);
   }
   
   public PrefValue<Boolean> newProjUsePackrat()
   {
      return bool("new_proj_use_packrat", false);
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
   
   public PrefValue<Boolean> checkForUpdates()
   {
      return bool("check_for_updates", true);
   }
   
   public PrefValue<Boolean> showInternalFunctionsInTraceback()
   {
      return bool("show_internal_functions", false);
   }
   
   public PrefValue<Integer> shinyViewerType()
   {
      return integer("shiny_viewer_type", ShinyViewerType.SHINY_VIEWER_WINDOW);
   }

   public PrefValue<String> documentAuthor()
   {
      return string("document_author", "");
   }
   
   public PrefValue<String> rmdPreferredTemplatePath()
   {
      return string("rmd_preferred_template_path", "");
   }
   
   public PrefValue<Integer> rmdViewerType()
   {
      return integer("rmd_viewer_type", RmdOutput.RMD_VIEWER_TYPE_WINDOW);
   }
   
   public PrefValue<Boolean> showPublishUi()
   {
      return bool("show_publish_ui", true);
   }

   public PrefValue<Boolean> enableRStudioConnect()
   {
      return bool("enable_rstudio_connect", false);
   }
   
   public PrefValue<RSConnectAccount> preferredPublishAccount()
   {
      return object("preferred_publish_account");
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
         
         // otherwise default to the internal viewer
         else
         {
            return PDF_PREVIEW_RSTUDIO;
         }
      }
      
      // web mode -- always default to internal viewer
      else
      {
         return PDF_PREVIEW_RSTUDIO;
      }
   }
}
