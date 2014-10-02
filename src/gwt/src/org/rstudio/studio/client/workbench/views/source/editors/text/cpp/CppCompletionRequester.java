/*
 * CppCompletionRequester.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text.cpp;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.model.CppCompletionResult;
import org.rstudio.studio.client.workbench.views.source.model.CppServerOperations;


public class CppCompletionRequester
{
   public CppCompletionRequester(CppServerOperations server)
   {
      server_ = server;
   }
   
   public void getCompletions(
         String docPath,
         String docContents,
         boolean docIsDirty,
         DocDisplay docDisplay,
         Position position,
         final boolean implicit,
         final ServerRequestCallback<CppCompletionResult> requestCallback)
   {
      server_.getCppCompletions(docPath, 
                                docContents, 
                                docIsDirty, 
                                position.getRow() + 1, 
                                position.getColumn() + 1, 
                                requestCallback);
   }
   
   
   public void flushCache()
   {
   }
   
   
   private final CppServerOperations server_;
}
