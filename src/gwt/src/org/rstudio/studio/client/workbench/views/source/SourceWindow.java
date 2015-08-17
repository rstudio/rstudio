/*
 * SourceWindow.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source;

import java.util.ArrayList;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.application.ApplicationQuit;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.DesktopHooks;
import org.rstudio.studio.client.application.MacZoomHandler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.SaveAction;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.UnsavedChangesItem;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.snippets.SnippetServerOperations;
import org.rstudio.studio.client.workbench.snippets.model.SnippetData;
import org.rstudio.studio.client.workbench.snippets.model.SnippetsChangedEvent;
import org.rstudio.studio.client.workbench.views.source.events.DocTabDragStartedEvent;
import org.rstudio.studio.client.workbench.views.source.events.LastSourceDocClosedEvent;
import org.rstudio.studio.client.workbench.views.source.events.LastSourceDocClosedHandler;
import org.rstudio.studio.client.workbench.views.source.events.PopoutDocEvent;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class SourceWindow implements LastSourceDocClosedHandler,
                                     PopoutDocEvent.Handler,
                                     DocTabDragStartedEvent.Handler
{
   @Inject
   public SourceWindow(
         Provider<DesktopHooks> pDesktopHooks,
         Satellite satellite,
         EventBus events,
         MacZoomHandler zoomHandler,
         SourceShim shim,
         SnippetServerOperations snippetServer)
   {
      sourceShim_ = shim;
      events_ = events;
      satellite_ = satellite;
      
      // this class is for satellite source windows only; if an instance gets
      // created in the main window, don't hook up any of its behaviors
      if (!Satellite.isCurrentWindowSatellite())
         return;
      
      // add event handlers
      events.addHandler(LastSourceDocClosedEvent.TYPE, this);
      events.addHandler(PopoutDocEvent.TYPE, this);
      events.addHandler(DocTabDragStartedEvent.TYPE, this);

      // set up desktop hooks (required to e.g. process commands issued by 
      // the desktop frame)
      pDesktopHooks.get();
      
      // export callbacks for main window
      exportFromSatellite();
      
      // load custom snippets into this window
      snippetServer.getSnippets(new ServerRequestCallback<JsArray<SnippetData>>()
      {
         @Override
         public void onResponseReceived(JsArray<SnippetData> snippets)
         {
            if (snippets != null && snippets.length() > 0)
            {
               events_.fireEvent(new SnippetsChangedEvent(
                     (SnippetsChangedEvent.Data)snippets));
            }
         }

         @Override
         public void onError(ServerError error)
         {
            // log this error, but don't bother the user with it--their own
            // snippets may not work but any real errors should be handled in
            // the main window
            Debug.logError(error);
         }
      });
      
      // in desktop mode, the frame checks to see if we want to be closed, but
      // in web mode the best we can do is prompt if the user attempts to close
      // a source window with unsaved chaanges.
      if (!Desktop.isDesktop())
      {
         Window.addWindowClosingHandler(new ClosingHandler() {
            @Override
            public void onWindowClosing(ClosingEvent event)
            {
               // ignore window closure if initiated from the main window
               if (satellite_.isClosePending())
               {
                  markReadyToClose();
                  return;
               }
               
               ArrayList<UnsavedChangesTarget> unsaved = 
                     sourceShim_.getUnsavedChanges();

               if (unsaved.size() > 0)
               {
                  String msg = "Your edits to the ";
                  if (unsaved.size() == 1)
                  {
                     msg += "file " + FilePathUtils.friendlyFileName(
                           unsaved.get(0).getPath());
                  }
                  else
                  {
                     msg += "files ";
                     for (int i = 0; i < unsaved.size(); i++)
                     {
                        msg += FilePathUtils.friendlyFileName(
                           unsaved.get(i).getPath());
                        if (i == unsaved.size() - 2)
                           msg += " and ";
                        else if (i < unsaved.size() - 2)
                           msg += ", ";
                     }
                  }
                  msg += " have not been saved.";
                  event.setMessage(msg);
               }
            }
         });
      }
   }
   
   public void setInitialDoc(String docId, SourcePosition sourcePosition)
   {
      initialDocId_ = docId;
      initialSourcePosition_ = sourcePosition;
   }
   
   public String getInitialDocId()
   {
      return initialDocId_;
   }
   
   public SourcePosition getInitialSourcePosition()
   {
      return initialSourcePosition_;
   }
   
   // Event handlers ----------------------------------------------------------

   @Override
   public void onLastSourceDocClosed(LastSourceDocClosedEvent event)
   {
      // if this is a source document window and its last document closed,
      // close the window itself
      markReadyToClose();
      WindowEx.get().close();
   }
   

   @Override
   public void onPopoutDoc(PopoutDocEvent event)
   {
      // can't pop out directly from a satellite to another satellite; fire
      // this one on the main window
      events_.fireEventToMainWindow(event);
   }

   @Override
   public void onDocTabDragStarted(DocTabDragStartedEvent event)
   {
      if (!event.isFromMainWindow())
      {
         // if this is a satellite, broadcast the event to the main window
         events_.fireEventToMainWindow(event);
      }
   }

   // Private methods ---------------------------------------------------------

   private final native void exportFromSatellite() /*-{
      var satellite = this;
      $wnd.rstudioCloseAllDocs = $entry(function(caption, onCompleted) {
         satellite.@org.rstudio.studio.client.workbench.views.source.SourceWindow::closeAllDocs(Ljava/lang/String;Lcom/google/gwt/user/client/Command;)(caption, onCompleted);
      });
      
      $wnd.rstudioGetUnsavedChanges = $entry(function() {
         return satellite.@org.rstudio.studio.client.workbench.views.source.SourceWindow::getUnsavedChanges()();
      });
      
      $wnd.rstudioHandleUnsavedChangesBeforeExit = $entry(function(targets, onCompleted) {
         satellite.@org.rstudio.studio.client.workbench.views.source.SourceWindow::handleUnsavedChangesBeforeExit(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/user/client/Command;)(targets, onCompleted);
      });
      
      $wnd.rstudioSaveWithPrompt = $entry(function(target, onCompleted) {
         satellite.@org.rstudio.studio.client.workbench.views.source.SourceWindow::saveWithPrompt(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/user/client/Command;)(target, onCompleted);
      });
      
      $wnd.rstudioSaveAllUnsaved = $entry(function(onCompleted) {
         satellite.@org.rstudio.studio.client.workbench.views.source.SourceWindow::saveAllUnsaved(Lcom/google/gwt/user/client/Command;)(onCompleted);
      });
      
      $wnd.rstudioReadyToClose = false;
      $wnd.rstudioCloseSourceWindow = $entry(function() {
         satellite.@org.rstudio.studio.client.workbench.views.source.SourceWindow::closeSourceWindow()();
      });
   }-*/;
   
   private void saveWithPrompt(JavaScriptObject jsoItem, Command onCompleted)
   {
      UnsavedChangesItem item = jsoItem.cast();
      sourceShim_.saveWithPrompt(item, onCompleted, null);
   }
   
   private void saveAllUnsaved(Command onCompleted)
   {
      sourceShim_.saveAllUnsaved(onCompleted);
   }
   
   private void handleUnsavedChangesBeforeExit(JavaScriptObject jsoItems, 
         Command onCompleted)
   {
      JsArray<UnsavedChangesItem> items = jsoItems.cast();
      ArrayList<UnsavedChangesTarget> targets = 
            new ArrayList<UnsavedChangesTarget>();
      for (int i = 0; i < items.length(); i++)
      {
         targets.add(items.get(i));
      }
      sourceShim_.handleUnsavedChangesBeforeExit(targets, onCompleted);
   }
   
   private void closeAllDocs(String caption, Command onCompleted)
   {
      sourceShim_.closeAllSourceDocs(caption, onCompleted);
   }
   
   private JsArray<UnsavedChangesItem> getUnsavedChanges()
   {
      JsArray<UnsavedChangesItem> items = JsArray.createArray().cast();
      ArrayList<UnsavedChangesTarget> targets = sourceShim_.getUnsavedChanges();
      for (UnsavedChangesTarget target: targets)
      {
         items.push(UnsavedChangesItem.create(target));
      }
      return items;
   }
   
   private void closeSourceWindow()
   {
      ApplicationQuit.QuitContext quitContext = 
            new ApplicationQuit.QuitContext()
            {
               @Override
               public void onReadyToQuit(boolean saveChanges)
               {
                  markReadyToClose();
                  
                  // we may be in the middle of closing the window already, so
                  // defer the closure request
                  Scheduler.get().scheduleDeferred(
                        new Scheduler.ScheduledCommand()
                  {
                     @Override
                     public void execute()
                     {
                        WindowEx.get().close();
                     }
                  });
               }
            };

      // prompt to save any unsaved documents
      if (sourceShim_.getUnsavedChanges().size() == 0)
         quitContext.onReadyToQuit(false);
      else
         ApplicationQuit.handleUnsavedChanges(SaveAction.SAVEASK, 
               "Close Source Window", sourceShim_, null, null, quitContext);
   }
   
   private final native void markReadyToClose() /*-{
      $wnd.rstudioReadyToClose = true;
   }-*/;
   
   private final EventBus events_;
   private final SourceShim sourceShim_;
   private final Satellite satellite_;
   private String initialDocId_;
   private SourcePosition initialSourcePosition_;
}
