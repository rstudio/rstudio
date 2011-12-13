/*
 * SVNServerOperations.java
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.server.Void;

import java.util.ArrayList;

public interface SVNServerOperations extends VCSServerOperations
{
   public static class ProcessResult extends JavaScriptObject
   {
      protected ProcessResult() {}

      public final native String getOutput() /*-{
         return this.output;
      }-*/;

      public final native int getExitCode() /*-{
         return this.exit_code;
      }-*/;
   }

   void svnAdd(ArrayList<String> paths,
               ServerRequestCallback<ProcessResult> requestCallback);

   void svnDelete(ArrayList<String> paths,
                  ServerRequestCallback<ProcessResult> requestCallback);

   void svnRevert(ArrayList<String> paths,
                  ServerRequestCallback<ProcessResult> requestCallback);

   void svnStatus(
         ServerRequestCallback<JsArray<StatusAndPathInfo>> requestCallback);

   void svnUpdate(ServerRequestCallback<ConsoleProcess> requestCallback);

   void svnCommit(
         ArrayList<String> paths,
         String message,
         ServerRequestCallback<ConsoleProcess> requestCallback);

   void svnDiffFile(String path,
                    Integer contextLines,
                    boolean noSizeWarning,
                    ServerRequestCallback<String> requestCallback);

   void svnApplyPatch(String path,
                      String patch,
                      ServerRequestCallback<Void> requestCallback);
}
