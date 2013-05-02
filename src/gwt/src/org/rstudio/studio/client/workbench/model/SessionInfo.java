/*
 * SessionInfo.java
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
package org.rstudio.studio.client.workbench.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.common.compilepdf.model.CompilePdfState;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.common.rnw.RnwWeave;
import org.rstudio.studio.client.workbench.views.buildtools.model.BuildState;
import org.rstudio.studio.client.workbench.views.output.find.model.FindInFilesState;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationState;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

public class SessionInfo extends JavaScriptObject
{
   protected SessionInfo()
   {
   }
   
   public final native String getClientId() /*-{
      return this.clientId;
   }-*/;
   
   public final native double getClientVersion() /*-{
      return this.version;
   }-*/;

   public final native String getUserIdentity() /*-{
      return this.userIdentity;
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

   public final native String getLogDir() /*-{
      return this.log_dir;
   }-*/;

   public final native String getScratchDir() /*-{
      return this.scratch_dir;
   }-*/;
   
   public final native String getTempDir() /*-{
      return this.temp_dir;
   }-*/;

   public final native JsObject getUiPrefs() /*-{
      if (!this.ui_prefs)
         this.ui_prefs = {};
      return this.ui_prefs;
   }-*/;

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

   public final native ClientInitState getClientState() /*-{
      return this.client_state;
   }-*/;
   
   public final native JsArray<SourceDocument> getSourceDocuments() /*-{
      return this.source_documents;
   }-*/;
   
   public final native WorkbenchLists getLists() /*-{
      return this.lists;
   }-*/;

   public final native boolean hasAgreement() /*-{
      return this.hasAgreement;
   }-*/;
   
   public final native Agreement pendingAgreement() /*-{
      return this.pendingAgreement;
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
         if (availableVcs[i].equals(id))
            return true;
      }
      
      return false;
   }
      
   public native final String getDefaultSSHKeyDir() /*-{
      return this.default_ssh_key_dir;
   }-*/;

   // TODO: The check for null was for migration in the presence of 
   // sessions that couldn't suspend (3/21/2011). Remove this check
   // once we are sufficiently clear of this date window.
   public final native String getInitialWorkingDir() /*-{
      if (!this.initial_working_dir)
         this.initial_working_dir = "~/";
      return this.initial_working_dir;
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
  
   public final native JsObject getProjectUIPrefs() /*-{
      if (!this.project_ui_prefs)
         this.project_ui_prefs = {};
      return this.project_ui_prefs;
   }-*/;

   public final native JsArray<ConsoleProcessInfo> getConsoleProcesses() /*-{
      return this.console_processes;
   }-*/;
   
   public final native boolean isInternalPdfPreviewEnabled() /*-{
      return this.internal_pdf_preview_enabled;
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
   public final static String BUILD_TOOLS_CUSTOM = "Custom";
   
   public final native String getBuildToolsType() /*-{
      return this.build_tools_type;
   }-*/;
   
   public final native String getBuildTargetDir() /*-{
      return this.build_target_dir;
   }-*/;
   
   public final native boolean getHasPackageSrcDir() /*-{
      return this.has_pkg_src;
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
   
   public final native boolean getHaveRcppAttributes() /*-{
      return this.have_rcpp_attributes;
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
   
   public final native boolean getAllowFileDownloads() /*-{
      return this.allow_file_download;
   }-*/;
   
   public final native boolean getAllowRemovePublicFolder() /*-{
      return this.allow_remove_public_folder;
   }-*/;
   
   public final native boolean getShowEnvironmentTab() /*-{
      return this.show_environment_tab;
   }-*/;

   
}
