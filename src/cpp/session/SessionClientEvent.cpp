/*
 * SessionClientEvent.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <session/SessionClientEvent.hpp>

#include <boost/lexical_cast.hpp>

#include <shared_core/Error.hpp>
#include <core/Log.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/SafeConvert.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {

namespace client_events {
   
const int kBusy = 1;
const int kConsolePrompt = 2;
const int kConsoleWriteOutput = 3;
const int kConsoleWriteError = 4;
const int kShowErrorMessage = 5;
const int kShowHelp = 6;
const int kBrowseUrl = 7;
const int kShowEditor = 11;
const int kChooseFile = 13;
const int kAbendWarning = 14;
const int kQuit = 15;
const int kSuicide = 16;
const int kFileChanged = 17;
const int kWorkingDirChanged = 18;
const int kPlotsStateChanged = 19;
const int kPackageStatusChanged = 21;
const int kPackageStateChanged = 22;
const int kLocator = 23;
const int kConsoleResetHistory = 25;
const int kSessionSerialization = 26;
const int kHistoryEntriesAdded = 27;
const int kQuotaStatus = 29;
const int kFileEdit = 32;
const int kShowContent = 33;
const int kShowData = 34;
const int kAsyncCompletion = 35;
const int kSaveActionChanged = 36;
const int kConsoleWritePrompt = 37;
const int kConsoleWriteInput = 38;
const int kShowWarningBar = 39;
const int kOpenProjectError = 40;
const int kVcsRefresh = 41;
const int kAskPass = 42;
const int kConsoleProcessOutput = 43;
const int kConsoleProcessExit = 44;
const int kListChanged = 45;
const int kConsoleProcessCreated = 46;
const int kUserPrefsChanged = 47;
const int kHandleUnsavedChanges = 48;
const int kConsoleProcessPrompt = 49;
const int kHTMLPreviewStartedEvent = 51;
const int kHTMLPreviewOutputEvent = 52;
const int kHTMLPreviewCompletedEvent = 53;
const int kCompilePdfStartedEvent = 54;
const int kCompilePdfOutputEvent = 55;
const int kCompilePdfErrorsEvent = 56;
const int kCompilePdfCompletedEvent = 57;
const int kSynctexEditFile = 58;
const int kFindResult = 59;
const int kFindOperationEnded = 60;
const int kRPubsUploadStatus = 61;
const int kBuildStarted = 62;
const int kBuildOutput = 63;
const int kBuildCompleted = 64;
const int kBuildErrors = 65;
const int kDirectoryNavigate = 66;
const int kDeferredInitCompleted = 67;
const int kPlotsZoomSizeChanged = 68;
const int kSourceCppStarted = 69;
const int kSourceCppCompleted = 70;
const int kLoadedPackageUpdates = 71;
const int kActivatePane = 72;
const int kShowPresentationPane = 73;
const int kEnvironmentRefresh = 74;
const int kContextDepthChanged = 75;
const int kEnvironmentAssigned = 76;
const int kEnvironmentRemoved = 77;
const int kBrowserLineChanged = 78;
const int kPackageLoaded = 79;
const int kPackageUnloaded = 80;
const int kPresentationPaneRequestCompleted = 81;
const int kUnhandledError = 82;
const int kErrorHandlerChanged = 83;
const int kViewerNavigate = 84;
const int kSourceExtendedTypeDetected = 85;
const int kShinyViewer = 86;
const int kDebugSourceCompleted = 87;
const int kRmdRenderStarted = 88;
const int kRmdRenderOutput = 89;
const int kRmdRenderCompleted = 90;
const int kRmdTemplateDiscovered = 91;
const int kRmdTemplateDiscoveryCompleted = 92;
const int kRmdShinyDocStarted = 93;
const int kRmdRSConnectDeploymentOutput = 94;
const int kRmdRSConnectDeploymentCompleted = 95;
const int kUserPrompt = 96;
const int kInstallRtools = 97;
const int kInstallShiny = 98;
const int kSuspendAndRestart = 99;
const int kDataViewChanged = 100;
const int kViewFunction = 101;
const int kMarkersChanged = 102;
const int kEnableRStudioConnect = 103;
const int kUpdateGutterMarkers = 104;
const int kSnippetsChanged = 105;
const int kJumpToFunction = 106;
const int kCollabEditStarted = 107;
const int kSessionCountChanged = 108;
const int kCollabEditEnded = 109;
const int kProjectUsersChanged = 110;
const int kRVersionsChanged = 111;
const int kShinyGadgetDialog = 112;
const int kRmdParamsReady = 113;
const int kRegisterUserCommand = 114;
const int kRmdRSConnectDeploymentFailed = 115;
const int kSendToConsole = 119;
const int kUserFollowStarted = 120;
const int kUserFollowEnded = 121;
const int kProjectAccessRevoked = 122;
const int kCollabEditSaved = 123;
const int kAddinRegistryUpdated = 124;
const int kChunkOutput = 125;
const int kChunkOutputFinished = 126;
const int kRprofStarted = 127;
const int kRprofStopped = 128;
const int kRprofCreated = 129;
const int kEditorCommand = 131;
const int kPreviewRmd = 132;
const int kWebsiteFileSaved = 133;
const int kChunkPlotRefreshed = 134;
const int kChunkPlotRefreshFinished = 135;
const int kReloadWithLastChanceSave = 136;
const int kConnectionUpdated = 139;
const int kEnableConnections = 140;
const int kConnectionListChanged = 141;
const int kActiveConnectionsChanged = 142;
const int kConnectionOpened = 143;
const int kNotebookRangeExecuted = 144;
const int kChunkExecStateChanged = 145;
const int kNavigateShinyFrame = 146;
const int kUpdateNewConnectionDialog = 147;
const int kProjectTemplateRegistryUpdated = 148;
const int kTerminalSubprocs = 149;
const int kPackageExtensionIndexingCompleted = 150;
const int kRStudioAPIShowDialog = 151;
const int kRStudioAPIShowDialogCompleted = 152;
const int kObjectExplorerEvent = 153;
const int kSendToTerminal = 154;
const int kClearTerminal = 155;
const int kAddTerminal = 156;
const int kActivateTerminal = 157;
const int kTerminalCwd = 158;
const int kAdminNotification = 159;
const int kRequestDocumentSave = 160;
const int kRequestDocumentSaveCompleted = 161;
const int kRequestOpenProject = 162;
const int kOpenFileDialog = 163;
const int kRemoveTerminal = 164;
const int kShowPageViewerEvent = 165;
const int kAskSecret = 166;
const int kTestsStarted = 167;
const int kTestsOutput = 168;
const int kTestsCompleted = 169;
const int kJobUpdated = 170;
const int kJobRefresh = 171;
const int kJobOutput = 172;
const int kDataOutputCompleted = 173;
const int kNewDocumentWithCode = 174;
const int kPlumberViewer = 175;
const int kAvailablePackagesReady = 176;
const int kComputeThemeColors = 177;
const int kRequestDocumentClose = 178;
const int kRequestDocumentCloseCompleted = 179;
const int kExecuteAppCommand = 180;
const int kUserStateChanged = 181;
const int kHighlightUi = 182;
const int kReplaceResult = 183;
const int kReplaceUpdated = 184;
const int kTutorialCommand = 185;
const int kTutorialLaunch = 186;
const int kReticulateEvent = 187;
const int kEnvironmentChanged = 188;
const int kRStudioApiRequest = 189;
const int kDocumentCloseAllNoSave = 190;
const int kMemoryUsageChanged = 191;
const int kCommandCallbacksChanged = 192;
const int kConsoleActivate = 193;
const int kJobsActivate = 194;
const int kPresentationPreview = 195;
const int kSuspendBlocked = 196;
const int kClipboardAction = 197;
const int kDeploymentRecordsUpdated = 198;
const int kRunAutomation = 199;
const int kConsoleWritePendingError = 200;
const int kConsoleWritePendingWarning = 201;

}

void ClientEvent::init(int type, const json::Value& data)
{
   type_ = type;
   data_ = data;
   id_ = core::system::generateUuid();
}
   
void ClientEvent::asJsonObject(int id, json::Object* pObject) const
{
   json::Object& object = *pObject;
   object["id"] = id;
   object["type"] = typeName();
   object["data"] = data();
}
   
std::string ClientEvent::typeName() const 
{
   switch (type_)
   {
      case client_events::kBusy:
         return "busy";
      case client_events::kConsolePrompt:
         return "console_prompt";
      case client_events::kConsoleWriteOutput:
         return "console_output";
      case client_events::kConsoleWriteError: 
         return "console_error";
      case client_events::kShowErrorMessage: 
         return "show_error_message";
      case client_events::kShowHelp: 
         return "show_help";
      case client_events::kBrowseUrl: 
         return "browse_url";
      case client_events::kShowEditor: 
         return "show_editor";
      case client_events::kChooseFile: 
         return "choose_file";
      case client_events::kAbendWarning:
         return "abend_warning";
      case client_events::kQuit:
         return "quit";
      case client_events::kSuicide: 
         return "suicide";
      case client_events::kFileChanged:
         return "file_changed";
      case client_events::kWorkingDirChanged: 
         return "working_dir_changed";
      case client_events::kPlotsStateChanged: 
         return "plots_state_changed";
      case client_events::kPackageStatusChanged: 
         return "package_status_changed";
      case client_events::kPackageStateChanged: 
         return "package_state_changed";
      case client_events::kLocator:
         return "locator";
      case client_events::kConsoleResetHistory:
         return "console_reset_history";
      case client_events::kSessionSerialization:
         return "session_serialization";
      case client_events::kHistoryEntriesAdded:
         return "history_entries_added";
      case client_events::kQuotaStatus:
         return "quota_status";
      case client_events::kFileEdit:
         return "file_edit";
      case client_events::kShowContent:
         return "show_content";
      case client_events::kShowData:
         return "show_data";
      case client_events::kAsyncCompletion:
         return "async_completion";
      case client_events::kSaveActionChanged:
         return "save_action_changed";
      case client_events::kConsoleWritePrompt:
         return "console_write_prompt";
      case client_events::kConsoleWriteInput:
         return "console_write_input";
      case client_events::kShowWarningBar:
         return "show_warning_bar";
      case client_events::kOpenProjectError:
         return "open_project_error";
      case client_events::kVcsRefresh:
         return "vcs_refresh";
      case client_events::kAskPass:
         return "ask_pass";
      case client_events::kConsoleProcessOutput:
         return "console_process_output";
      case client_events::kConsoleProcessExit:
         return "console_process_exit";
      case client_events::kListChanged:
         return "list_changed";
      case client_events::kUserPrefsChanged:
         return "user_prefs_changed";
      case client_events::kHandleUnsavedChanges:
         return "handle_unsaved_changes";
      case client_events::kConsoleProcessPrompt:
         return "console_process_prompt";
      case client_events::kConsoleProcessCreated:
         return "console_process_created";
      case client_events::kHTMLPreviewStartedEvent:
         return "html_preview_started_event";
      case client_events::kHTMLPreviewOutputEvent:
         return "html_preview_output_event";
      case client_events::kHTMLPreviewCompletedEvent:
         return "html_preview_completed_event";
      case client_events::kCompilePdfStartedEvent:
         return "compile_pdf_started_event";
      case client_events::kCompilePdfOutputEvent:
         return "compile_pdf_output_event";
      case client_events::kCompilePdfErrorsEvent:
         return "compile_pdf_errors_event";
      case client_events::kCompilePdfCompletedEvent:
         return "compile_pdf_completed_event";
      case client_events::kSynctexEditFile:
         return "synctex_edit_file";
      case client_events::kFindResult:
         return "find_result";
      case client_events::kFindOperationEnded:
         return "find_operation_ended";
      case client_events::kRPubsUploadStatus:
         return "rpubs_upload_status";
      case client_events::kBuildStarted:
         return "build_started";
      case client_events::kBuildOutput:
         return "build_output";
      case client_events::kBuildCompleted:
         return "build_completed";
      case client_events::kBuildErrors:
         return "build_errors";
      case client_events::kDirectoryNavigate:
         return "directory_navigate";
      case client_events::kDeferredInitCompleted:
         return "deferred_init_completed";
      case client_events::kPlotsZoomSizeChanged:
         return "plots_zoom_size_changed";
      case client_events::kSourceCppStarted:
         return "source_cpp_started";
      case client_events::kSourceCppCompleted:
         return "source_cpp_completed";
      case client_events::kLoadedPackageUpdates:
         return "loaded_package_updates";
      case client_events::kActivatePane:
         return "activate_pane";
      case client_events::kShowPresentationPane:
         return "show_presentation_pane";
      case client_events::kEnvironmentRefresh:
         return "environment_refresh";
      case client_events::kContextDepthChanged:
         return "context_depth_changed";
      case client_events::kEnvironmentAssigned:
         return "environment_assigned";
      case client_events::kEnvironmentRemoved:
         return "environment_removed";
      case client_events::kBrowserLineChanged:
         return "browser_line_changed";
      case client_events::kPackageLoaded:
         return "package_loaded";
      case client_events::kPackageUnloaded:
         return "package_unloaded";
      case client_events::kPresentationPaneRequestCompleted:
         return "presentation_pane_request_completed";
      case client_events::kUnhandledError:
         return "unhandled_error";
      case client_events::kErrorHandlerChanged:
         return "error_handler_changed";
      case client_events::kViewerNavigate:
         return "viewer_navigate";
      case client_events::kSourceExtendedTypeDetected:
         return "source_extended_type_detected";
      case client_events::kShinyViewer:
         return "shiny_viewer";
      case client_events::kDebugSourceCompleted:
         return "debug_source_completed";
      case client_events::kRmdRenderStarted:
         return "rmd_render_started";
      case client_events::kRmdRenderOutput:
         return "rmd_render_output";
      case client_events::kRmdRenderCompleted:
         return "rmd_render_completed";
      case client_events::kRmdShinyDocStarted:
         return "rmd_shiny_doc_started";
      case client_events::kRmdRSConnectDeploymentOutput:
         return "rsconnect_deployment_output";
      case client_events::kRmdRSConnectDeploymentCompleted:
         return "rsconnect_deployment_completed";
      case client_events::kRmdRSConnectDeploymentFailed:
         return "rsconnect_deployment_failed";
      case client_events::kUserPrompt:
         return "user_prompt";
      case client_events::kInstallRtools:
         return "install_r_tools";
      case client_events::kInstallShiny:
         return "install_shiny";
      case client_events::kSuspendAndRestart:
         return "suspend_and_restart";
      case client_events::kDataViewChanged:
         return "data_view_changed";
      case client_events::kViewFunction:
         return "view_function";
      case client_events::kMarkersChanged:
         return "markers_changed";
      case client_events::kEnableRStudioConnect:
         return "enable_rstudio_connect";
      case client_events::kUpdateGutterMarkers:
         return "update_gutter_markers";
      case client_events::kSnippetsChanged:
         return "snippets_changed";
      case client_events::kJumpToFunction:
         return "jump_to_function";
      case client_events::kCollabEditStarted:
         return "collab_edit_started";
      case client_events::kSessionCountChanged:
         return "session_count_changed";
      case client_events::kCollabEditEnded:
         return "collab_edit_ended";
      case client_events::kProjectUsersChanged:
         return "project_users_changed";
      case client_events::kRVersionsChanged:
         return "r_versions_changed";
      case client_events::kShinyGadgetDialog:
         return "shiny_gadget_dialog";
      case client_events::kRmdParamsReady:
         return "rmd_params_ready";
      case client_events::kRegisterUserCommand:
         return "register_user_command";
      case client_events::kSendToConsole:
         return "send_to_console";
      case client_events::kUserFollowStarted:
         return "user_follow_started";
      case client_events::kUserFollowEnded:
         return "user_follow_ended";
      case client_events::kProjectAccessRevoked:
         return "project_access_revoked";
      case client_events::kCollabEditSaved:
         return "collab_edit_saved";
      case client_events::kAddinRegistryUpdated:
         return "addin_registry_updated";
      case client_events::kChunkOutput:
         return "chunk_output";
      case client_events::kChunkOutputFinished:
         return "chunk_output_finished";
      case client_events::kRprofStarted:
         return "rprof_started";
      case client_events::kRprofStopped:
         return "rprof_stopped";
      case client_events::kRprofCreated:
         return "rprof_created";
      case client_events::kEditorCommand:
         return "editor_command";
      case client_events::kPreviewRmd:
         return "preview_rmd";
      case client_events::kWebsiteFileSaved:
         return "website_file_saved";
      case client_events::kChunkPlotRefreshed:
         return "chunk_plot_refreshed";
      case client_events::kChunkPlotRefreshFinished:
         return "chunk_plot_refresh_finished";
      case client_events::kReloadWithLastChanceSave:
         return "reload_with_last_chance_save";
      case client_events::kConnectionUpdated:
         return "connection_updated";
      case client_events::kEnableConnections:
         return "enable_connections";
      case client_events::kConnectionListChanged:
         return "connection_list_changed";
      case client_events::kActiveConnectionsChanged:
         return "active_connections_changed";
      case client_events::kConnectionOpened:
         return "connection_opened";
      case client_events::kNotebookRangeExecuted:
         return "notebook_range_executed";
      case client_events::kChunkExecStateChanged:
         return "chunk_exec_state_changed";
      case client_events::kNavigateShinyFrame:
         return "navigate_shiny_frame";
      case client_events::kUpdateNewConnectionDialog:
         return "update_new_connection_dialog";
      case client_events::kProjectTemplateRegistryUpdated:
         return "project_template_registry_updated";
      case client_events::kTerminalSubprocs:
         return "terminal_subprocs";
      case client_events::kPackageExtensionIndexingCompleted:
         return "package_extension_indexing_completed";
      case client_events::kRStudioAPIShowDialog:
         return "rstudioapi_show_dialog";
      case client_events::kRStudioAPIShowDialogCompleted:
         return "rstudioapi_show_dialog_completed";
      case client_events::kObjectExplorerEvent:
         return "object_explorer_event";
      case client_events::kSendToTerminal:
         return "send_to_terminal";
      case client_events::kClearTerminal:
         return "clear_terminal";
      case client_events::kAddTerminal:
         return "add_terminal";
      case client_events::kActivateTerminal:
         return "activate_terminal";
      case client_events::kTerminalCwd:
         return "terminal_cwd";
      case client_events::kAdminNotification:
         return "admin_notification";
      case client_events::kRequestDocumentSave:
         return "request_document_save";
      case client_events::kRequestDocumentSaveCompleted:
         return "request_document_save_completed";
      case client_events::kRequestOpenProject:
         return "request_open_project";
      case client_events::kOpenFileDialog:
         return "open_file_dialog";
      case client_events::kRemoveTerminal:
         return "remove_terminal";
      case client_events::kShowPageViewerEvent:
         return "show_page_viewer";
      case client_events::kAskSecret:
         return "ask_secret";
      case client_events::kTestsStarted:
         return "tests_started";
      case client_events::kTestsOutput:
         return "tests_output";
      case client_events::kTestsCompleted:
         return "tests_completed";
      case client_events::kJobUpdated:
         return "job_updated";
      case client_events::kJobRefresh:
         return "job_refresh";
      case client_events::kJobOutput:
         return "job_output";
      case client_events::kDataOutputCompleted:
         return "data_output_completed";
      case client_events::kNewDocumentWithCode:
         return "new_document_with_code";
      case client_events::kAvailablePackagesReady:
         return "available_packages_ready";
      case client_events::kPlumberViewer:
         return "plumber_viewer";
      case client_events::kComputeThemeColors:
         return "compute_theme_colors";
      case client_events::kRequestDocumentClose:
         return "request_document_close";
      case client_events::kRequestDocumentCloseCompleted:
         return "request_document_close_completed";
      case client_events::kExecuteAppCommand:
         return "execute_app_command";
      case client_events::kUserStateChanged:
         return "user_state_changed";
      case client_events::kHighlightUi:
         return "highlight_ui";
      case client_events::kReplaceResult:
         return "replace_result";
      case client_events::kReplaceUpdated:
         return "replace_updated";
      case client_events::kTutorialCommand:
         return "tutorial_command";
      case client_events::kTutorialLaunch:
         return "tutorial_launch";
      case client_events::kReticulateEvent:
         return "reticulate_event";
      case client_events::kEnvironmentChanged:
         return "environment_changed";
      case client_events::kRStudioApiRequest:
         return "rstudioapi_request";
      case client_events::kDocumentCloseAllNoSave:
         return "document_close_all_no_save";
      case client_events::kMemoryUsageChanged:
         return "memory_usage_changed";
      case client_events::kCommandCallbacksChanged:
         return "command_callbacks_changed";
      case client_events::kConsoleActivate:
         return "console_activate";
      case client_events::kJobsActivate:
         return "jobs_activate";
      case client_events::kPresentationPreview:
         return "presentation_preview";
      case client_events::kSuspendBlocked:
         return "session_suspend_blocked";
      case client_events::kClipboardAction:
         return "clipboard_action";
      case client_events::kDeploymentRecordsUpdated:
         return "deployment_records_updated";
      case client_events::kRunAutomation:
         return "run_automation";
      case client_events::kConsoleWritePendingError:
         return "console_write_pending_error";
      case client_events::kConsoleWritePendingWarning:
         return "console_write_pending_warning";
      default:
         LOG_WARNING_MESSAGE("unexpected event type: " + 
                             safe_convert::numberToString(type_));
         return "";
   }
}

ClientEvent showEditorEvent(const std::string& content,
                            bool isRCode,
                            bool lineWrapping)
{
   json::Object data;
   data["content"] = content;
   data["is_r_code"] = isRCode;
   data["line_wrapping"] = lineWrapping;
   return ClientEvent(client_events::kShowEditor, data);
}

ClientEvent browseUrlEvent(const std::string& url, const std::string& window)
{
   json::Object browseURLInfo;
   browseURLInfo["url"] = url;
   browseURLInfo["window"] = window;
   return ClientEvent(client_events::kBrowseUrl, browseURLInfo);
}
    
   
ClientEvent showErrorMessageEvent(const std::string& title,
                                  const std::string& message)
{
   json::Object errorMessage;
   errorMessage["title"] = title;
   errorMessage["message"] = message;
   return ClientEvent(client_events::kShowErrorMessage, errorMessage);
}


   
   
} // namespace session
} // namespace rstudio
