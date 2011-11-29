/*
 * GitServerOperations.java
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
package org.rstudio.studio.client.common.vcs;

import com.google.gwt.core.client.JsArray;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.crypto.CryptoServerOperations;
import org.rstudio.studio.client.projects.model.VcsCloneOptions;
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.vcs.dialog.CommitCount;
import org.rstudio.studio.client.workbench.views.vcs.dialog.CommitInfo;

import java.util.ArrayList;

public interface GitServerOperations extends CryptoServerOperations
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

   void gitListBranches(ServerRequestCallback<BranchesInfo> requestCallback);

   void gitCheckout(String id,
                    ServerRequestCallback<ConsoleProcess> requestCallback);

   void gitCommit(String message,
                  boolean amend,
                  boolean signOff,
                  ServerRequestCallback<ConsoleProcess> requestCallback);

   void gitDiffFile(String path,
                    PatchMode patchMode,
                    int contextLines,
                    boolean noSizeWarning,
                    ServerRequestCallback<String> requestCallback);

   void gitApplyPatch(String patch, PatchMode mode,
                      ServerRequestCallback<Void> requestCallback);

   void gitHistoryCount(String spec, String filter,
                        ServerRequestCallback<CommitCount> requestCallback);
   /**
    * @param spec Revision list or description. "" for default.
    * @param maxentries Limit the number of entries returned. -1 for no limit.
    */
   void gitHistory(String spec,
                   int skip,
                   int maxentries,
                   String filter,
                   ServerRequestCallback<RpcObjectList<CommitInfo>> requestCallback);

   void gitExecuteCommand(
         String command,
         ServerRequestCallback<ConsoleProcess> requestCallback);

   void gitShow(String rev,
                boolean noSizeWarning,
                ServerRequestCallback<String> requestCallback);

   void gitClone(VcsCloneOptions options,
                 ServerRequestCallback<ConsoleProcess> requestCallback);

   void gitPush(ServerRequestCallback<ConsoleProcess> requestCallback);

   void gitPull(ServerRequestCallback<ConsoleProcess> requestCallback);

   void askpassCompleted(String value, boolean remember,
                         ServerRequestCallback<Void> requestCallback);
   
   void gitSshPublicKey(String privateKeyPath,
                        ServerRequestCallback<String> requestCallback);
    
   void gitHasRepo(String directory,
                   ServerRequestCallback<Boolean> requestCallback);
   
   void gitInitRepo(String directory,
                    ServerRequestCallback<Void> requestCallback);
}
