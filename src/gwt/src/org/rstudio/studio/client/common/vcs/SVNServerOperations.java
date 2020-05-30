/*
 * SVNServerOperations.java
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
package org.rstudio.studio.client.common.vcs;

import com.google.gwt.core.client.JsArray;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.vcs.dialog.CommitCount;
import org.rstudio.studio.client.workbench.views.vcs.dialog.CommitInfo;

import java.util.ArrayList;

public interface SVNServerOperations extends VCSServerOperations
{
   void svnAdd(ArrayList<String> paths,
               ServerRequestCallback<ProcessResult> requestCallback);

   void svnDelete(ArrayList<String> paths,
                  ServerRequestCallback<ProcessResult> requestCallback);

   void svnRevert(ArrayList<String> paths,
                  ServerRequestCallback<ProcessResult> requestCallback);

   void svnResolve(String accept,
                   ArrayList<String> paths,
                   ServerRequestCallback<ProcessResult> requestCallback);

   void svnStatus(
         ServerRequestCallback<JsArray<StatusAndPathInfo>> requestCallback);

   void svnUpdate(ServerRequestCallback<ConsoleProcess> requestCallback);

   void svnCleanup(ServerRequestCallback<ProcessResult> requestCallback);
   
   void svnCommit(
         ArrayList<String> paths,
         String message,
         ServerRequestCallback<ConsoleProcess> requestCallback);

   void svnDiffFile(String path,
                    Integer contextLines,
                    boolean noSizeWarning,
                    ServerRequestCallback<DiffResult> requestCallback);

   void svnApplyPatch(String path,
                      String patch,
                      String sourceEncoding,
                      ServerRequestCallback<Void> requestCallback);

   /**
    *
    * @param revision Revision number to start at (-1 for latest)
    * @param path The path to show revisions for (null for .)
    * @param searchText
    * @param requestCallback
    */
   void svnHistoryCount(int revision,
                        FileSystemItem path,
                        String searchText,
                        ServerRequestCallback<CommitCount> requestCallback);

   /**
    *
    * @param revision Revision number to start at (-1 for latest)
    * @param path The path to show revisions for (null for .)
    * @param maxentries -1 for no limit
    * @param searchText
    * @param requestCallback
    */
   void svnHistory(int revision,
                   FileSystemItem path,
                   int skip,
                   int maxentries,
                   String searchText,
                   ServerRequestCallback<RpcObjectList<CommitInfo>> requestCallback);

   void svnShow(int revision,
                boolean noSizeWarning,
                ServerRequestCallback<String> requestCallback);

   void svnShowFile(int revision,
                    String filename,
                    ServerRequestCallback<String> requestCallback);
   
   
   void svnGetIgnores(String path, 
                      ServerRequestCallback<ProcessResult> requestCallback);
   
   void svnSetIgnores(String path,
                      String ignores,
                      ServerRequestCallback<ProcessResult> requestCallback);
}
