/*
 * ClientEvent.java
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
package org.rstudio.studio.client.server.remote;

import com.google.gwt.core.client.JavaScriptObject;

class ClientEvent extends JavaScriptObject
{   
   public static final String Busy = "busy";
   public static final String ConsolePrompt = "console_prompt";
   public static final String ConsoleOutput = "console_output" ;
   public static final String ConsoleError = "console_error";
   public static final String ConsoleWritePrompt = "console_write_prompt";
   public static final String ConsoleWriteInput = "console_write_input";
   public static final String ShowErrorMessage = "show_error_message";
   public static final String ShowHelp = "show_help" ;
   public static final String BrowseUrl = "browse_url";
   public static final String WorkspaceRefresh = "workspace_refresh";
   public static final String WorkspaceAssign = "workspace_assign";
   public static final String WorkspaceRemove = "workspace_remove";
   public static final String ShowEditor = "show_editor";
   public static final String ChooseFile = "choose_file";
   public static final String AbendWarning = "abend_warning";
   public static final String Quit = "quit";
   public static final String Suicide = "suicide";
   public static final String FileChanged = "file_changed";
   public static final String WorkingDirChanged = "working_dir_changed";
   public static final String PlotsStateChanged = "plots_state_changed";
   public static final String ViewData = "view_data";
   public static final String PackageStatusChanged = "package_status_changed";
   public static final String InstalledPackagesChanged = "installed_packages_changed";
   public static final String Locator = "locator";
   public static final String ConsoleResetHistory = "console_reset_history";
   public static final String SessionSerialization = "session_serialization";
   public static final String HistoryEntriesAdded = "history_entries_added";
   public static final String QuotaStatus = "quota_status";
   public static final String FileEdit = "file_edit";
   public static final String ShowContent = "show_content";
   public static final String ShowData = "show_data";
   public static final String AsyncCompletion = "async_completion";
   public static final String SaveActionChanged = "save_action_changed";
   public static final String ShowWarningBar = "show_warning_bar";
   public static final String OpenProjectError = "open_project_error";
   public static final String VcsRefresh = "vcs_refresh";
   public static final String AskPass = "ask_pass";
   public static final String ConsoleProcessOutput = "console_process_output";
   public static final String ConsoleProcessExit = "console_process_exit";
   public static final String ListChanged = "list_changed";
   public static final String UiPrefsChanged = "ui_prefs_changed";
   public static final String HandleUnsavedChanges = "handle_unsaved_changes";
   public static final String PosixShellOutput = "posix_shell_output";
   public static final String PosixShellExit = "posix_shell_exit";
   public static final String ConsoleProcessPrompt = "console_process_prompt";
   public static final String ConsoleProcessCreated = "console_process_created";
   public static final String HTMLPreviewStartedEvent = "html_preview_started_event";
   public static final String HTMLPreviewOutputEvent = "html_preview_output_event";
   public static final String HTMLPreviewCompletedEvent = "html_preview_completed_event";
   public static final String CompilePdfStartedEvent = "compile_pdf_started_event";
   public static final String CompilePdfOutputEvent = "compile_pdf_output_event";
   public static final String CompilePdfErrorsEvent = "compile_pdf_errors_event";
   public static final String CompilePdfCompletedEvent = "compile_pdf_completed_event";
   public static final String FindResult = "find_result";
   public static final String FindOperationEnded = "find_operation_ended";

   protected ClientEvent()
   {
   }
   
   public final native int getId() /*-{
      return this.id;
   }-*/;
   
   public final native String getType() /*-{
      return this.type;
   }-*/;
   
   public final native <T> T getData() /*-{
      return this.data;
   }-*/;
}