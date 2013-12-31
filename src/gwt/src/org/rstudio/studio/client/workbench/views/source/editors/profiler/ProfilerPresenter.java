/*
 * ProfilerPresenter.java
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
package org.rstudio.studio.client.workbench.views.source.editors.profiler;

import java.util.HashMap;

import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfilerContents;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfilerServerOperations;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
 
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.HasValue;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ProfilerPresenter
{ 
   public interface Display
   {
      HasValue<Integer> getPropA();
      HasValue<Boolean> getPropB();
   }

   @Inject
   public ProfilerPresenter(ProfilerServerOperations server,
                            Binder binder,
                            Commands commands)
   {
      server_ = server;
      commands_ = commands;
      binder.bind(commands, this);
      
      // default profiler commands to disabled until we are attached
      // to a document and view
      disableAllCommands();
   }
   
   public void attatch(SourceDocument doc, Display view)
   {
      // save references to doc and view
      doc_ = doc;
      view_ = view;
      
      // initialize view
      ProfilerContents contents = getContents();
      view_.getPropA().setValue(contents.getPropA());
      view_.getPropB().setValue(contents.getPropB());
      
      // subscribe to value changes on the view to save contents 
      // to the server whenenver it's modfied
      handlerRegistrations_.add(view_.getPropA()
                  .addValueChangeHandler(new ValueChangeHandler<Integer>() {
         @Override
         public void onValueChange(ValueChangeEvent<Integer> event)
         {
            contentsUpdater_.nudge();           
         }
         
      }));
      
      handlerRegistrations_.add(view_.getPropB()
                 .addValueChangeHandler(new ValueChangeHandler<Boolean>() {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            contentsUpdater_.nudge();
         }
      }));
      
      // enable commands for stopped state
      enableStoppedCommands();
   }
   
   public void detach()
   {
      // unsubscribe from view
      handlerRegistrations_.removeHandler();
      
      // null out references to doc and view
      doc_ = null;
      view_ = null;
      
      // disable all commands
      disableAllCommands();
   }
   
   @Handler
   public void onStartProfiler()
   {
     
      // manage commands
      enableStartedCommands();
   }
   
   @Handler
   public void onStopProfiler()
   {
     
      
      // manage commands
      enableStoppedCommands();
   }
   
   private void disableAllCommands()
   {
      commands_.startProfiler().setEnabled(false);
      commands_.stopProfiler().setEnabled(false);
   }
   
   
   private void enableStartedCommands()
   {
      commands_.startProfiler().setEnabled(false);
      commands_.stopProfiler().setEnabled(true);
   }
   private void enableStoppedCommands()
   {
      commands_.startProfiler().setEnabled(true);
      commands_.stopProfiler().setEnabled(false);   
   }
   
   // create a time buffered command for updating profiler contents (ensures
   // that we save no more frequently than every 100ms even in the face of
   // many changes over a short time)
   private TimeBufferedCommand contentsUpdater_ = new TimeBufferedCommand(100) {

      @Override
      protected void performAction(boolean shouldSchedulePassive)
      {         
         // tab might have been closed in the meantime, check for this first
         if (doc_ == null || view_ == null)
            return;
         
         // update document properties if they've changed
         ProfilerContents contents = ProfilerContents.create(
                                           view_.getPropA().getValue(),
                                           view_.getPropB().getValue());
         if (!contents.equalTo(getContents()))
         {
            HashMap<String, String> props = new HashMap<String, String>();
            contents.fillProperties(props);
            server_.modifyDocumentProperties(doc_.getId(),
                                             props,
                                             new VoidServerRequestCallback());
         }  
      }
   };
   
   // typed access to underlying document properties
   private ProfilerContents getContents()
   {
      return (ProfilerContents)doc_.getProperties().cast();
   }
   
   
   private SourceDocument doc_ = null;
   private Display view_ = null;
   private final ProfilerServerOperations server_;
   private final Commands commands_;
   private final HandlerRegistrations handlerRegistrations_ = 
                                             new HandlerRegistrations();
   
   public interface Binder extends CommandBinder<Commands, ProfilerPresenter> {}
}
