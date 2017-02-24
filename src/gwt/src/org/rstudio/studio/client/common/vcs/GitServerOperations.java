/*
 * GitServerOperations.java
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

public interface GitServerOperations extends VCSServerOperations
{
   public enum PatchMode
   {
      Working(0),
      Stage(1);

      PatchMode(int intVal)
      {
         intVal_ = intVal;
      }

      public int getValue()
      {
         return intVal_;
      }

      private final int intVal_;
   }

   void gitAdd(ArrayList<String> paths,
               ServerRequestCallback<Void> requestCallback);
   void gitRemove(ArrayList<String> paths,
                  ServerRequestCallback<Void> requestCallback);
   void gitDiscard(ArrayList<String> paths,
                   ServerRequestCallback<Void> requestCallback);
   void gitRevert(ArrayList<String> paths,
                  ServerRequestCallback<Void> requestCallback);
   void gitStage(ArrayList<String> paths,
                 ServerRequestCallback<Void> requestCallback);
   void gitUnstage(ArrayList<String> paths,
                   ServerRequestCallback<Void> requestCallback);

   void gitAllStatus(
         ServerRequestCallback<AllStatus> requestCallback);

   void gitFullStatus(
         ServerRequestCallback<JsArray<StatusAndPathInfo>> requestCallback);

   void gitCreateBranch(String branch,
                        ServerRequestCallback<ConsoleProcess> requestCallback);
   
   void gitListBranches(ServerRequestCallback<BranchesInfo> requestCallback);
   
   void gitListRemotes(ServerRequestCallback<JsArray<RemotesInfo>> requestCallback);
   
   void gitAddRemote(String name,
                     String url,
                     ServerRequestCallback<JsArray<RemotesInfo>> requestCallback);

   void gitCheckout(String id,
                    ServerRequestCallback<ConsoleProcess> requestCallback);
   
   void gitCheckoutRemote(String branch,
                          String remote,
                          ServerRequestCallback<ConsoleProcess> requestCallback);

   void gitCommit(String message,
                  boolean amend,
                  boolean signOff,
                  ServerRequestCallback<ConsoleProcess> requestCallback);

   void gitDiffFile(String path,
                    PatchMode patchMode,
                    int contextLines,
                    boolean noSizeWarning,
                    ServerRequestCallback<DiffResult> requestCallback);

   /**
    * @param patch The patch, in UTF-8 encoding
    * @param mode Whether the patch should be applied to working copy or index
    * @param sourceEncoding The encoding that the patch should be transcoded to
    *    before applying
    * @param requestCallback
    */
   void gitApplyPatch(String patch, PatchMode mode, String sourceEncoding,
                      ServerRequestCallback<Void> requestCallback);

   void gitHistoryCount(String spec, 
                        FileSystemItem fileFilter,
                        String searchText,
                        ServerRequestCallback<CommitCount> requestCallback);
   /**
    * @param spec Revision list or description. "" for default.
    * @param maxentries Limit the number of entries returned. -1 for no limit.
    */
   void gitHistory(String spec,
                   FileSystemItem fileFilter,
                   int skip,
                   int maxentries,
                   String searchText,
                   ServerRequestCallback<RpcObjectList<CommitInfo>> requestCallback);

   void gitShow(String rev,
                boolean noSizeWarning,
                ServerRequestCallback<String> requestCallback);
   
   void gitShowFile(String rev, 
                    String filename,
                    ServerRequestCallback<String> requestCallback);
   
   void gitExportFile(String rev,
                      String filename,
                      String targetPath,
                      ServerRequestCallback<Void> requestCallback);

   void gitPush(ServerRequestCallback<ConsoleProcess> requestCallback);
   
   void gitPushBranch(String branch,
                      String remote,
                      ServerRequestCallback<ConsoleProcess> requestCallback);

   void gitPull(ServerRequestCallback<ConsoleProcess> requestCallback);
   
   void gitSshPublicKey(String privateKeyPath,
                        ServerRequestCallback<String> requestCallback);
    
   void gitHasRepo(String directory,
                   ServerRequestCallback<Boolean> requestCallback);
   
   void gitInitRepo(String directory,
                    ServerRequestCallback<Void> requestCallback);
   
   void gitGetIgnores(String path, 
                      ServerRequestCallback<ProcessResult> requestCallback);

   void gitSetIgnores(String path,
                      String ignores,
                      ServerRequestCallback<ProcessResult> requestCallback);
   
   void gitGithubRemoteUrl(String view, 
                           String path,
                           ServerRequestCallback<String> callback);
}
