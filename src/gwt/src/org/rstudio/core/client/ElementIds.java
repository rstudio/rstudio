/*
 * ElementIds.java
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
package org.rstudio.core.client;

import com.google.gwt.aria.client.Id;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.regex.Pattern;

public class ElementIds
{
   /**
    * Return a unique ID based on examination of existing elements. Must be assigned
    * immediately to an element in the DOM to ensure uniqueness.
    * @param baseId
    * @return
    */
   public static String getUniqueElementId(String baseId)
   {
      String elementIdBase = getElementId(baseId);
      String elementId = elementIdBase;
      int counter = 0;

      // ensure uniqueness; for example, if multiple modal dialogs are displayed, make sure
      // the OK button instances, etc., are uniquely identified
      while (DomUtils.getElementById(elementId) != null)
      {
         elementId = elementIdBase + "_" + counter++;
      }
     return elementId;
   }

   public static void assignElementId(Element ele, String id)
   {
      ele.setId(getUniqueElementId(id));
   }

   public static void assignElementId(Widget widget, String id)
   {
      assignElementId(widget.getElement(), id);
   }

   public static String getElementId(String id)
   {
      return ID_PREFIX + id;
   }

   public static Id getAriaElementId(String id)
   {
      return Id.of(getElementId(id));
   }

   public static boolean isInstanceOf(Widget widget, String baseId)
   {
      return isInstanceOf(widget.getElement(), baseId);
   }

   public static boolean isInstanceOf(Element ele, String baseId)
   {
      String actualId = ele.getId();
      String testId = ElementIds.getElementId(baseId);
      if (actualId == testId)
         return true;

      // does ID match disambiguation pattern?
      if (RE_NUMBERED_ELEMENT_ID.test(actualId))
      {
         String trimmedId = actualId.substring(0, actualId.lastIndexOf('_'));
         return trimmedId == testId;
      }
      return false;
   }

   public static String idSafeString(String text)
   {
      // If text contains "C++" it will generate the same label as one containing
      // plain "C", so substitute "CPP" to avoid duplicate IDs.
      if (text.contains("C++"))
      {
         text = text.replaceFirst("C\\+\\+", "CPP");
      }

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

   private static final Pattern RE_NUMBERED_ELEMENT_ID = Pattern.create("^[a-zA-Z0-9_]+_\\d+$");

   public final static String ID_PREFIX = "rstudio_";

   // global list of specific IDs we assign -- we keep this list centralized in this class as a
   // so that we can be sure an ID is not used elsewhere in the product
   public final static String RSTUDIO_LOGO = "rstudio_logo";
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
   public final static String GENERAL_GRAPHICS_PREFS = "general_graphics_prefs";
   public final static String GENERAL_ADVANCED_PREFS = "general_advanced_prefs";

   public final static String RMARKDOWN_BASIC_PREFS = "rmarkdown_basic_prefs";
   public final static String RMARKDOWN_ADVANCED_PREFS = "markdown_advanced_prefs";
   public final static String RMARKDOWN_VISUAL_MODE_PREFS = "markdown_visual_mode_prefs";
   public final static String RMARKDOWN_CITATIONS_PREFS = "markdown_citations_prefs";

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
   public final static String FIND_FILES_PATTERN_EXAMPLE = "find_files_pattern_example";

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
   public final static String NEW_RMD_TEMPLATE_LABEL = "new_rmd_template_label";
   public static String getNewRmdTemplateLabel() { return getElementId(NEW_RMD_TEMPLATE_LABEL); }
   public final static String NEW_RMD_TEMPLATE = "new_rmd_template";
   public static String getNewRmdTemplate() { return getElementId(NEW_RMD_TEMPLATE); }

   // RmdTemplateChooser
   public final static String RMD_TEMPLATE_CHOOSER_NAME = "rmd_template_chooser_name";
   public static String getRmdTemplateChooserName() { return getElementId(RMD_TEMPLATE_CHOOSER_NAME); }

   // NewShinyWebApplication
   public final static String NEW_SHINY_APP_NAME = "new_shiny_app_name";
   public final static String NEW_SHINY_APP_SINGLE_FILE = "new_shiny_app_single_file";
   public final static String NEW_SHINY_APP_MULTI_FILE = "new_shiny_app_multi_file";

   // TextBoxWithButton and subclasses -- prefixes for button/text/help, combined with suffixes
   public final static String TBB_TEXT = "tbb_text";
   public final static String TBB_BUTTON = "tbb_button";
   public final static String TBB_HELP = "tbb_help";

   // TextBoxWithButton and subclasses -- unique suffix added to text field, button, and help link;
   // only has to be unique within this enum
   public enum TextBoxButtonId
   {
      BUILD_SCRIPT("build_Script"),
      CA_BUNDLE("ca_bundle"),
      DEFAULT_WORKING_DIR("default_working_dir"),
      ZOTERO_DATA_DIRECTORY("zotero_data_directory"),
      EXISTING_PROJECT_DIR("existing_project_dir"),
      FIND_IN("find_in"),
      GIT("git"),
      JOB_SCRIPT("job_script"),
      JOB_WORKING_DIR("job_working_dir"),
      ODBC_PATH("odbc_path"),
      PACKAGE_ARCHIVE("package_archive"),
      PDF_ROOT("pdf_root"),
      PLUMBER_DIR("plumber_dir"),
      PRIMARY_CRAN("primary_cran"),
      PRO_JOB_DIR("pro_job_dir"),
      PRO_JOB_SCRIPT("pro_job_script"),
      PRO_NEW_SESSION_DIR("pro_new_session_dir"),
      PROJECT_PARENT("project_parent"),
      PROJECT_REPO_DIR("project_repo_dir"),
      PROJECT_ROOT("project_root"),
      PROJECT_TEMPLATE("project_template"),
      PROJECT_TEXT_ENCODING("project_text_encoding"),
      RMD_DIR("rmd_dir"),
      RMD_OPTION("rmd_option"),
      RMD_TEMPLATE_DIR("rmd_template_dir"),
      R_VERSION("r_version"),
      SHINY_DIR("shiny_dir"),
      SVN("svn"),
      TERMINAL("terminal"),
      TEXT_ENCODING("text_encoding"),
      UPLOAD_TARGET("upload_target"),
      VCS_IGNORE("vcs_ignore"),
      VCS_TERMINAL("vcs_terminal"),
      CHOOSE_IMAGE("choose_image"),
      PYTHON_PATH("python_path");

      TextBoxButtonId(String value)
      {
         value_ = value;
      }

      @Override
      public String toString()
      {
         return value_;
      }

      private final String value_;
   }

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

   // JobLauncherDialog
   public final static String JOB_LAUNCHER_ENVIRONMENT = "job_launcher_environment";
   public static String getJobLauncherEnvironment() { return getElementId(JOB_LAUNCHER_ENVIRONMENT); }

   // JobLauncherDialogPro
   public final static String JOB_LAUNCHER_PRO_OPTIONS = "job_launcher_pro_options";
   public final static String JOB_LAUNCHER_PRO_ENVIRONMENT = "job_launcher_pro_environment";

   // OpenSharedProjectDialog (Pro)
   public final static String SHARED_PROJ_MINE = "shared_proj_mine";
   public final static String SHARED_PROJ_SHARED = "shared_proj_shared";

   // RmdTemplateOptionsWidget
   public final static String RMD_TEMPLATE_OPTIONS_OUTPUT_FORMAT = "rmd_template_options_output_format";
   public static String getRmdTemplateOptionsOutputFormat() { return getElementId(RMD_TEMPLATE_OPTIONS_OUTPUT_FORMAT); }

   // Modal Dialogs
   public final static String DIALOG_GLOBAL_PREFS = "dialog_global_prefs";

   // DataImport
   public final static String DATA_IMPORT_UI_OPTIONS = "data_import_ui_options";
   public final static String DATA_IMPORT_FILE_URL = "data_import_file_url";
   public static String getDataImportFileUrl() { return getElementId(DATA_IMPORT_FILE_URL); }
   public final static String DATA_IMPORT_OPTIONS_FILECHOOSER = "data_import_options_filechooser";
   public static String getDataImportOptionsFilechooser() { return getElementId(DATA_IMPORT_OPTIONS_FILECHOOSER); }
   public final static String DATA_IMPORT_CODE_PREVIEW = "data_import_code_preview";
   public static String getDataImportCodePreview() { return getElementId(DATA_IMPORT_CODE_PREVIEW); }
   public final static String DATA_IMPORT_OPTIONS_NAME = "data_import_options_name";
   public static String getDataImportOptionsName() { return getElementId(DATA_IMPORT_OPTIONS_NAME); }
   public final static String DATA_IMPORT_OPTIONS_SKIP = "data_import_options_skip";
   public static String getDataImportOptionsSkip() { return getElementId(DATA_IMPORT_OPTIONS_SKIP); }
   public final static String DATA_IMPORT_OPTIONS_MAXROWS = "data_import_options_maxrows";
   public static String getDataImportOptionsMaxrows() { return getElementId(DATA_IMPORT_OPTIONS_MAXROWS); }
   public final static String DATA_IMPORT_OPTIONS_DELIMITER = "data_import_options_delimiter";
   public static String getDataImportOptionsDelimiter() { return getElementId(DATA_IMPORT_OPTIONS_DELIMITER); }
   public final static String DATA_IMPORT_OPTIONS_QUOTES = "data_import_options_quotes";
   public static String getDataImportOptionsQuotes() { return getElementId(DATA_IMPORT_OPTIONS_QUOTES); }
   public final static String DATA_IMPORT_OPTIONS_ESCAPE = "data_import_options_escape";
   public static String getDataImportOptionsEscape() { return getElementId(DATA_IMPORT_OPTIONS_ESCAPE); }
   public final static String DATA_IMPORT_OPTIONS_COMMENT = "data_import_options_comment";
   public static String getDataImportOptionsComment() { return getElementId(DATA_IMPORT_OPTIONS_COMMENT); }
   public final static String DATA_IMPORT_OPTIONS_NA = "data_import_options_na";
   public static String getDataImportOptionsNa() { return getElementId(DATA_IMPORT_OPTIONS_NA); }
   public final static String DATA_IMPORT_OPTIONS_SHEET = "data_import_options_sheet";
   public static String getDataImportOptionsSheet() { return getElementId(DATA_IMPORT_OPTIONS_SHEET); }
   public final static String DATA_IMPORT_OPTIONS_RANGE = "data_import_options_range";
   public static String getDataImportOptionsRange() { return getElementId(DATA_IMPORT_OPTIONS_RANGE); }
   public final static String DATA_IMPORT_OPTIONS_FORMAT = "data_import_options_format";
   public static String getDataImportOptionsFormat() { return getElementId(DATA_IMPORT_OPTIONS_FORMAT); }

   // DataImportOptionsUiCsvLocale
   public final static String DATA_IMPORT_CSV_DATENAME = "data_import_csv_datename";
   public static String getDataImportCsvDatename() { return getElementId(DATA_IMPORT_CSV_DATENAME); }
   public final static String DATA_IMPORT_CSV_ENCODING = "data_import_csv_encoding";
   public static String getDataImportCsvEncoding() { return getElementId(DATA_IMPORT_CSV_ENCODING); }
   public final static String DATA_IMPORT_CSV_DATE_FORMAT = "data_import_csv_date_format";
   public static String getDataImportCsvDateFormat() { return getElementId(DATA_IMPORT_CSV_DATE_FORMAT); }
   public final static String DATA_IMPORT_CSV_TIME_FORMAT = "data_import_csv_time_format";
   public static String getDataImportCsvTimeFormat() { return getElementId(DATA_IMPORT_CSV_TIME_FORMAT); }
   public final static String DATA_IMPORT_CSV_DECIMAL_MARK = "data_import_csv_decimal_mark";
   public static String getDataImportCsvDecimalMark() { return getElementId(DATA_IMPORT_CSV_DECIMAL_MARK); }
   public final static String DATA_IMPORT_CSV_GROUPING_MARK = "data_import_csv_grouping_mark";
   public static String getDataImportCsvGroupingMark() { return getElementId(DATA_IMPORT_CSV_GROUPING_MARK); }
   public final static String DATA_IMPORT_CSV_TZ = "data_import_csv_tz";
   public static String getDataImportCsvTz() { return getElementId(DATA_IMPORT_CSV_TZ); }

   // AboutDialogContents
   public final static String ABOUT_LICENSE_INFO = "about_license_info";
   public static String getAboutLicenseInfo() { return getElementId(ABOUT_LICENSE_INFO); }

   // TextEditingTargetWidget
   public final static String CB_SOURCE_ON_SAVE = "cb_source_on_save";
   public static String getCbSourceOnSave() { return getElementId(CB_SOURCE_ON_SAVE); }
   public final static String TOGGLE_DOC_OUTLINE_BUTTON = "toggle_doc_outline_button";
   public static String getToggleDocOutlineButton() { return getElementId(TOGGLE_DOC_OUTLINE_BUTTON); }

   // AddinsToolbarButton
   public final static String ADDINS_TOOLBAR_BUTTON = "addins_toolbar_button";
   public static String getAddinsToolbarButton() { return getElementId(ADDINS_TOOLBAR_BUTTON); }

   // CodeSearchWidget
   public final static String CODE_SEARCH_WIDGET = "code_search_widget";
   public static String getCodeSearchWidget() { return getElementId(CODE_SEARCH_WIDGET); }

   // EnvironmentPane
   public final static String MB_IMPORT_DATASET = "mb_import_dataset";
   public static String getMbImportDataset() { return getElementId(MB_IMPORT_DATASET); }
   public final static String MB_ENVIRONMENT_LIST = "mb_environment_list";
   public static String getMbEnvironmentList() { return getElementId(MB_ENVIRONMENT_LIST); }
   public final static String MB_OBJECT_LIST_VIEW = "mb_object_list_view";
   public static String getMbObjectListView() { return getElementId(MB_OBJECT_LIST_VIEW); }
   public final static String SW_ENVIRONMENT = "sw_environment";
   public static String getSwEnvironment() { return getElementId(SW_ENVIRONMENT); }

   // HistoryPane
   public final static String SW_HISTORY = "sw_history";
   public static String getSwHistory() { return getElementId(SW_HISTORY); }

   // GitPane
   public final static String MB_GIT_MORE = "mb_git_more";
   public static String getMbGitMore() { return getElementId(MB_GIT_MORE); }
   public final static String TB_GIT_REFRESH = "tb_git_refresh";
   public static String getTbGitRefresh() { return getElementId(TB_GIT_REFRESH); }

   // FileCommandToolbar
   public final static String MB_FILES_MORE = "mb_files_more";
   public static String getMbFilesMore() { return getElementId(MB_FILES_MORE); }

   // PlotsToolbar
   public final static String MB_PLOTS_EXPORT = "mb_plots_export";
   public static String getMbPlotsExport() { return getElementId(MB_PLOTS_EXPORT); }

   // PackagesPane
   public final static String SW_PACKAGES = "sw_packages";
   public static String getSwPackages() { return getElementId(SW_PACKAGES); }

   // SpellingDialog
   public final static String SPELLING_NOT_IN_DICT= "spelling_not_in_dict";
   public static String getSpellingNotInDict() { return getElementId(SPELLING_NOT_IN_DICT); }
   public final static String SPELLING_CHANGE_TO= "spelling_change_to";
   public static String getSpellingChangeTo() { return getElementId(SPELLING_CHANGE_TO); }

   // HelpPane
   public final static String SW_HELP_FIND_IN_TOPIC = "sw_help_find_in_topic";
   public static String getSwHelpFindInTopic() { return getElementId(SW_HELP_FIND_IN_TOPIC); }

   // HelpSearchWidget
   public final static String SW_HELP = "sw_help";
   public static String getSwHelp() { return getElementId(SW_HELP); }

   // NewRdDialog
   public final static String NEW_RD_NAME = "new_rd_name";
   public static String getNewRdName() { return getElementId(NEW_RD_NAME); }
   public final static String NEW_RD_TEMPLATE = "new_rd_template";
   public static String getNewRdTemplate() { return getElementId(NEW_RD_TEMPLATE); }

   // SvnResolveDialog
   public final static String SVN_RESOLVE_GROUP = "svn_resolve_group";
   public final static String SVN_RESOLVE_MINE = "svn_resolve_mine";
   public final static String SVN_RESOLVE_MINE_DESC = "svn_resolve_mine_desc";
   public final static String SVN_RESOLVE_MINE_CONFLICT = "svn_resolve_mine_conflict";
   public final static String SVN_RESOLVE_MINE_CONFLICT_DESC = "svn_resolve_mine_conflict_desc";
   public final static String SVN_RESOLVE_THEIRS_CONFLICT = "svn_resolve_theirs_conflict";
   public final static String SVN_RESOLVE_THEIRS_CONFLICT_DESC = "svn_resolve_theirs_conflict_desc";
   public final static String SVN_RESOLVE_MINE_ALL = "svn_resolve_mine_all";
   public final static String SVN_RESOLVE_MINE_ALL_DESC = "svn_resolve_mine_all_desc";
   public final static String SVN_RESOLVE_THEIRS_ALL = "svn_resolve_theirs_all";
   public final static String SVN_RESOLVE_THEIRS_ALL_DESC = "svn_resolve_theirs_all_desc";

   // TutorialPane
   public final static String TUTORIAL_FRAME = "tutorial_frame";

   // ShowPublicKeyDialog
   public final static String PUBLIC_KEY_TEXT = "public_key_text";
   public final static String PUBLIC_KEY_LABEL = "public_key_label";

   // JobQuitControls
   public final static String JOB_QUIT_LISTBOX = "job_quit_listbox";
   public static String getJobQuitListbox() { return getElementId(JOB_QUIT_LISTBOX); }

   // RSConnect
   public final static String RSC_SERVER_URL = "rsc_server_url";
   public static String getRscServerUrl() { return getElementId(RSC_SERVER_URL); }
   public final static String RSC_ACCOUNT_LIST_LABEL = "rsc_account_list_label";
   public static String getRscAccountListLabel() { return getElementId(RSC_ACCOUNT_LIST_LABEL); }
   public final static String RSC_ACCOUNT_LIST = "rsc_account_list";
   public static String getRscAccountList() { return getElementId(RSC_ACCOUNT_LIST); }
   public final static String RSC_FILES_LIST_LABEL = "rsc_files_list_label";

   // WindowFrameButton (combined with unique suffix for each quadrant
   public final static String FRAME_MIN_BTN = "frame_min_btn";
   public final static String FRAME_MAX_BTN = "frame_max_btn";
   public final static String MIN_FRAME_MIN_BTN = "min_frame_min_btn";
   public final static String MIN_FRAME_MAX_BTN = "min_frame_max_btn";

   // Visual Markdown Editing dialogs
   public final static String VISUAL_MD_RAW_FORMAT_SELECT = "visual_md_raw_format_select";
   public static String getVisualMdRawFormatSelect() { return getElementId(VISUAL_MD_RAW_FORMAT_SELECT); }
   public final static String VISUAL_MD_RAW_FORMAT_CONTENT = "visual_md_raw_format_content";
   public static String getVisualMdRawContent() { return getElementId(VISUAL_MD_RAW_FORMAT_CONTENT); }
   public final static String VISUAL_MD_RAW_FORMAT_REMOVE_BUTTON = "visual_md_raw_format_remove_button";
   public final static String VISUAL_MD_INSERT_TABLE_ROWS = "visual_md_insert_table_rows";
   public static String getVisualMdInsertTableRows() { return getElementId(VISUAL_MD_INSERT_TABLE_ROWS); }
   public final static String VISUAL_MD_INSERT_TABLE_COLUMNS = "visual_md_insert_table_columns";
   public static String getVisualMdInsertTableColumns() { return getElementId(VISUAL_MD_INSERT_TABLE_COLUMNS); }
   public final static String VISUAL_MD_INSERT_TABLE_CAPTION = "visual_md_insert_table_caption";
   public static String getVisualMdInsertTableCaption() { return getElementId(VISUAL_MD_INSERT_TABLE_CAPTION); }
   public final static String VISUAL_MD_INSERT_TABLE_HEADER = "visual_md_insert_table_heaeder";
   public final static String VISUAL_MD_ATTR_REMOVE_BUTTON = "visual_md_attr_remove_button";
   public final static String VISUAL_MD_ATTR_ID_LABEL1 = "visual_md_attr_id_label1";
   public static String getVisualMdAttrIdLabel1() { return getElementId(VISUAL_MD_ATTR_ID_LABEL1); }
   public final static String VISUAL_MD_ATTR_ID_LABEL2 = "visual_md_attr_id_label2";
   public static String getVisualMdAttrIdLabel2() { return getElementId(VISUAL_MD_ATTR_ID_LABEL2); }
   public final static String VISUAL_MD_ATTR_ID_GENERATE = "visual_md_attr_id_generate";
   public final static String VISUAL_MD_ATTR_ID = "visual_md_attr_id";
   public static String getVisualMdAttrId() { return getElementId(VISUAL_MD_ATTR_ID); }
   public final static String VISUAL_MD_ATTR_CLASSES_LABEL1 = "visual_md_attr_classes_label1";
   public static String getVisualMdAttrClassesLabel1() { return getElementId(VISUAL_MD_ATTR_CLASSES_LABEL1); }
   public final static String VISUAL_MD_ATTR_CLASSES_LABEL2 = "visual_md_attr_classes_label2";
   public static String getVisualMdAttrClassesLabel2() { return getElementId(VISUAL_MD_ATTR_CLASSES_LABEL2); }
   public final static String VISUAL_MD_ATTR_CLASSES = "visual_md_attr_classes";
   public static String getVisualMdAttrClasses() { return getElementId(VISUAL_MD_ATTR_CLASSES); }
   public final static String VISUAL_MD_ATTR_STYLE_LABEL1 = "visual_md_attr_style_label1";
   public static String getVisualMdAttrStyleLabel1() { return getElementId(VISUAL_MD_ATTR_STYLE_LABEL1); }
   public final static String VISUAL_MD_ATTR_STYLE_LABEL2 = "visual_md_attr_style_label2";
   public static String getVisualMdAttrStyleLabel2() { return getElementId(VISUAL_MD_ATTR_STYLE_LABEL2); }
   public final static String VISUAL_MD_ATTR_STYLE = "visual_md_attr_style";
   public static String getVisualMdAttrStyle() { return getElementId(VISUAL_MD_ATTR_STYLE); }
   public final static String VISUAL_MD_ATTR_KEYVALUE_LABEL1 = "visual_md_attr_keyvalue_label1";
   public static String getVisualMdAttrKeyValueLabel1() { return getElementId(VISUAL_MD_ATTR_KEYVALUE_LABEL1); }
   public final static String VISUAL_MD_ATTR_KEYVALUE_LABEL2 = "visual_md_attr_keyvalue_label2";
   public static String getVisualMdAttrKeyValueLabel2() { return getElementId(VISUAL_MD_ATTR_KEYVALUE_LABEL2); }
   public final static String VISUAL_MD_ATTR_KEYVALUE = "visual_md_attr_keyvalue";
   public static String getVisualMdAttrKeyValue() { return getElementId(VISUAL_MD_ATTR_KEYVALUE); }
   public final static String VISUAL_MD_CITATION_ID = "visual_md_citation_id";
   public static String getVisualMdCitationId() { return getElementId(VISUAL_MD_CITATION_ID); }
   public final static String VISUAL_MD_CITATION_LOCATOR = "visual_md_citation_locator";
   public static String getVisualMdCitationLocator() { return getElementId(VISUAL_MD_CITATION_LOCATOR); }
   public final static String VISUAL_MD_LIST_TYPE = "visual_md_list_type";
   public static String getVisualMdListType() { return getElementId(VISUAL_MD_LIST_TYPE); }
   public final static String VISUAL_MD_LIST_ORDER = "visual_md_list_order";
   public static String getVisualMdListOrder() { return getElementId(VISUAL_MD_LIST_ORDER); }
   public final static String VISUAL_MD_LIST_NUMBER_STYLE = "visual_md_list_number_style";
   public static String getVisualMdListNumberStyle() { return getElementId(VISUAL_MD_LIST_NUMBER_STYLE); }
   public final static String VISUAL_MD_LIST_NUMBER_DELIM = "visual_md_list_number_delim";
   public static String getVisualMdListNumberDelim() { return getElementId(VISUAL_MD_LIST_NUMBER_DELIM); }
   public final static String VISUAL_MD_LIST_NUMBER_DELIM_NOTE = "visual_md_list_number_delim_note";
   public static String getVisualMdListNumberDelimNote() { return getElementId(VISUAL_MD_LIST_NUMBER_DELIM_NOTE); }
   public final static String VISUAL_MD_LIST_INSERT_CITE_ID = "visual_md_insert_cite_id";
   public static String getVisualMdInsertCiteId() { return getElementId(VISUAL_MD_LIST_INSERT_CITE_ID); }
   public final static String VISUAL_MD_LIST_INSERT_CITE_PREVIEW = "visual_md_insert_cite_preview";
   public static String getVisualMdInsertCitePreview() { return getElementId(VISUAL_MD_LIST_INSERT_CITE_PREVIEW); }
   public final static String VISUAL_MD_LIST_INSERT_CITE_BIB = "visual_md_insert_cite_bib";
   public static String getVisualMdInsertCiteBib() { return getElementId(VISUAL_MD_LIST_INSERT_CITE_BIB); }
   public final static String VISUAL_MD_LIST_INSERT_CITE_CREATE_BIB = "visual_md_insert_cite_create_bib";
   public static String getVisualMdInsertCiteCreateBib() { return getElementId(VISUAL_MD_LIST_INSERT_CITE_CREATE_BIB); }
   public final static String VISUAL_MD_LIST_INSERT_CITE_CREATE_BIB_TYPE = "visual_md_insert_cite_create_bib_type";
   public static String getVisualMdInsertCiteCreateBibType() { return getElementId(VISUAL_MD_LIST_INSERT_CITE_CREATE_BIB_TYPE); }


   public final static String VISUAL_MD_LIST_TIGHT = "visual_md_ordered_list_tight";
   public final static String VISUAL_MD_IMAGE_TAB_IMAGE = "visual_md_image_tab_image";
   public final static String VISUAL_MD_IMAGE_WIDTH = "visual_md_image_width";
   public final static String VISUAL_MD_IMAGE_HEIGHT = "visual_md_image_height";
   public final static String VISUAL_MD_IMAGE_UNITS = "visual_md_image_units";
   public final static String VISUAL_MD_IMAGE_LOCK_RATIO = "visual_md_image_lock_ratio";
   public final static String VISUAL_MD_IMAGE_TITLE = "visual_md_image_title";
   public final static String VISUAL_MD_IMAGE_ALT = "visual_md_image_alt";
   public final static String VISUAL_MD_IMAGE_LINK_TO = "visual_md_image_link_to";
   public final static String VISUAL_MD_IMAGE_TAB_ATTRIBUTES = "visual_md_image_tab_attributes";
   public final static String VISUAL_MD_LINK_REMOVE_LINK_BUTTON = "visual_md_link_remove_link_button";
   public final static String VISUAL_MD_LINK_TAB_LINK = "visual_md_link_tab_link";
   public final static String VISUAL_MD_LINK_TYPE = "visual_md_link_type";
   public final static String VISUAL_MD_LINK_HREF = "visual_md_link_href";
   public final static String VISUAL_MD_LINK_SELECT_HEADING = "visual_md_link_select_heading";
   public final static String VISUAL_MD_LINK_SELECT_ID = "visual_md_link_select_id";
   public final static String VISUAL_MD_LINK_TEXT = "visual_md_link_text";
   public final static String VISUAL_MD_LINK_TITLE = "visual_md_link_title";
   public final static String VISUAL_MD_LINK_TAB_ATTRIBUTES = "visual_md_link_tab_attributes";
   public final static String VISUAL_MD_CODE_BLOCK_TAB_LANGUAGE = "visual_md_code_block_tab_language";
   public final static String VISUAL_MD_CODE_BLOCK_TAB_ATTRIBUTES = "visual_md_code_block_tab_attributes";
   public final static String VISUAL_MD_CODE_BLOCK_LANG_LABEL1 = "visual_md_code_block_lang_label1";
   public final static String VISUAL_MD_CODE_BLOCK_LANG_LABEL2 = "visual_md_code_block_lang_label2";
   public final static String VISUAL_MD_CODE_BLOCK_LANG = "visual_md_code_block_tab_lang";


   // ProgressDialog
   public final static String PROGRESS_TITLE_LABEL = "progress_title_label";

   // AccessibilityPreferencesPane
   public final static String A11Y_GENERAL_PREFS = "a11y_general_prefs";
   public final static String A11Y_ANNOUNCEMENTS_PREFS = "a11y_announcements_prefs";

   // SatelliteWindow
   public final static String SATELLITE_PANEL = "satellite_panel";

   // CommandPalette
   public final static String COMMAND_PALETTE_LIST = "command_palette_list";
   public final static String COMMAND_PALETTE_SEARCH = "command_palette_search";
   public final static String COMMAND_ENTRY_PREFIX = "command_entry_";
}
