/*
 * VisualModeChunkExec.java
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


package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.panmirror.PanmirrorRmdChunk;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIExecute;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetRMarkdownHelper;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

// provide chunk execution service to panmirror

public class VisualModeChunkExec
{
   public VisualModeChunkExec(DocUpdateSentinel docUpdateSentinel,
                              TextEditingTargetRMarkdownHelper rmarkdownHelper,
                              VisualModeEditorSync sync)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      docUpdateSentinel_ = docUpdateSentinel;
      rmarkdownHelper_ = rmarkdownHelper;
      sync_ = sync;
   }
   
   
   @Inject
   void initialize(EventBus events)
   {
      events_ = events;
   }
   
   public PanmirrorUIExecute uiExecute()
   {
      PanmirrorUIExecute uiExecute = new PanmirrorUIExecute();
      uiExecute.executeRmdChunk = (chunk) -> {
         executeRmdChunk(chunk);
      };
      return uiExecute;
   }
   
   private void executeRmdChunk(PanmirrorRmdChunk chunk)
   { 
      // ignore null chunk
      if (chunk == null)
         return;
      
      // see if this is for a supported language (bail if not)
      String chunkLang = null;
      for (String lang : kRmdChunkExecutionLangs) 
      {
         if (chunk.lang.equalsIgnoreCase(lang)) 
         {
            chunkLang = lang;
            break;
         }
      }
      if (chunkLang == null)
         return;
      
      // execute the chunk
      final String finalChunkLang = chunkLang;
      sync_.syncToEditor(false, () -> {
         // ensure source is synced with server
         docUpdateSentinel_.withSavedDoc(new Command() {
            @Override
            public void execute()
            {
               // allow server to prepare for chunk execution
               // (e.g. by populating 'params' in the global environment)
               rmarkdownHelper_.prepareForRmdChunkExecution(
                  docUpdateSentinel_.getId(),
                  docUpdateSentinel_.getContents(), 
                  () -> {
                     events_.fireEvent(new SendToConsoleEvent(chunk.code, 
                                                              finalChunkLang, 
                                                              true));
                  }
               );
            }
         });  
      }); 
   }
   
   public final static String[] kRmdChunkExecutionLangs = new String[] { "R", "Python" };
   
   private final DocUpdateSentinel docUpdateSentinel_;
   private final VisualModeEditorSync sync_;
   
   private final TextEditingTargetRMarkdownHelper rmarkdownHelper_;
   
   private EventBus events_;
   
}
