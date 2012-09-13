/*
 * DocUpdateSentinel.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.model;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import org.rstudio.core.client.Barrier.Token;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.patch.SubstringDiff;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.events.LastChanceSaveEvent;
import org.rstudio.studio.client.workbench.events.LastChanceSaveHandler;
import org.rstudio.studio.client.workbench.model.ChangeTracker;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Fold;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FoldChangeEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.SourceOnSaveChangedEvent;

import java.util.HashMap;
import java.util.Map;

public class DocUpdateSentinel
      implements ValueChangeHandler<Void>,FoldChangeEvent.Handler
{
   private class ReopenFileCallback extends ServerRequestCallback<SourceDocument>
   {
      public ReopenFileCallback()
      {
      }
      
      public ReopenFileCallback(Command onCompleted)
      {
         onCompleted_ = onCompleted;
      }
      
      @Override
      public void onResponseReceived(
            SourceDocument response)
      {
         sourceDoc_ = response;
         docDisplay_.setCode(sourceDoc_.getContents(), true);
         dirtyState_.markClean();

         if (progress_ != null)
            progress_.onCompleted();
         
         if (onCompleted_ != null)
            onCompleted_.execute();
      }

      @Override
      public void onError(ServerError error)
      {
         if (progress_ != null)
         {
            progress_.onError(error.getUserMessage());
         }
         
         if (onCompleted_ != null)
            onCompleted_.execute();
      }
      
      private Command onCompleted_ = null;
   }

   public DocUpdateSentinel(SourceServerOperations server,
                            DocDisplay docDisplay,
                            SourceDocument sourceDoc,
                            ProgressIndicator progress,
                            DirtyState dirtyState,
                            EventBus events)
   {
      server_ = server;
      docDisplay_ = docDisplay;
      sourceDoc_ = sourceDoc;
      progress_ = progress;
      dirtyState_ = dirtyState;
      eventBus_ = events;
      changeTracker_ = docDisplay.getChangeTracker();

      bufferedCommand_ = new TimeBufferedCommand(2000)
      {
         @Override
         protected void performAction(boolean shouldSchedulePassive)
         {
            assert !shouldSchedulePassive;
            maybeAutoSave();
         }
      };

      docDisplay_.addValueChangeHandler(this);
      docDisplay_.addFoldChangeHandler(this);

      // Web only
      closeHandlerReg_ = Window.addWindowClosingHandler(new ClosingHandler()
      {
         public void onWindowClosing(ClosingEvent event)
         {
            if (changesPending_)
               event.setMessage("Some of your source edits are still being " +
                                "synchronized with the server. If you " +
                                "continue, your latest changes may be lost.");
         }
      });

      // Desktop only
      lastChanceSaveHandlerReg_ = events.addHandler(
            LastChanceSaveEvent.TYPE,
            new LastChanceSaveHandler() {
               public void onLastChanceSave(LastChanceSaveEvent event)
               {
                  // We're quitting. Save one last time.

                  final Token token = event.acquire();
                  boolean saving = doSave(null, null, null,
                                          new ProgressIndicator()
                  {
                     public void onProgress(String message)
                     {
                     }
                     
                     public void clearProgress()
                     {
                        // alternate way to signal completion. safe to quit
                        token.release();
                     }

                     public void onCompleted()
                     {
                        // We saved successfully. We're safe to quit now.
                        token.release();
                     }

                     public void onError(String message)
                     {
                        // The save didn't succeed. Oh well. Nothing we can
                        // do but quit.
                        token.release();
                     }
                  });

                  if (!saving)
                  {
                     // No save was performed (not needed). We're safe to quit
                     // now, no need to wait for server requests to complete.
                     token.release();
                  }
               }
            });
   }

   private boolean maybeAutoSave()
   {
      if (changeTracker_.hasChanged())
      {
         return doSave(null, null, null, progress_);
      }
      else
      {
         changesPending_ = false;
         return false;
      }
   }

   public void changeFileType(String fileType, final ProgressIndicator progress)
   {
      saveWithSuspendedAutoSave(null, fileType, null, progress);
   }

   public void save(String path,
                    // fileType==null means don't change value
                    String fileType,
                    // encoding==null means don't change value
                    String encoding,
                    final ProgressIndicator progress)
   {
      assert path != null;
      if (path == null)
         throw new IllegalArgumentException("Path cannot be null");
      saveWithSuspendedAutoSave(path, fileType, encoding, progress);
   }

   private void saveWithSuspendedAutoSave(String path,
                                          String fileType,
                                          String encoding,
                                          final ProgressIndicator progress)
   {
      bufferedCommand_.suspend();
      doSave(path, fileType, encoding, new ProgressIndicator()
      {
         public void onProgress(String message)
         {
            if (progress != null)
               progress.onProgress(message);
         }
         
         public void clearProgress()
         {
            bufferedCommand_.resume();
            if (progress != null)
               progress.clearProgress();
         }

         public void onCompleted()
         {
            bufferedCommand_.resume();
            if (progress != null)
               progress.onCompleted();
         }

         public void onError(String message)
         {
            bufferedCommand_.resume();
            if (progress != null)
               progress.onError(message);
         }
      });
   }

   private boolean doSave(final String path,
                          final String fileType,
                          final String encoding,
                          final ProgressIndicator progress)
   {
      /* We need to fork the change tracker so that we can "mark" the moment
         in history when we took the contents from the source doc, so that
         if the document is edited while the save is in progress we don't
         reset the true change tracker's state to a version we haven't
         actually sent to the server. */
      final ChangeTracker thisChangeTracker = changeTracker_.fork();

      final String newContents = docDisplay_.getCode();
      String oldContents = sourceDoc_.getContents();
      final String hash = sourceDoc_.getHash();

      final String foldSpec = Fold.encode(Fold.flatten(docDisplay_.getFolds()));
      String oldFoldSpec = sourceDoc_.getFoldSpec();

      //String patch = DiffMatchPatch.diff(oldContents, newContents);
      SubstringDiff diff = new SubstringDiff(oldContents, newContents);

      // Don't auto-save when there are no changes. In addition to being
      // wasteful, it causes the server to think the document is dirty.
      if (path == null && fileType == null && diff.isEmpty()
          && foldSpec.equals(oldFoldSpec))
      {
         changesPending_ = false;
         return false;
      }

      if (path == null && fileType == null
          && oldContents.length() == 0
          && newContents.equals("\n"))
      {
         // This is necessary due to us adding an extra \n to empty
         // documents, which we have to do or else CodeMirror starts
         // acting funny. If we add the extra \n but don't do this
         // check, then reloading the browser causes empty documents
         // to appear dirty.
         changesPending_ = false;
         return false;
      }

      server_.saveDocumentDiff(
            sourceDoc_.getId(),
            path,
            fileType,
            encoding,
            foldSpec,
            diff.getReplacement(),
            diff.getOffset(),
            diff.getLength(),
            hash,
            new ServerRequestCallback<String>()
            {
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
                  if (progress != null)
                     progress.onError(error.getUserMessage());
                  changesPending_ = false;
               }

               @Override
               public void onResponseReceived(String newHash)
               {
                  if (newHash != null)
                  {
                     // If the document hasn't changed further since the version
                     // we saved, then we know we're all synced up.
                     if (!thisChangeTracker.hasChanged())
                        changeTracker_.reset();

                     onSuccessfulUpdate(newContents,
                                        newHash,
                                        path,
                                        fileType,
                                        encoding);
                     if (progress != null)
                        progress.onCompleted();
                  }
                  else if (!hash.equals(sourceDoc_.getHash()))
                  {
                     // We just hit a race condition where two updates
                     // happened at once. Try again
                     doSave(path, fileType, encoding, progress);
                  }
                  else
                  {
                     /*Debug.log("Diff-based save failed--falling back to " +
                               "snapshot save");*/
                     server_.saveDocument(
                           sourceDoc_.getId(),
                           path,
                           fileType,
                           encoding,
                           foldSpec,
                           newContents,
                           this);
                  }
               }
            });

      return true;
   }

   private void onSuccessfulUpdate(String contents,
                                   String hash,
                                   String path,
                                   String fileType,
                                   String encoding)
   {
      changesPending_ = false;
      sourceDoc_.setContents(contents);
      sourceDoc_.setHash(hash);
      if (path != null)
      {
         sourceDoc_.setDirty(false);
         sourceDoc_.setPath(path);
      }
      if (fileType != null)
      {
         sourceDoc_.setType(fileType);
      }
      if (encoding != null)
         sourceDoc_.setEncoding(encoding);
   }

   public boolean sourceOnSave()
   {
      return sourceDoc_.sourceOnSave();
   }

   public void setSourceOnSave(final boolean shouldSourceOnSave,
                               final ProgressIndicator progress)
   {
      if (sourceDoc_.sourceOnSave() == shouldSourceOnSave)
         return;
      
      server_.setSourceDocumentOnSave(
            sourceDoc_.getId(),
            shouldSourceOnSave,
            new ServerRequestCallback<Void>()
            {
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
                  if (progress != null)
                     progress.onError(error.getUserMessage());
               }

               @Override
               public void onResponseReceived(Void response)
               {
                  sourceDoc_.setSourceOnSave(shouldSourceOnSave);
                  
                  eventBus_.fireEvent(new SourceOnSaveChangedEvent());
                  
                  if (progress != null)
                     progress.onCompleted();
               }
            });
   }

   public String getProperty(String propertyName)
   {
      JsObject properties = sourceDoc_.getProperties();
      return properties.getString(propertyName);
   }

   public void setProperty(String name,
                           String value,
                           ProgressIndicator progress)
   {
      HashMap<String, String> props = new HashMap<String, String>();
      props.put(name, value);
      modifyProperties(props, progress);
   }

   /**
    * Applies the values in the given HashMap to the document's property bag.
    * This does NOT replace all of the doc's properties on the server; any
    * properties that already exist but are not present in the HashMap, are
    * left unchanged. If a HashMap entry has a null value, that property
    * should be removed.
    */
   public void modifyProperties(final HashMap<String, String> properties,
                                final ProgressIndicator progress)
   {
      server_.modifyDocumentProperties(
            sourceDoc_.getId(),
            properties,
            new ServerRequestCallback<Void>()
            {
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
                  if (progress != null)
                     progress.onError(error.getUserMessage());
               }

               @Override
               public void onResponseReceived(Void response)
               {
                  applyProperties(sourceDoc_.getProperties(), properties);
                  if (progress != null)
                     progress.onCompleted();
               }
            });
   }

   private void applyProperties(JsObject properties,
                                HashMap<String, String> newProperties)
   {
      for (Map.Entry<String, String> entry : newProperties.entrySet())
      {
         if (entry.getValue() == null)
            properties.unset(entry.getKey());
         else
            properties.setString(entry.getKey(), entry.getValue());
      }
   }

   public void onValueChange(ValueChangeEvent<Void> voidValueChangeEvent)
   {
      changesPending_ = true;
      bufferedCommand_.nudge();
   }

   @Override
   public void onFoldChange(FoldChangeEvent event)
   {
      changesPending_ = true;
      bufferedCommand_.nudge();
   }

   public String getPath()
   {
      return sourceDoc_.getPath();
   }

   public void stop()
   {
      bufferedCommand_.suspend();
      closeHandlerReg_.removeHandler();
      lastChanceSaveHandlerReg_.removeHandler();
   }

   public void revert()
   {
      revert(null);
   }
   
   public void revert(Command onCompleted)
   {
      server_.revertDocument(
            sourceDoc_.getId(),
            sourceDoc_.getType(),
            new ReopenFileCallback(onCompleted));
   }

   public void reopenWithEncoding(String encoding)
   {
      server_.reopenWithEncoding(
            sourceDoc_.getId(),
            encoding,
            new ReopenFileCallback());
   }

   public void ignoreExternalEdit()
   {
      // Warning: This leaves the sourceDoc_ with a stale LastModifiedDate
      // but we don't use it.
      server_.ignoreExternalEdit(sourceDoc_.getId(),
                                 new SimpleRequestCallback<Void>());
   }

   public String getEncoding()
   {
      return sourceDoc_.getEncoding();
   }

   public boolean isAscii()
   {
      String code = docDisplay_.getCode();
      for (int i = 0; i < code.length(); i++)
         if (code.charAt(i) >= 128)
            return false;
      return true;
   }

   private boolean changesPending_ = false;
   private final ChangeTracker changeTracker_;
   private final SourceServerOperations server_;
   private final DocDisplay docDisplay_;
   private SourceDocument sourceDoc_;
   private final ProgressIndicator progress_;
   private final DirtyState dirtyState_;
   private final EventBus eventBus_;
   private final TimeBufferedCommand bufferedCommand_;
   private final HandlerRegistration closeHandlerReg_;
   private HandlerRegistration lastChanceSaveHandlerReg_;
}
