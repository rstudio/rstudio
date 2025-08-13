/*
 * WorkbenchServerOperations.java
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
package org.rstudio.studio.client.workbench.model;

import org.rstudio.studio.client.common.compilepdf.model.CompilePdfServerOperations;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.common.debugging.DebuggingServerOperations;
import org.rstudio.studio.client.common.dependencies.model.DependencyServerOperations;
import org.rstudio.studio.client.common.mirrors.model.MirrorsServerOperations;
import org.rstudio.studio.client.common.r.roxygen.RoxygenServerOperations;
import org.rstudio.studio.client.common.repos.model.SecondaryReposServerOperations;
import org.rstudio.studio.client.common.spelling.model.SpellingServerOperations;
import org.rstudio.studio.client.common.synctex.model.SynctexServerOperations;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.common.vcs.SVNServerOperations;
import org.rstudio.studio.client.panmirror.server.PanmirrorServerOperations;
import org.rstudio.studio.client.projects.model.ProjectTemplateServerOperations;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.renv.model.RenvServerOperations;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.addins.AddinsServerOperations;
import org.rstudio.studio.client.workbench.prefs.views.PythonServerOperations;
import org.rstudio.studio.client.workbench.snippets.SnippetServerOperations;
import org.rstudio.studio.client.workbench.views.choosefile.model.ChooseFileServerOperations;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionsServerOperations;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleServerOperations;
import org.rstudio.studio.client.workbench.views.edit.model.EditServerOperations;
import org.rstudio.studio.client.workbench.views.environment.dataimport.model.DataImportServerOperations;
import org.rstudio.studio.client.workbench.views.environment.model.EnvironmentServerOperations;
import org.rstudio.studio.client.workbench.views.jobs.model.JobsServerOperations;
import org.rstudio.studio.client.workbench.views.output.find.model.FindInFilesServerOperations;
import org.rstudio.studio.client.workbench.views.output.markers.model.MarkersServerOperations;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.presentation2.model.Presentation2ServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfilerServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.model.ThemeServerOperations;
import org.rstudio.studio.client.workbench.views.terminal.TerminalShellInfo;
import org.rstudio.studio.client.workbench.views.tutorial.TutorialServerOperations;
import org.rstudio.studio.client.workbench.views.viewer.model.ViewerServerOperations;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public interface WorkbenchServerOperations extends ConsoleServerOperations,
                                                   PackagesServerOperations,
                                                   PlotsServerOperations,
                                                   EditServerOperations,
                                                   ChooseFileServerOperations,
                                                   MirrorsServerOperations,
                                                   GitServerOperations,
                                                   SVNServerOperations,
                                                   ProjectsServerOperations,
                                                   ProjectTemplateServerOperations,
                                                   WorkbenchListsServerOperations,
                                                   SpellingServerOperations,
                                                   CompilePdfServerOperations,
                                                   FindInFilesServerOperations,
                                                   SynctexServerOperations,
                                                   EnvironmentServerOperations,
                                                   DebuggingServerOperations,
                                                   MetaServerOperations,
                                                   ViewerServerOperations,
                                                   ProfilerServerOperations,
                                                   RMarkdownServerOperations,
                                                   PanmirrorServerOperations,
                                                   DependencyServerOperations,
                                                   RenvServerOperations,
                                                   MarkersServerOperations,
                                                   RoxygenServerOperations,
                                                   SnippetServerOperations,
                                                   AddinsServerOperations,
                                                   DataImportServerOperations,
                                                   ConnectionsServerOperations,
                                                   JobsServerOperations,
                                                   SecondaryReposServerOperations,
                                                   ThemeServerOperations,
                                                   TutorialServerOperations,
                                                   PythonServerOperations,
                                                   Presentation2ServerOperations
{   
   void initializeForMainWorkbench();
   void setAuthorized();
   void setUnauthorized();
   void disconnect();
   
   void setWorkbenchMetrics(WorkbenchMetrics clientMetrics,
                            ServerRequestCallback<Void> requestCallback);
   
   void updateClientState(JavaScriptObject temporary,
                          JavaScriptObject persistent,
                          JavaScriptObject projectPersistent,
                          ServerRequestCallback<Void> requestCallback);
   
    
   void userPromptCompleted(int response, 
                            ServerRequestCallback<Void> requestCallback);
   
   void adminNotificationAcknowledged(String id, ServerRequestCallback<Void> requestCallback);
   
   void openFileDialogCompleted(String path, ServerRequestCallback<Void> requestCallback);
   
   void getTerminalOptions(
                     ServerRequestCallback<TerminalOptions> requestCallback);
  
   void getTerminalShells(
         ServerRequestCallback<JsArray<TerminalShellInfo>> requestCallback);

   /**
    * Start a terminal session
    * 
    * <p>On the client, the "caption" is displayed in the terminal tab, and
    * defaults to something like "Terminal 1", and can be changed by the user.</p>
    * 
    * <p>The "title" is set in response to xterm escape sequences, and is shown 
    * above the terminal pane (e.g. show current working directory via bash's
    * PROMPT_COMMAND feature.</p>
    * 
    * @param cpi terminal metadata
    * @param requestCallback callback from server upon completion
    */
   void startTerminal(ConsoleProcessInfo cpi,
                      ServerRequestCallback<ConsoleProcess> requestCallback);
   
   void executeCode(String code, ServerRequestCallback<Void> requestCallback);
}
