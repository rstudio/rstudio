/*
 * WorkbenchServerOperations.java
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
package org.rstudio.studio.client.workbench.model;

import com.google.gwt.core.client.JavaScriptObject;

import org.rstudio.studio.client.common.mirrors.model.MirrorsServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.views.choosefile.model.ChooseFileServerOperations;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleServerOperations;
import org.rstudio.studio.client.workbench.views.data.model.DataServerOperations;
import org.rstudio.studio.client.workbench.views.edit.model.EditServerOperations;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.help.model.HelpServerOperations;
import org.rstudio.studio.client.workbench.views.history.model.HistoryServerOperations;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
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
                                                   MirrorsServerOperations
{   
   void onWorkbenchReady();
   
   void setWorkbenchMetrics(WorkbenchMetrics clientMetrics,
                            ServerRequestCallback<Void> requestCallback);
   
   void setPrefs(RPrefs rPrefs,
                 JavaScriptObject uiPrefs,
                 ServerRequestCallback<Void> requestCallback);
   
   void setUiPrefs(JavaScriptObject uiPrefs,
                   ServerRequestCallback<Void> requestCallback);

   void getRPrefs(ServerRequestCallback<RPrefs> requestCallback);
 
   void updateClientState(JavaScriptObject temporary,
                          JavaScriptObject persistent,
                          ServerRequestCallback<Void> requestCallback);
   
    
   void userPromptCompleted(int response, 
                            ServerRequestCallback<Void> requestCallback);
}
