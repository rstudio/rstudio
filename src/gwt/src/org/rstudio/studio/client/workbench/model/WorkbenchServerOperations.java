/*
 * WorkbenchServerOperations.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

import org.rstudio.studio.client.common.compilepdf.model.CompilePdfServerOperations;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.crypto.CryptoServerOperations;
import org.rstudio.studio.client.common.debugging.DebuggingServerOperations;
import org.rstudio.studio.client.common.dependencies.model.DependencyServerOperations;
import org.rstudio.studio.client.common.mirrors.model.MirrorsServerOperations;
import org.rstudio.studio.client.common.r.roxygen.RoxygenServerOperations;
import org.rstudio.studio.client.common.spelling.model.SpellingServerOperations;
import org.rstudio.studio.client.common.synctex.model.SynctexServerOperations;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.common.vcs.SVNServerOperations;
import org.rstudio.studio.client.packrat.model.PackratServerOperations;
import org.rstudio.studio.client.projects.model.ProjectTemplateServerOperations;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.addins.AddinsServerOperations;
import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.PrefsServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.snippets.SnippetServerOperations;
import org.rstudio.studio.client.workbench.views.buildtools.model.BuildServerOperations;
import org.rstudio.studio.client.workbench.views.choosefile.model.ChooseFileServerOperations;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionsServerOperations;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleServerOperations;
import org.rstudio.studio.client.workbench.views.data.model.DataServerOperations;
import org.rstudio.studio.client.workbench.views.edit.model.EditServerOperations;
import org.rstudio.studio.client.workbench.views.environment.dataimport.model.DataImportServerOperations;
import org.rstudio.studio.client.workbench.views.environment.model.EnvironmentServerOperations;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.help.model.HelpServerOperations;
import org.rstudio.studio.client.workbench.views.history.model.HistoryServerOperations;
import org.rstudio.studio.client.workbench.views.output.find.model.FindInFilesServerOperations;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintServerOperations;
import org.rstudio.studio.client.workbench.views.output.markers.model.MarkersServerOperations;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfilerServerOperations;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;
import org.rstudio.studio.client.workbench.views.terminal.TerminalShellInfo;
import org.rstudio.studio.client.workbench.views.viewer.model.ViewerServerOperations;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public interface WorkbenchServerOperations extends ConsoleServerOperations,
                                                   FilesServerOperations,
                                                   PackagesServerOperations,
                                                   HelpServerOperations,
                                                   PlotsServerOperations,
                                                   EditServerOperations,
                                                   SourceServerOperations,
                                                   DataServerOperations,
                                                   ChooseFileServerOperations,
                                                   HistoryServerOperations,
                                                   MirrorsServerOperations,
                                                   GitServerOperations,
                                                   SVNServerOperations,
                                                   PrefsServerOperations,
                                                   ProjectsServerOperations,
                                                   ProjectTemplateServerOperations,
                                                   CodeSearchServerOperations,
                                                   CryptoServerOperations,
                                                   WorkbenchListsServerOperations,
                                                   SpellingServerOperations,
                                                   CompilePdfServerOperations,
                                                   FindInFilesServerOperations,
                                                   SynctexServerOperations,
                                                   BuildServerOperations,
                                                   PresentationServerOperations,
                                                   EnvironmentServerOperations,
                                                   DebuggingServerOperations,
                                                   MetaServerOperations,
                                                   ViewerServerOperations,
                                                   ProfilerServerOperations,
                                                   RMarkdownServerOperations,
                                                   DependencyServerOperations,
                                                   PackratServerOperations,
                                                   MarkersServerOperations,
                                                   LintServerOperations,
                                                   RoxygenServerOperations,
                                                   SnippetServerOperations,
                                                   AddinsServerOperations,
                                                   DataImportServerOperations,
                                                   ConnectionsServerOperations
{   
   void initializeForMainWorkbench();
   void disconnect();
   
   void setWorkbenchMetrics(WorkbenchMetrics clientMetrics,
                            ServerRequestCallback<Void> requestCallback);
   
   void getRPrefs(ServerRequestCallback<RPrefs> requestCallback);
 
   void updateClientState(JavaScriptObject temporary,
                          JavaScriptObject persistent,
                          JavaScriptObject projectPersistent,
                          ServerRequestCallback<Void> requestCallback);
   
    
   void userPromptCompleted(int response, 
                            ServerRequestCallback<Void> requestCallback);
   
   void getTerminalOptions(
                     ServerRequestCallback<TerminalOptions> requestCallback);
  
   void getTerminalShells(
         ServerRequestCallback<JsArray<TerminalShellInfo>> requestCallback);

   /**
    * Start a terminal session
    * 
    * <p> Supports both a "caption" and a "title", and both are persisted on the
    * server.</p>
    * 
    * <p>On the client, the "caption" is displayed in the terminal tab, and
    * defaults to something like "Terminal 1", and can be changed by the user.</p>
    * 
    * <p>The "title" is set in response to xterm escape sequences, and is shown 
    * above the terminal pane (e.g. show current working directory via bash's
    * PROMPT_COMMAND feature.</p>
    * 
    * @param shellType one of TerminalShellInfo SHELL_* enum values
    * @param cols initial number of text columns for pseudoterminal
    * @param rows initial number of text rows for pseudoterminal
    * @param handle initial terminal handle (pass empty or null string for new terminal)
    * @param caption caption associated with the terminal
    * @param title title associated with the terminal
    * @param websocket try to connect via WebSocket
    * @param sequence relative order of terminal creation (1-based)
    * @param requestCallback callback from server upon completion
    */
   void startTerminal(int shellType, int cols, int rows, String handle, 
                      String caption, String title, boolean websocket, int sequence, 
                      ServerRequestCallback<ConsoleProcess> requestCallback);
   
   void executeCode(String code, ServerRequestCallback<Void> requestCallback);
}
