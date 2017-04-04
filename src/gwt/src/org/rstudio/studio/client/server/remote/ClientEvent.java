/*
 * ClientEvent.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
   public static final String PackageStateChanged = "package_state_changed";
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
   public static final String SynctexEditFile = "synctex_edit_file";
   public static final String FindResult = "find_result";
   public static final String FindOperationEnded = "find_operation_ended";
   public static final String RPubsUploadStatus = "rpubs_upload_status";
   public static final String BuildStarted = "build_started";
   public static final String BuildOutput = "build_output";
   public static final String BuildCompleted = "build_completed";
   public static final String BuildErrors = "build_errors";
   public static final String DirectoryNavigate = "directory_navigate";
   public static final String DeferredInitCompleted = "deferred_init_completed";
   public static final String PlotsZoomSizeChanged = "plots_zoom_size_changed";
   public static final String SourceCppStarted = "source_cpp_started";
   public static final String SourceCppCompleted = "source_cpp_completed";
   public static final String LoadedPackageUpdates = "loaded_package_updates";
   public static final String ActivatePane = "activate_pane";
   public static final String ShowPresentationPane = "show_presentation_pane";
   public static final String EnvironmentRefresh = "environment_refresh";
   public static final String ContextDepthChanged = "context_depth_changed";
   public static final String EnvironmentAssigned = "environment_assigned";
   public static final String EnvironmentRemoved = "environment_removed";
   public static final String BrowserLineChanged = "browser_line_changed";
   public static final String PackageLoaded = "package_loaded";
   public static final String PackageUnloaded = "package_unloaded";
   public static final String PresentationPaneRequestCompleted = "presentation_pane_request_completed";
   public static final String UnhandledError = "unhandled_error";
   public static final String ErrorHandlerChanged = "error_handler_changed";
   public static final String ViewerNavigate = "viewer_navigate";
   public static final String UpdateCheck = "update_check";
   public static final String SourceExtendedTypeDetected = "source_extended_type_detected";
   public static final String ShinyViewer = "shiny_viewer";
   public static final String DebugSourceCompleted = "debug_source_completed";
   public static final String RmdRenderStarted = "rmd_render_started";
   public static final String RmdRenderOutput = "rmd_render_output";
   public static final String RmdRenderCompleted = "rmd_render_completed";
   public static final String RmdTemplateDiscovered = "rmd_template_discovered";
   public static final String RmdTemplateDiscoveryCompleted = "rmd_template_discovery_completed";
   public static final String RmdShinyDocStarted = "rmd_shiny_doc_started";
   public static final String RSConnectDeploymentOutput = "rsconnect_deployment_output";
   public static final String RSConnectDeploymentCompleted = "rsconnect_deployment_completed";
   public static final String RSConnectDeploymentFailed = "rsconnect_deployment_failed";
   public static final String UserPrompt = "user_prompt";
   public static final String InstallRtools = "install_r_tools";
   public static final String InstallShiny = "install_shiny";
   public static final String SuspendAndRestart = "suspend_and_restart";
   public static final String PackratRestoreNeeded = "packrat_restore_needed";
   public static final String DataViewChanged = "data_view_changed";
   public static final String ViewFunction = "view_function";
   public static final String MarkersChanged = "markers_changed";
   public static final String EnableRStudioConnect = "enable_rstudio_connect";
   public static final String UpdateGutterMarkers = "update_gutter_markers";
   public static final String SnippetsChanged = "snippets_changed";
   public static final String JumpToFunction = "jump_to_function";
   public static final String CollabEditStarted = "collab_edit_started";
   public static final String SessionCountChanged = "session_count_changed";
   public static final String CollabEditEnded = "collab_edit_ended";
   public static final String ProjectUsersChanged = "project_users_changed";
   public static final String RVersionsChanged = "r_versions_changed";
   public static final String ShinyGadgetDialog = "shiny_gadget_dialog";
   public static final String RmdParamsReady = "rmd_params_ready";
   public static final String RegisterUserCommand = "register_user_command";
   public static final String SendToConsole = "send_to_console";
   public static final String UserFollowStarted = "user_follow_started";
   public static final String UserFollowEnded = "user_follow_ended";
   public static final String ProjectAccessRevoked = "project_access_revoked";
   public static final String CollabEditSaved = "collab_edit_saved";
   public static final String AddinRegistryUpdated = "addin_registry_updated";
   public static final String ChunkOutput = "chunk_output";
   public static final String ChunkOutputFinished = "chunk_output_finished";
   public static final String RprofStarted = "rprof_started";
   public static final String RprofStopped = "rprof_stopped";
   public static final String RprofCreated = "rprof_created";
   public static final String EditorCommand = "editor_command";
   public static final String PreviewRmd = "preview_rmd";
   public static final String WebsiteFileSaved = "website_file_saved";
   public static final String ChunkPlotRefreshed = "chunk_plot_refreshed";
   public static final String ChunkPlotRefreshFinished = "chunk_plot_refresh_finished";
   public static final String ReloadWithLastChanceSave = "reload_with_last_chance_save";
   public static final String ConnectionUpdated = "connection_updated";
   public static final String EnableConnections = "enable_connections";
   public static final String ConnectionListChanged = "connection_list_changed";
   public static final String ActiveConnectionsChanged = "active_connections_changed";
   public static final String ConnectionOpened = "connection_opened";
   public static final String NotebookRangeExecuted = "notebook_range_executed";
   public static final String ChunkExecStateChanged = "chunk_exec_state_changed";
   public static final String NavigateShinyFrame = "navigate_shiny_frame";
   public static final String UpdateNewConnectionDialog = "update_new_connection_dialog";
   public static final String ProjectTemplateRegistryUpdated = "project_template_registry_updated";
   public static final String TerminalBusy = "terminal_busy";
   public static final String PackageExtensionIndexingCompleted = "package_extension_indexing_completed";
   public static final String TerminalSubProcs = "terminal_subprocs";
   public static final String RStudioAPIShowDialog = "rstudioapi_show_dialog";
   public static final String RStudioAPIShowDialogCompleted = "rstudioapi_show_dialog_completed";
   public static final String ObjectExplorerEvent = "object_explorer_event";

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
