/*
 * VCSServerOperations.java
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
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.vcs.dialog.CommitCount;
import org.rstudio.studio.client.workbench.views.vcs.dialog.CommitInfo;

import java.util.ArrayList;

public interface VCSServerOperations extends CryptoServerOperations
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

   void vcsAdd(ArrayList<String> paths,
               ServerRequestCallback<Void> requestCallback);
   void vcsRemove(ArrayList<String> paths,
                  ServerRequestCallback<Void> requestCallback);
   void vcsDiscard(ArrayList<String> paths,
                   ServerRequestCallback<Void> requestCallback);
   void vcsRevert(ArrayList<String> paths,
                  ServerRequestCallback<Void> requestCallback);
   void vcsStage(ArrayList<String> paths,
                 ServerRequestCallback<Void> requestCallback);
   void vcsUnstage(ArrayList<String> paths,
                   ServerRequestCallback<Void> requestCallback);

   void vcsAllStatus(
         ServerRequestCallback<AllStatus> requestCallback);

   void vcsFullStatus(
         ServerRequestCallback<JsArray<StatusAndPath>> requestCallback);

   void vcsListBranches(ServerRequestCallback<BranchesInfo> requestCallback);

   void vcsCheckout(String id,
                    ServerRequestCallback<ConsoleProcess> requestCallback);

   void vcsCommitGit(String message,
                     boolean amend,
                     boolean signOff,
                     ServerRequestCallback<ConsoleProcess> requestCallback);

   void vcsDiffFile(String path,
                    PatchMode patchMode,
                    int contextLines,
                    ServerRequestCallback<String> requestCallback);

   void vcsApplyPatch(String patch, PatchMode mode,
                      ServerRequestCallback<Void> requestCallback);

   void vcsHistoryCount(String spec,
                        ServerRequestCallback<CommitCount> requestCallback);
   /**
    * @param spec Revision list or description. "" for default.
    * @param maxentries Limit the number of entries returned. -1 for no limit.
    */
   void vcsHistory(String spec,
                   int skip,
                   int maxentries,
                   ServerRequestCallback<RpcObjectList<CommitInfo>> requestCallback);

   void vcsExecuteCommand(
         String command,
         ServerRequestCallback<ConsoleProcess> requestCallback);

   void vcsShow(String rev,
                ServerRequestCallback<String> requestCallback);

   void vcsClone(String repoUrl,
                 String parentPath,
                 ServerRequestCallback<ConsoleProcess> requestCallback);

   void vcsPush(ServerRequestCallback<ConsoleProcess> requestCallback);

   void vcsPull(ServerRequestCallback<ConsoleProcess> requestCallback);

   void askpassCompleted(String value,
                         ServerRequestCallback<Void> requestCallback);
}
