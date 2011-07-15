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
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.vcs.history.CommitInfo;

import java.util.ArrayList;

public interface VCSServerOperations
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

   void vcsFullStatus(
         ServerRequestCallback<JsArray<StatusAndPath>> requestCallback);

   void vcsCommitGit(String message,
                     boolean amend,
                     boolean signOff,
                     ServerRequestCallback<Void> requestCallback);

   void vcsDiffFile(String path,
                    PatchMode patchMode,
                    int contextLines,
                    ServerRequestCallback<String> requestCallback);

   void vcsApplyPatch(String patch, PatchMode mode,
                      ServerRequestCallback<Void> requestCallback);

   void vcsHistory(String spec,
                   ServerRequestCallback<RpcObjectList<CommitInfo>> requestCallback);

   void vcsExecuteCommand(
         String command,
         ServerRequestCallback<ExecuteCommandResult> requestCallback);
}
