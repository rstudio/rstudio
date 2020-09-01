/*
 * SessionInfo.java
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
package org.rstudio.studio.client.workbench.model;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.application.ApplicationUtils;
import org.rstudio.studio.client.application.model.RVersionsInfo;
import org.rstudio.studio.client.application.model.SessionInitOptions;
import org.rstudio.studio.client.common.compilepdf.model.CompilePdfState;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.common.debugging.model.ErrorManagerState;
import org.rstudio.studio.client.common.dependencies.model.DependencyList;
import org.rstudio.studio.client.common.rnw.RnwWeave;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;
import org.rstudio.studio.client.workbench.prefs.model.PrefLayer;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.buildtools.model.BuildState;
import org.rstudio.studio.client.workbench.views.connections.model.Connection;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionId;
import org.rstudio.studio.client.workbench.views.environment.model.EnvironmentContextData;
import org.rstudio.studio.client.workbench.views.jobs.model.JobState;
import org.rstudio.studio.client.workbench.views.output.find.model.FindInFilesState;
import org.rstudio.studio.client.workbench.views.output.markers.model.MarkersState;
import org.rstudio.studio.client.workbench.views.packages.model.PackageProvidedExtensions;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationState;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public class SessionInfo extends JavaScriptObject
{
   protected SessionInfo()
   {
   }

   public final native String getClientId() /*-{
      return this.clientId;
   }-*/;

   public final native String getClientVersion() /*-{
      return this.client_version;
   }-*/;

   public final native String getUserIdentity() /*-{
      return this.userIdentity;
   }-*/;

   public final native String getSystemUsername() /*-{
      return this.systemUsername;
   }-*/;

   public final native String getSessionId() /*-{
      return this.session_id;
   }-*/;

   public final native String getSessionLabel() /*-{
      return this.session_label;
   }-*/;

   public final native JsArray<RnwWeave> getRnwWeaveTypes() /*-{
      return this.rnw_weave_types;
   }-*/;

   public final native JsArrayString getLatexProgramTypes() /*-{
      return this.latex_program_types;
   }-*/;

   public final native TexCapabilities getTexCapabilities() /*-{
      return this.tex_capabilities;
   }-*/;

   public final native CompilePdfState getCompilePdfState() /*-{
      return this.compile_pdf_state;
   }-*/;

   public final native FindInFilesState getFindInFilesState() /*-{
      return this.find_in_files_state;
   }-*/;

   public final native MarkersState getMarkersState() /*-{
      return this.markers_state;
   }-*/;

   public final native String getLogDir() /*-{
      return this.log_dir;
   }-*/;

   public final native String getScratchDir() /*-{
      return this.scratch_dir;
   }-*/;

   public final native String getTempDir() /*-{
      return this.temp_dir;
   }-*/;

   public final native JsArray<PrefLayer> getPrefs() /*-{
      if (!this.user_prefs)
         this.user_prefs = [ {} ];
      return this.user_prefs;
   }-*/;

   public final JsObject getUserPrefs()
   {
      return getPrefs().get(Math.min(getPrefs().length(), UserPrefs.LAYER_USER)).getValues();
   }

   public final PrefLayer getUserPrefLayer()
   {
      return getPrefs().get(Math.min(getPrefs().length(), UserPrefs.LAYER_USER));
   }

   public final native JsArray<PrefLayer> getUserState() /*-{
      if (!this.user_state)
         this.user_state = [ {} ];
      return this.user_state;
   }-*/;

   public final PrefLayer getUserStateLayer()
   {
      return getUserState().get(Math.min(getPrefs().length(), UserState.LAYER_USER));
   }

   public final static String DESKTOP_MODE = "desktop";
   public final static String SERVER_MODE = "server";

   public final native String getMode() /*-{
      return this.mode;
   }-*/;

   public final native boolean getResumed() /*-{
      return this.resumed;
   }-*/;

   public final native String getDefaultPrompt() /*-{
      return this.prompt;
   }-*/;

   public final native JsArrayString getConsoleHistory() /*-{
      return this.console_history;
   }-*/;

   public final native int getConsoleHistoryCapacity() /*-{
      return this.console_history_capacity;
   }-*/;

   public final native RpcObjectList<ConsoleAction> getConsoleActions() /*-{
      return this.console_actions;
   }-*/;

   public final native int getConsoleActionsLimit() /*-{
      return this.console_actions_limit;
   }-*/;

   public final native String getConsoleLanguage() /*-{
      return this.console_language;
   }-*/;

   public final native ClientInitState getClientState() /*-{
      return this.client_state;
   }-*/;

   public final native JsArray<SourceDocument> getSourceDocuments() /*-{
      return this.source_documents;
   }-*/;

   public final native void setSourceDocuments(JsArray<SourceDocument> docs) /*-{
      this.source_documents = docs;
   }-*/;

   public final native WorkbenchLists getLists() /*-{
      return this.lists;
   }-*/;

   public final native String docsURL() /*-{
      return this.docsURL;
   }-*/;

   public final native String getRstudioVersion() /*-{
      return this.rstudio_version;
   }-*/;

   public final native String getSystemEncoding() /*-{
      return this.system_encoding;
   }-*/;

   public final native String getLicenseMessage() /*-{
      return this.license_message;
   }-*/;

   public final boolean isVcsEnabled()
   {
      return !StringUtil.isNullOrEmpty(getVcsName());
   }

   public final boolean isVcsAvailable()
   {
      String[] availableVcs = getAvailableVCS();
      return availableVcs.length > 0 && availableVcs[0].length() > 0;
   }

   public final String[] getAvailableVCS()
   {
      return this.<JsObject>cast().getString("vcs_available", true).split(",");
   }

   public final native String getVcsName() /*-{
      return this.vcs;
   }-*/;

   public final boolean isVcsAvailable(String id)
   {
      String[] availableVcs = getAvailableVCS();
      for (int i=0; i<availableVcs.length; i++)
      {
         if (availableVcs[i] == id)
            return true;
      }

      return false;
   }

   public native final String getDefaultSSHKeyDir() /*-{
      return this.default_ssh_key_dir;
   }-*/;

   public native final boolean isGithubRepository() /*-{
      return this.is_github_repo;
   }-*/;

   // TODO: The check for null was for migration in the presence of
   // sessions that couldn't suspend (3/21/2011). Remove this check
   // once we are sufficiently clear of this date window.
   public final native String getInitialWorkingDir() /*-{
      if (!this.initial_working_dir)
         this.initial_working_dir = "~/";
      return this.initial_working_dir;
   }-*/;

   public final native String getDefaultWorkingDir() /*-{
      return this.default_working_dir;
   }-*/;

   public final native String getDefaultProjectDir() /*-{
      return this.default_project_dir;
   }-*/;

   public final native String getActiveProjectFile() /*-{
      return this.active_project_file;
   }-*/;

   public final FileSystemItem getActiveProjectDir()
   {
      String projFile = getActiveProjectFile();
      if (projFile != null)
      {
         return FileSystemItem.createFile(projFile).getParentPath();
      }
      else
      {
         return null;
      }
   }

   public final native JsArrayString getProjectOpenDocs() /*-{
      if (!this.project_open_docs)
         this.project_open_docs = {};
      return this.project_open_docs;
   }-*/;

   public final native boolean projectSupportsSharing() /*-{
      return !!this.project_supports_sharing;
   }-*/;

   public final native boolean projectParentBrowseable() /*-{
      return !!this.project_parent_browseable;
   }-*/;

   public final native String getProjectUserDataDir() /*-{
      return this.project_user_data_directory;
   }-*/;

   public final native JsArray<ConsoleProcessInfo> getConsoleProcesses() /*-{
      return this.console_processes;
   }-*/;

   public final native String getSumatraPdfExePath() /*-{
      return this.sumatra_pdf_exe_path;
   }-*/;

   public final native HTMLCapabilities getHTMLCapabilities() /*-{
      return this.html_capabilities;
   }-*/;

   public final static String BUILD_TOOLS_NONE = "None";
   public final static String BUILD_TOOLS_PACKAGE = "Package";
   public final static String BUILD_TOOLS_MAKEFILE = "Makefile";
   public final static String BUILD_TOOLS_WEBSITE = "Website";
   public final static String BUILD_TOOLS_CUSTOM = "Custom";

   public final native String getBuildToolsType() /*-{
      return this.build_tools_type;
   }-*/;

   public final native boolean getBuildToolsBookdownWebsite() /*-{
      return this.build_tools_bookdown_website;
   }-*/;

   public final native String getBuildTargetDir() /*-{
      return this.build_target_dir;
   }-*/;

   public final native BlogdownConfig getBlogdownConfig() /*-{
      return this.blogdown_config;
   }-*/;
   
   public final native boolean getIsBookdownProject() /*-{
      return this.is_bookdown_project;
   }-*/;

   public final native boolean getIsDistillProject() /*-{
      return this.is_distill_project;
   }-*/;

   public final native boolean getHasPackageSrcDir() /*-{
      return this.has_pkg_src;
   }-*/;

   public final native boolean getHasPackageVignetteDir() /*-{
      return this.has_pkg_vig;
   }-*/;

   public final String getPresentationName()
   {
      PresentationState state = getPresentationState();
      if (state != null)
         return state.getPaneCaption();
      else
         return "Presentation";
   }

   public final native PresentationState getPresentationState() /*-{
      return this.presentation_state;
   }-*/;

   public final native BuildState getBuildState() /*-{
      return this.build_state;
   }-*/;

   public final native boolean isDevtoolsInstalled() /*-{
      return this.devtools_installed;
   }-*/;

   public final native boolean isCairoPdfAvailable() /*-{
      return this.have_cairo_pdf;
   }-*/;

   public final native boolean getAllowVcsExeEdit() /*-{
      return this.allow_vcs_exe_edit;
   }-*/;

   public final native boolean getAllowCRANReposEdit() /*-{
      return this.allow_cran_repos_edit;
   }-*/;

   public final native boolean getAllowVcs() /*-{
      return this.allow_vcs;
   }-*/;

   public final native boolean getAllowPackageInstallation() /*-{
      return this.allow_pkg_install;
   }-*/;

   public final native boolean getAllowShell() /*-{
      return this.allow_shell;
   }-*/;

   public final native boolean getAllowTerminalWebsockets() /*-{
      return this.allow_terminal_websockets;
   }-*/;

   public final native boolean getAllowFileDownloads() /*-{
      return this.allow_file_download;
   }-*/;

   public final native boolean getAllowFileUploads() /*-{
      return this.allow_file_upload;
   }-*/;

   public final native boolean getAllowRemovePublicFolder() /*-{
      return this.allow_remove_public_folder;
   }-*/;

   public final native boolean getAllowFullUI() /*-{
      return this.allow_full_ui;
   }-*/;

   public final native int getWebSocketPingInterval() /*-{
      return this.websocket_ping_interval;
   }-*/;

   public final native int getWebSocketConnectTimeout() /*-{
      return this.websocket_connect_timeout;
   }-*/;

   public final native boolean getAllowExternalPublish() /*-{
      return this.allow_external_publish;
   }-*/;

   public final native boolean getAllowPublish() /*-{
      return this.allow_publish;
   }-*/;

   public final native boolean getAllowOpenSharedProjects() /*-{
      return this.allow_open_shared_projects;
   }-*/;

   public final native boolean getProjectSharingEnumerateServerUsers() /*-{
      return this.project_sharing_enumerate_server_users;
   }-*/;

   public final native boolean getLauncherSession() /*-{
      return this.launcher_session;
   }-*/;

   public final native boolean getCrashHandlerSettingsModifiable() /*-{
      return this.crash_handler_settings_modifiable;
   }-*/;

   public final native boolean getPromptForCrashHandlerPermission() /*-{
      return this.prompt_for_crash_handler_permission;
   }-*/;

   public final native JsObject getLaunchParameters() /*-{
      return this.launch_parameters;
   }-*/;

   public final native EnvironmentContextData getEnvironmentState() /*-{
      return this.environment_state;
   }-*/;

   public final native boolean getDisablePackages() /*-{
      return this.disable_packages;
   }-*/;

   public final native ErrorManagerState getErrorState() /*-{
      return this.error_state;
   }-*/;

   public final native boolean getDisableCheckForUpdates() /*-{
      return this.disable_check_for_updates;
   }-*/;

   public final native boolean getShowIdentity() /*-{
      return this.show_identity;
   }-*/;

   public final native boolean getHaveAdvancedStepCommands() /*-{
      return this.have_advanced_step_commands;
   }-*/;

   public final native boolean getRMarkdownPackageAvailable() /*-{
      return this.rmarkdown_available;
   }-*/;

   public final native void setRMarkdownPackageAvailable(boolean available) /*-{
      this.rmarkdown_available = available;
   }-*/;

   public final native boolean getKnitParamsAvailable()  /*-{
      return this.knit_params_available;
   }-*/;

   public final native void setKnitParamsAvailable(boolean available) /*-{
      this.knit_params_available = available;
   }-*/;

   public final native boolean getKnitWorkingDirAvailable() /*-{
      return this.knit_working_dir_available;
   }-*/;

   public final native void setKnitWorkingDirAvailable(boolean available) /*-{
      this.knit_working_dir_available = available;
   }-*/;

   public final native boolean getPptAvailable() /*-{
      return this.ppt_available;
   }-*/;

   public final native void setPptAvailable(boolean available) /*-{
      this.ppt_available = available;
   }-*/;

   public final native boolean getClangAvailable() /*-{
      return this.clang_available;
   }-*/;

   public final native boolean getConnectionsEnabled() /*-{
      return this.connections_enabled;
   }-*/;

   public final native boolean getActivateConnections() /*-{
      return this.activate_connections;
   }-*/;

   public final native JsArray<Connection> getConnectionList() /*-{
      return this.connection_list;
   }-*/;

   public final native JsArray<ConnectionId> getActiveConnections() /*-{
      return this.active_connections;
   }-*/;

   public final native JobState getJobState() /*-{
      return this.job_state;
   }-*/;

   public final native boolean getLauncherJobsEnabled() /*-{
      return this.launcher_jobs_enabled;
   }-*/;

   public final native boolean getShowHelpHome() /*-{
      return this.show_help_home;
   }-*/;

   public final native boolean getMultiSession() /*-{
      return this.multi_session;
   }-*/;

   public final native int getActiveSessionCount() /*-{
      return this.active_session_count;
   }-*/;

   public final native RVersionsInfo getRVersionsInfo() /*-{
      return this.r_versions_info;
   }-*/;

   public final native boolean getPresentationCommands() /*-{
      return this.presentation_commands;
   }-*/;

   public final native boolean getTutorialApiAvailable() /*-{
      return this.tutorial_api_available;
   }-*/;

   public final native boolean getTutorialApiParentNotifyAvailable() /*-{
      return this.tutorial_api_parent_notify_available;
   }-*/;

   public final native String getTutorialApiClientOrigin() /*-{
      return this.tutorial_api_client_origin;
   }-*/;

   public final native boolean getCloudFolderEnabled() /*-{
      return this.cloud_folder_enabled;
   }-*/;

    public final native boolean getPackratAvailable() /*-{
      return this.packrat_available;
   }-*/;

    public final native boolean getRenvAvailable() /*-{
      return this.renv_available;
   }-*/;

   public final native boolean getShowUserHomePage() /*-{
      return this.show_user_home_page;
   }-*/;

   public final String getUserHomePageUrl()
   {
      String url = getUserHomePageUrlNative();
      if (url != null)
         url = ApplicationUtils.getHostPageBaseURLWithoutContext(false) + url;
      return url;
   }

   private final native String getUserHomePageUrlNative() /*-{
      return this.user_home_page_url;
   }-*/;

   public final native RAddins getAddins() /*-{
      return this.r_addins;
   }-*/;

   public final native PackageProvidedExtensions.Data getPackageProvidedExtensions() /*-{
      return this.package_provided_extensions;
   }-*/;

   public final native boolean getSupportDriverLicensing() /*-{
      return this.drivers_support_licensing;
   }-*/;

   public final native boolean quitChildProcessesOnExit() /*-{
      return this.quit_child_processes_on_exit;
   }-*/;

   public final native int getGitCommitLargeFileSize() /*-{
      return this.git_commit_large_file_size;
   }-*/;

   public final native String getDefaultRSConnectServer() /*-{
      return this.default_rsconnect_server;
   }-*/;

   public final native String getRLibsUser() /*-{
      return this.r_libs_user;
   }-*/;

   public final native SessionInitOptions getInitOptions() /*-{
      return this.init_options;
   }-*/;

   /**
    * @return The list of R packages we depend on
    */
   public final native DependencyList getPackageDependencies() /*-{
      return this.package_dependencies;
   }-*/;

   /**
    * @return project identifier (only meaningful in RStudio Server Pro)
    */
   public final native String getProjectId() /*-{
      return this.project_id;
   }-*/;

   public final native JsArrayString getGraphicsBackends() /*-{
     return this.graphics_backends;
   }-*/;

   public final native boolean getPythonInitialized() /*-{
      return this.python_initialized;
   }-*/;

   public final native String getSessionNode() /*-{
      return this.session_node;
   }-*/;
   
   public final native String getReticulatePython() /*-{
      return this.reticulate_python;
   }-*/;
   
}
