/*
 * DocUpdateSentinel.java
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
package org.rstudio.studio.client.workbench.views.source.model;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;

import org.rstudio.core.client.Barrier.Token;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.DebouncedCommand;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.patch.SubstringDiff;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.ValueChangeHandlerManager;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerErrorCause;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.events.LastChanceSaveEvent;
import org.rstudio.studio.client.workbench.model.ChangeTracker;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Fold;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.VimMarks;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FoldChangeEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.SourceOnSaveChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkDefinition;
import org.rstudio.studio.client.workbench.views.source.events.SaveFailedEvent;
import org.rstudio.studio.client.workbench.views.source.events.SaveFileEvent;
import org.rstudio.studio.client.workbench.views.source.events.SaveInitiatedEvent;

import java.util.HashMap;
import java.util.Map;

public class DocUpdateSentinel
      implements ValueChangeHandler<Void>,
      FoldChangeEvent.Handler
{
   private class ReopenFileCallback extends ServerRequestCallback<SourceDocument>
   {
      public ReopenFileCallback()
      {
      }

      public ReopenFileCallback(Command onCompleted, boolean ignoreDeletes)
      {
         onCompleted_ = onCompleted;
         ignoreDeletes_ = ignoreDeletes;
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
            if (ignoreDeletes_)
            {
               ServerErrorCause cause = error.getCause();
               if (cause.getCode() != ServerErrorCause.FILE_NOT_FOUND)
                  progress_.onError(error.getUserMessage());
            }
            else
            {
               progress_.onError(error.getUserMessage());
            }
         }

         if (onCompleted_ != null)
            onCompleted_.execute();
      }

      private Command onCompleted_ = null;
      private boolean ignoreDeletes_ = false;
   }

   public DocUpdateSentinel(SourceServerOperations server,
                            DocDisplay docDisplay,
                            SourceDocument sourceDoc,
                            ProgressIndicator progress,
                            DirtyState dirtyState,
                            EventBus events,
                            UserPrefs prefs,
                            ChunkDefinition.Provider chunkDefProvider)
   {
      server_ = server;
      docDisplay_ = docDisplay;
      sourceDoc_ = sourceDoc;
      progress_ = progress;
      dirtyState_ = dirtyState;
      eventBus_ = events;
      prefs_ = prefs;
      chunkDefProvider_ = chunkDefProvider;
      changeTracker_ = docDisplay.getChangeTracker();
      propertyChangeHandlers_ =
            new HashMap<String, ValueChangeHandlerManager<String>>();

      prefs_.autoSaveOnIdle().bind((String behavior) ->
      {
         if (behavior == UserPrefs.AUTO_SAVE_ON_IDLE_BACKUP)
         {
            // Set up the debounced auto-save method when the preference is
            // enabled
            createAutosaver();
         }
         else if (autosaver_ != null)
         {
            // Turn off unsaved change tracking
            autosaver_.suspend();
            autosaver_ = null;
         }
      });
      prefs_.autoSaveIdleMs().bind((Integer ms) ->
      {
         // If we have an auto-saver, re-create it with the new idle timeout
         if (autosaver_ != null)
         {
            autosaver_.suspend();
            autosaver_ = null;
            createAutosaver();
         }
      });

      docDisplay_.addValueChangeHandler(this);
      docDisplay_.addFoldChangeHandler(this);

      // Web only
      if (!Desktop.isDesktop())
      {
         closeHandlerReg_ = Window.addWindowClosingHandler(new ClosingHandler()
         {
            public void onWindowClosing(ClosingEvent event)
            {
               if (changesPending_)
                  event.setMessage("Some of your source edits are still being " +
                        "backed up. If you continue, your latest " +
                        "changes may be lost. Do you want to continue?");
            }
         });
      }

      // Desktop only
      if (Desktop.isDesktop())
      {
         lastChanceSaveHandlerReg_ = events.addHandler(
               LastChanceSaveEvent.TYPE,
               new LastChanceSaveEvent.Handler() {
                  public void onLastChanceSave(LastChanceSaveEvent event)
                  {
                     // We're quitting. Save one last time.
                     final Token token = event.acquire();
                     boolean saving = doSave(null, null, null, true,
                           new ProgressIndicator()
                     {
                        public void onProgress(String message)
                        {
                        }

                        public void onProgress(String message, Operation onCancel)
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
   }

   public void withSavedDoc(final Command onSaved)
   {
      withSavedDocImpl(true, onSaved, null);
   }

   public void withSavedDocNoRetry(final Command onSaved)
   {
      withSavedDocImpl(false, onSaved, null);
   }

   private void withSavedDocImpl(final boolean retryWrite,
                                 final Command onSaved,
         final CommandWithArg<String> onError)
   {
      if (changeTracker_.hasChanged())
      {
         boolean saved = doSave(null, null, null, retryWrite, new ProgressIndicator() {

            @Override
            public void onCompleted()
            {
               if (onSaved != null)
                  onSaved.execute();
            }

            @Override public void onError(String message)
            {
               if (onError != null)
                  onError.execute(message);
            }

            @Override public void onProgress(String message) {}
            @Override public void onProgress(String message, Operation onCancel) {}
            @Override public void clearProgress() {}
         });

         if (!saved)
            onSaved.execute();
      }
      else
      {
         onSaved.execute();
      }
   }
   private boolean maybeAutoSave()
   {
      if (changeTracker_.hasChanged())
      {
         return doSave(null, null, null, false, new ProgressIndicator()
         {

            @Override
            public void onProgress(String message, Operation onCancel)
            {
               if (progress_ != null)
                  progress_.onProgress(message, onCancel);
            }

            @Override
            public void onProgress(String message)
            {
               if (progress_ != null)
                  progress_.onProgress(message);

            }

            @Override
            public void onError(String message)
            {
               // Inform the user only once if this was an autosave failure.
               if (!loggedAutosaveError_)
               {
                  // do not show the error if it is a transient autosave related issue - this can occur fairly frequently
                  // when attempting to save files that are being backed up by external software
                  if (!message.contains("The process cannot access the file because it is being used by another process"))
                  {
                     loggedAutosaveError_ = true;

                     RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
                           "Error Autosaving File",
                           "RStudio was unable to autosave this file. You may need " +
                                 "to restart RStudio.");
                  }
               }

               // Use regular completed callback for indicator.
               if (progress_ != null)
                  progress_.onCompleted();
            }

            @Override
            public void onCompleted()
            {
               if (progress_ != null)
                  progress_.onCompleted();
            }

            @Override
            public void clearProgress()
            {
               if (progress_ != null)
                  progress_.clearProgress();
            }
         });
      }
      else
      {
         changesPending_ = false;
         return false;
      }
   }

   public void changeFileType(String fileType, final ProgressIndicator progress)
   {
      saveWithSuspendedAutoSave(null, fileType, null, true, progress);
   }

   public void save(String path,
                    // fileType==null means don't change value
                    String fileType,
                    // encoding==null means don't change value
                    String encoding,
                    boolean retryWrite,
                    final ProgressIndicator progress)
   {
      assert path != null;
      if (path == null)
         throw new IllegalArgumentException("Path cannot be null");
      saveWithSuspendedAutoSave(path, fileType, encoding, retryWrite, progress);
   }

   private void saveWithSuspendedAutoSave(String path,
                                          String fileType,
                                          String encoding,
                                          boolean retryWrite,
                                          final ProgressIndicator progress)
   {
      if (autosaver_ != null)
         autosaver_.suspend();

      doSave(path, fileType, encoding, retryWrite, new ProgressIndicator()
      {
         public void onProgress(String message)
         {
            onProgress(message, null);
         }

         public void onProgress(String message, Operation onCancel)
         {
            if (progress != null)
               progress.onProgress(message, onCancel);
         }

         public void clearProgress()
         {
            if (autosaver_ != null)
               autosaver_.resume();
            if (progress != null)
               progress.clearProgress();
         }

         public void onCompleted()
         {
            if (autosaver_!= null)
               autosaver_.resume();
            if (progress != null)
               progress.onCompleted();
         }

         public void onError(String message)
         {
            if (autosaver_ != null)
               autosaver_.resume();
            if (progress != null)
               progress.onError(message);
         }
      });
   }

   private boolean doSave(String path,
                          String fileType,
                          String encoding,
                          boolean retryWrite,
                          ProgressIndicator progress)
   {
      boolean didSave = false;
      try
      {
         didSave = doSaveImpl(path, fileType, encoding, retryWrite, progress);
      }
      catch (Exception ex)
      {
         // log the exception that caused the problem (not all progress
         // indicators, even if present, will report it)
         Debug.log("Exception occurred during save: ");
         Debug.logException(ex);

         // report error to progress indicator if present
         if (progress != null)
         {
            progress.onError("Could not save " + path + ": " +
                             ex.getMessage());
         }
      }

      // Update marks after document save
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            if (docDisplay_.isVimModeOn())
            {
               String oldMarksSpec = "";
               if (hasProperty("marks"))
                  oldMarksSpec = getProperty("marks");

               String newMarksSpec = VimMarks.encode(docDisplay_.getMarks());
               if (oldMarksSpec != newMarksSpec)
                  setProperty("marks", newMarksSpec);
            }
         }
      });

      return didSave;
   }

   private boolean doSaveImpl(final String path,
                              final String fileType,
                              final String encoding,
                              final boolean retryWrite,
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

      final JsArray<ChunkDefinition> newChunkDefs = chunkDefProvider_.getChunkDefs();
      JsArray<ChunkDefinition> oldChunkDefs =
            sourceDoc_.getNotebookDoc().getChunkDefs();

      SubstringDiff diff = new SubstringDiff(oldContents, newContents);

      // Don't auto-save when there are no changes. In addition to being
      // wasteful, it causes the server to think the document is dirty.
      if (path == null && fileType == null && diff.isValid() && diff.isEmpty()
          && foldSpec == oldFoldSpec
          && (newChunkDefs == null ||
              ChunkDefinition.equalTo(newChunkDefs, oldChunkDefs)))
      {
         changesPending_ = false;
         return false;
      }

      if (path == null && fileType == null
          && oldContents.length() == 0
          && newContents == "\n")
      {
         // This is necessary due to us adding an extra \n to empty
         // documents, which we have to do or else CodeMirror starts
         // acting funny. If we add the extra \n but don't do this
         // check, then reloading the browser causes empty documents
         // to appear dirty.
         changesPending_ = false;
         return false;
      }


      try
      {
         if (path != null)
         {
            // notify that a save is underway (for collaborative editing)
            eventBus_.fireEvent(new SaveInitiatedEvent(path, getId()));
         }
      }
      catch(Exception e)
      {
         Debug.logException(e);
      }

      server_.saveDocumentDiff(
            sourceDoc_.getId(),
            path,
            fileType,
            encoding,
            foldSpec,
            newChunkDefs,
            diff.getReplacement(),
            diff.getOffset(),
            diff.getLength(),
            diff.isValid(),
            hash,
            retryWrite,
            new ServerRequestCallback<String>()
            {
               @Override
               public void onError(ServerError error)
               {
                  // Always log save errors.
                  Debug.logError(error);

                  // Report errors to indicator.
                  if (progress != null)
                  {
                     String errorMessage =
                           "Error saving " + path + ": " +
                                 error.getUserMessage();

                     progress.onError(errorMessage);
                  }

                  // Attempt to report save error.
                  try
                  {
                     if (path != null)
                     {
                        eventBus_.fireEvent(new SaveFailedEvent(path, getId()));
                     }
                  }
                  catch (Exception e)
                  {
                     Debug.logException(e);
                  }

                  changesPending_ = false;
               }

               @Override
               public void onResponseReceived(String newHash)
               {
                  if (newHash != null)
                  {
                     // If the document hasn't changed further since the version
                     // we saved, then we know we're all synced up.
                     try
                     {
                        if (!thisChangeTracker.hasChanged())
                           changeTracker_.reset();

                        // update the foldSpec and newChunkDefs so we
                        // can use them for change detection the next
                        // time around
                        sourceDoc_.setFoldSpec(foldSpec);
                        sourceDoc_.getNotebookDoc().setChunkDefs(newChunkDefs);

                        onSuccessfulUpdate(newContents,
                                           newHash,
                                           path,
                                           fileType,
                                           encoding);
                     }
                     catch(Exception ex)
                     {
                        // log exception, but continue (we want to guarantee the
                        // progress indicator is updated)
                        Debug.log("Exception in post-save update " + path +
                                  " to " + newHash + ": " + ex.getMessage());
                     }
                     if (progress != null)
                        progress.onCompleted();

                     // let anyone interested know we just saved
                     SaveFileEvent saveEvent = new SaveFileEvent(path, fileType, encoding);
                     docDisplay_.fireEvent(saveEvent);
                     eventBus_.fireEvent(saveEvent);
                  }
                  else if (hash != sourceDoc_.getHash())
                  {
                     // We just hit a race condition where two updates
                     // happened at once. Try again
                     doSave(path, fileType, encoding, retryWrite, progress);
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
                           newChunkDefs,
                           newContents,
                           retryWrite,
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

   public boolean hasProperty(String propertyName)
   {
      return sourceDoc_.getProperties().hasKey(propertyName);
   }

   public String getProperty(String propertyName)
   {
      JsObject properties = sourceDoc_.getProperties();
      return properties.getString(propertyName);
   }

   public String getProperty(String propertyName, String defaultValue)
   {
      if (hasProperty(propertyName))
         return getProperty(propertyName);
      return defaultValue;
   }

   public boolean getBoolProperty(String propertyName, boolean defaultValue)
   {
      if (hasProperty(propertyName))
         return getProperty(propertyName) == PROPERTY_TRUE;
      return defaultValue;
   }

   public void setBoolProperty(String propertyName, boolean value)
   {
      setProperty(propertyName, value ? PROPERTY_TRUE : PROPERTY_FALSE);
   }

   public void setProperty(String name,
                           String value,
                           ProgressIndicator progress)
   {
      HashMap<String, String> props = new HashMap<String, String>();
      props.put(name, value);
      modifyProperties(props, progress);
   }

   public void setProperty(String name, String value)
   {
      setProperty(name, value, null);
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

   public void modifyProperties(final HashMap<String, String> properties)
   {
      modifyProperties(properties, null);
   }

   public HandlerRegistration addPropertyValueChangeHandler(
         final String propertyName,
         final ValueChangeHandler<String> handler)
   {
      if (!propertyChangeHandlers_.containsKey(propertyName))
            propertyChangeHandlers_.put(
                  propertyName, new ValueChangeHandlerManager<String>(this));
      return propertyChangeHandlers_.get(propertyName).addValueChangeHandler(
            handler);
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
         if (propertyChangeHandlers_.containsKey(entry.getKey()))
         {
            ValueChangeEvent.fire(
                  propertyChangeHandlers_.get(entry.getKey()),
                  entry.getValue());
         }
      }
   }

   public void withChangeDetectionSuspended(Command command)
   {
      try
      {
         suspendDetectChanges_ += 1;
         command.execute();
      }
      finally
      {
         suspendDetectChanges_ -= 1;
      }
   }

   public void onValueChange(ValueChangeEvent<Void> voidValueChangeEvent)
   {
      nudgeAutosave();
   }

   @Override
   public void onFoldChange(FoldChangeEvent event)
   {
      nudgeAutosave();
   }
   
   public void nudgeAutosave()
   {
      if (suspendDetectChanges_ > 0)
         return;

      changesPending_ = true;
      if (autosaver_ != null)
         autosaver_.nudge();
   }

   public String getPath()
   {
      return sourceDoc_.getPath();
   }

   public String getContents()
   {
      return sourceDoc_.getContents();
   }

   public SourceDocument getDoc()
   {
      return sourceDoc_;
   }

   public void stop()
   {
      if (autosaver_ != null)
         autosaver_.suspend();

      if (closeHandlerReg_ != null)
      {
         closeHandlerReg_.removeHandler();
         closeHandlerReg_ = null;
      }

      if (lastChanceSaveHandlerReg_ != null)
      {
         lastChanceSaveHandlerReg_.removeHandler();
         lastChanceSaveHandlerReg_ = null;
      }
   }

   public void revert()
   {
      revert(null, false);
   }

   public void revert(Command onCompleted, boolean ignoreDeletes)
   {
      server_.revertDocument(
            sourceDoc_.getId(),
            sourceDoc_.getType(),
            new ReopenFileCallback(onCompleted, ignoreDeletes));
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

   public String getId()
   {
      return sourceDoc_.getId();
   }

   private void createAutosaver()
   {
      if (autosaver_ == null)
      {
         autosaver_ = new DebouncedCommand(prefs_.autoSaveMs())
         {
            @Override
            protected void execute()
            {
               maybeAutoSave();
            }
         };
      }
   }

   private int suspendDetectChanges_ = 0;
   private boolean changesPending_ = false;
   private final ChangeTracker changeTracker_;
   private final SourceServerOperations server_;
   private final DocDisplay docDisplay_;
   private SourceDocument sourceDoc_;
   private final ProgressIndicator progress_;
   private final DirtyState dirtyState_;
   private final EventBus eventBus_;
   private DebouncedCommand autosaver_;
   private final UserPrefs prefs_;
   private HandlerRegistration closeHandlerReg_;
   private HandlerRegistration lastChanceSaveHandlerReg_;
   private final HashMap<String, ValueChangeHandlerManager<String>>
                 propertyChangeHandlers_;
   private final ChunkDefinition.Provider chunkDefProvider_;
   private boolean loggedAutosaveError_ = false;

   public final static String PROPERTY_TRUE = "true";
   public final static String PROPERTY_FALSE = "false";


}
