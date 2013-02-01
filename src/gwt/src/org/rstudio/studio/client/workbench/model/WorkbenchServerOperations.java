/*
 * WorkbenchServerOperations.java
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

import org.rstudio.studio.client.common.compilepdf.model.CompilePdfServerOperations;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.crypto.CryptoServerOperations;
import org.rstudio.studio.client.common.mirrors.model.MirrorsServerOperations;
import org.rstudio.studio.client.common.spelling.model.SpellingServerOperations;
import org.rstudio.studio.client.common.synctex.model.SynctexServerOperations;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.common.vcs.SVNServerOperations;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.PrefsServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.views.buildtools.model.BuildServerOperations;
import org.rstudio.studio.client.workbench.views.choosefile.model.ChooseFileServerOperations;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleServerOperations;
import org.rstudio.studio.client.workbench.views.data.model.DataServerOperations;
import org.rstudio.studio.client.workbench.views.edit.model.EditServerOperations;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.output.find.model.FindInFilesServerOperations;
import org.rstudio.studio.client.workbench.views.help.model.HelpServerOperations;
import org.rstudio.studio.client.workbench.views.history.model.HistoryServerOperations;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationServerOperations;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;
import org.rstudio.studio.client.workbench.views.workspace.model.WorkspaceServerOperations;

public interface WorkbenchServerOperations extends ConsoleServerOperations,
                                                   FilesServerOperations,
                                                   PackagesServerOperations,
                                                   HelpServerOperations,
                                                   WorkspaceServerOperations,
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
                                                   CodeSearchServerOperations,
                                                   CryptoServerOperations,
                                                   WorkbenchListsServerOperations,
                                                   SpellingServerOperations,
                                                   CompilePdfServerOperations,
                                                   FindInFilesServerOperations,
                                                   SynctexServerOperations,
                                                   BuildServerOperations,
                                                   PresentationServerOperations
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
   
   
   void startShellDialog(ServerRequestCallback<ConsoleProcess> requestCallback);
}
