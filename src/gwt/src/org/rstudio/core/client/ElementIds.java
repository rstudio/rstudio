/*
 * ElementIds.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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
package org.rstudio.core.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.dom.DomUtils;

public class ElementIds
{
   public static void assignElementId(Element ele, String id)
   {
      String elementIdBase = ID_PREFIX + id;
      String elementId = elementIdBase;
      int counter = 0;
      
      // ensure uniqueness; for example, if multiple modal dialogs are displayed, make sure
      // the OK button instances, etc., are uniquely identified
      while (DomUtils.getElementById(elementId) != null)
      {
         elementId = elementIdBase + "_" + counter++;
      }
      ele.setId(elementId);
   }

   public static void assignElementId(Widget widget, String id)
   {
      assignElementId(widget.getElement(), id);
   }

   public static String getElementId(String id)
   {
      return ID_PREFIX + id;
   }

   public static String idSafeString(String text)
   {
      // replace all non-alphanumerics with underscores
      String id = text.replaceAll("[^a-zA-Z0-9]", "_");
      
      // collapse multiple underscores to a single underscore
      id = id.replaceAll("_+", "_");

      // clean up leading/trailing underscores
      id = id.replaceAll("^_+", "");
      id = id.replaceAll("_+$", "");
      
      // convert to lowercase and return
      return id.toLowerCase();
   }

   public static String idFromLabel(String label)
   {
      return ID_PREFIX + "label_" + idSafeString(label);
   }

   public final static String ID_PREFIX = "rstudio_";

   // global list of specific IDs we assign -- we keep this list centralized in this class as a
   // so that we can be sure an ID is not used elsewhere in the product
   public final static String CONSOLE_INPUT = "console_input";
   public final static String CONSOLE_OUTPUT = "console_output";
   public final static String DEPLOY_CONTENT = "deploy_content";
   public final static String FIND_REPLACE_BAR = "find_replace_bar";
   public final static String HELP_FRAME = "help_frame";
   public final static String LOADING_SPINNER = "loading_image";
   public final static String PLOT_IMAGE_FRAME = "plot_image_frame";
   public final static String POPUP_COMPLETIONS = "popup_completions";
   public final static String PREFERENCES_CONFIRM = "preferences_confirm";
   public final static String PUBLISH_CONNECT = "publish_connect";
   public final static String PUBLISH_DISCONNECT = "publish_disconnect";
   public final static String PUBLISH_ITEM = "publish_item";
   public final static String PUBLISH_RECONNECT = "publish_reconnect";
   public final static String PUBLISH_SHOW_DEPLOYMENTS = "show_deployments";
   public final static String RSC_SERVER_URL = "rsc_server_url";
   public final static String SHELL_WIDGET = "shell_widget";
   public final static String SOURCE_TEXT_EDITOR = "source_text_editor";
   public final static String XTERM_WIDGET = "xterm_widget";
   public final static String FILE_DIALOG_NAME_PROMPT = "file_dialog_name_prompt";
   public final static String WORKBENCH_PANEL = "workbench_panel";
   public final static String WORKBENCH_TAB = "workbench_tab";
   public final static String FILE_ACCEPT_BUTTON = "file_accept";
   public final static String FILE_CANCEL_BUTTON = "file_cancel";
   public final static String FILE_NEW_FOLDER_BUTTON = "file_new_folder";
   public final static String PREVIEW_BUTTON = "preview";
   public final static String CREATE_BUTTON = "create";
   public final static String DIALOG_YES_BUTTON = "dlg_yes";
   public final static String DIALOG_NO_BUTTON = "dlg_no";
   public final static String DIALOG_CANCEL_BUTTON = "dlg_cancel";
   public final static String DIALOG_OK_BUTTON = "dlg_ok";
   public final static String DIALOG_APPLY_BUTTON = "dlg_apply";
   public final static String DIALOG_RETRY_BUTTON = "dlg_retry";
   public final static String SELECT_ALL_BUTTON = "select_all";
   public final static String SELECT_NONE_BUTTON = "select_none";
   public final static String ABOUT_MANAGE_LICENSE_BUTTON = "about_manage_license";
   public final static String TEXT_SOURCE_BUTTON = "text_source";
   public final static String TEXT_SOURCE_BUTTON_DROPDOWN = "text_source_dropdown";
   public final static String EMPTY_DOC_BUTTON = "empty_doc";

   public final static String EDIT_EDITING_PREFS = "edit_editing_prefs";
   public final static String EDIT_DISPLAY_PREFS = "edit_display_prefs";
   public final static String EDIT_SAVING_PREFS = "edit_saving_prefs";
   public final static String EDIT_COMPLETION_PREFS = "editing_completion_prefs";
   public final static String EDIT_DIAGNOSTICS_PREFS = "editing_diagnostics_prefs";

   public final static String GENERAL_BASIC_PREFS = "general_basic_prefs";
   public final static String GENERAL_ADVANCED_PREFS = "general_advanced_prefs";

   public final static String PACKAGE_MANAGEMENT_PREFS = "package_management_prefs";
   public final static String PACKAGE_DEVELOPMENT_PREFS = "package_development_prefs";

   public final static String TERMINAL_GENERAL_PREFS = "terminal_general_prefs";
   public final static String TERMINAL_CLOSING_PREFS = "terminal_closing_prefs";

   // AskSecretDialog
   public final static String ASK_SECRET_TEXT = "ask_secret_text";
   public static String getAskSecretText() { return getElementId(ASK_SECRET_TEXT); }

   // FindInFilesDialog
   public final static String FIND_FILES_TEXT = "find_files_text";
   public static String getFindFilesText() { return getElementId(FIND_FILES_TEXT); }

   // ImportFileSettingsDialog
   public final static String IMPORT_FILE_NAME = "import_file_name";
   public static String getImportFileName() { return getElementId(IMPORT_FILE_NAME); }
   public final static String IMPORT_FILE_ENCODING = "import_file_encoding";
   public static String getImportFileEncoding() { return getElementId(IMPORT_FILE_ENCODING); }
   public final static String IMPORT_FILE_ROW_NAMES = "import_file_row_names";
   public static String getImportFileRowNames() { return getElementId(IMPORT_FILE_ROW_NAMES); }
   public final static String IMPORT_FILE_SEPARATOR = "import_file_separator";
   public static String getImportFileSeparator() { return getElementId(IMPORT_FILE_SEPARATOR); }
   public final static String IMPORT_FILE_DECIMAL = "import_file_decimal";
   public static String getImportFileDecimal() { return getElementId(IMPORT_FILE_DECIMAL); }
   public final static String IMPORT_FILE_QUOTE = "import_file_quote";
   public static String getImportFileQuote() { return getElementId(IMPORT_FILE_QUOTE); }
   public final static String IMPORT_FILE_COMMENT = "import_file_comment";
   public static String getImportFileComment() { return getElementId(IMPORT_FILE_COMMENT); }
   public final static String IMPORT_FILE_NA_STRINGS = "import_file_na_strings";
   public static String getImportFileNaStrings() { return getElementId(IMPORT_FILE_NA_STRINGS); }

   // NewRMarkdownDialog
   public final static String NEW_RMD_TITLE = "new_rmd_title";
   public static String getNewRmdTitle() { return getElementId(NEW_RMD_TITLE); }
   public final static String NEW_RMD_AUTHOR = "new_rmd_author";
   public static String getNewRmdAuthor() { return getElementId(NEW_RMD_AUTHOR); }

   // RmdTemplateChooser
   public final static String RMD_TEMPLATE_CHOOSER_NAME = "rmd_template_chooser_name";
   public static String getRmdTemplateChooserName() { return getElementId(RMD_TEMPLATE_CHOOSER_NAME); }

   // NewShinyWebApplication
   public final static String NEW_SHINY_APP_NAME = "new_shiny_app_name";
   public final static String NEW_SHINY_APP_SINGLE_FILE = "new_shiny_app_single_file";
   public final static String NEW_SHINY_APP_MULTI_FILE = "new_shiny_app_multi_file";

   // TextBoxWithButton
   public final static String TEXTBOXBUTTON_TEXT = "textboxbutton_text";
   public final static String TEXTBOXBUTTON_BUTTON = "textboxbutton_button";
   public final static String TEXTBOXBUTTON_HELP = "textboxbutton_help";

   // TerminalPane
   public final static String TERMINAL_DROPDOWN_MENUBUTTON = "terminal_dropdown_menubutton";

   // GlobalToolbar
   public final static String NEW_FILE_MENUBUTTON = "new_file_menubutton";
   public final static String OPEN_MRU_MENUBUTTON = "open_mru_menubutton";
   public final static String VCS_MENUBUTTON = "vcs_menubutton";
   public final static String PANELAYOUT_MENUBUTTON = "panelayout_menubutton";
   public final static String PROJECT_MENUBUTTON = "project_menubutton";
   public final static String PROJECT_MENUBUTTON_TOOLBAR_SUFFIX = "toolbar";
   public final static String PROJECT_MENUBUTTON_MENUBAR_SUFFIX = "menubar";

   // BuildPane
   public final static String BUILD_MORE_MENUBUTTON = "build_more_menubutton";
   public final static String BUILD_BOOKDOWN_MENUBUTTON = "build_bookdown_menubutton";

   // Modal Dialogs
   public final static String DIALOG_GLOBAL_PREFS = "dialog_global_prefs";
}
