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

import java.util.ArrayList;

import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.model.CppServerOperations;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

public class CppCompletionRequester
{
   public CppCompletionRequester(CppServerOperations server)
   {
      server_ = server;
   }
   
   public void getCompletions(
         final String line, 
         final int pos,
         final boolean implicit,
         final ServerRequestCallback<CppCompletionResult> requestCallback)
   {
      ArrayList<String> completions = new ArrayList<String>();
      completions.add("Lenny");
      completions.add("Moe");
      completions.add("Curly");
      
      final CppCompletionResult result = CppCompletionResult.create(
                                       JsUtil.toJsArrayString(completions));
      
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {

         @Override
         public void execute()
         {
            requestCallback.onResponseReceived(result);   
         }  
      });
   }
   
   
   public void flushCache()
   {
   }
   
   
   @SuppressWarnings("unused")
   private final CppServerOperations server_;
}
