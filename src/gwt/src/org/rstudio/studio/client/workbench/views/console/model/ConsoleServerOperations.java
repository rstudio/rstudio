/*
 * ConsoleServerOperations.java
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
package org.rstudio.studio.client.workbench.views.console.model;

import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.shell.ShellInput;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.history.model.HistoryServerOperations;

public interface ConsoleServerOperations extends CodeToolsServerOperations,
                                                 HistoryServerOperations
{
   // interrupt the current session
   void interrupt(ServerRequestCallback<Void> requestCallback);

   // send console input
   void consoleInput(String consoleInput, 
                     String consoleId,
                     ServerRequestCallback<Void> requestCallback);
   
   void resetConsoleActions(ServerRequestCallback<Void> requestCallback);

   void processStart(String handle,
                     ServerRequestCallback<Void> requestCallback);

   void processInterrupt(String handle,
                         ServerRequestCallback<Void> requestCallback);
   
   void processReap(String handle,
                    ServerRequestCallback<Void> requestCallback);

   void processWriteStdin(String handle,
                          ShellInput input,
                          ServerRequestCallback<Void> requestCallback);
   
   void processSetShellSize(String handle,
                            int width,
                            int height,
                            ServerRequestCallback<Void> requestCallback);

   void processEraseBuffer(String handle,
                           ServerRequestCallback<Void> requestCallback);

   void processGetBufferChunk(String handle,
                              int chunk,
                              ServerRequestCallback<ProcessBufferChunk> requestCallback);
   
   /**
    * Switch a server process to use Rpc mode after failing to connect to
    * non-rpc channel such as websocket.
    * @param handle process handle
    * @param requestCallback
    */
   void processUseRpc(String handle,
                      ServerRequestCallback<Void> requestCallback);

   /**
    * Set/update the caption of given process.
    * @param handle process handle
    * @param caption new caption
    * @param requestCallback response
    */
   void processSetCaption(String handle,
                          String caption,
                          ServerRequestCallback<Void> requestCallback);
   /**
    * Set/update the title of given process.
    * @param handle process handle
    * @param title new title
    * @param requestCallback response
    */
   void processSetTitle(String handle,
                        String title,
                        ServerRequestCallback<Void> requestCallback);
}
