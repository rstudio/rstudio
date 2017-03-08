/*
 * ProfilerEditingTarget.java
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.EnsureHeightHandler;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.ReadOnlyValue;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.common.filetypes.FileIconResources;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.ProfilerType;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.rsconnect.model.PublishHtmlSource;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfileOperationResponse;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfilerContents;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfilerServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.events.CollabEditStartParams;
import org.rstudio.studio.client.workbench.views.source.events.DocWindowChangedEvent;
import org.rstudio.studio.client.workbench.views.source.events.PopoutDocEvent;
import org.rstudio.studio.client.workbench.views.source.events.SourceNavigationEvent;
import org.rstudio.studio.client.workbench.views.source.model.DocTabDragParams;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourceNavigation;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import java.util.HashMap;
import java.util.HashSet;

public class ProfilerEditingTarget implements EditingTarget,
                                              HasSelectionCommitHandlers<CodeNavigationTarget>
{
   interface MyCommandBinder
   extends CommandBinder<Commands, ProfilerEditingTarget>
   {
   }
   
   private static final MyCommandBinder commandBinder =
         GWT.create(MyCommandBinder.class);
      
   @Inject
   public ProfilerEditingTarget(ProfilerPresenter presenter,
                                Commands commands,
                                EventBus events,
                                ProfilerServerOperations server,
                                GlobalDisplay globalDisplay,
                                Provider<SourceWindowManager> pSourceWindowManager,
                                FileDialogs fileDialogs,
                                RemoteFileSystemContext fileContext,
                                WorkbenchContext workbenchContext,
                                EventBus eventBus,
                                SourceServerOperations sourceServer,
                                FileTypeRegistry fileTypeRegistry)
   {
      presenter_ = presenter;
      commands_ = commands;
      events_ = events;
      server_ = server;
      globalDisplay_ = globalDisplay;
      pSourceWindowManager_ = pSourceWindowManager;
      fileDialogs_ = fileDialogs;
      fileContext_ = fileContext;
      workbenchContext_ = workbenchContext;
      eventBus_ = eventBus;
      sourceServer_ = sourceServer;
      fileTypeRegistry_ = fileTypeRegistry;
      
      if (!initializedEvents_)
      {
         initializedEvents_ = true;
         initializeEvents();
      }
   }

   public String getId()
   {
      return doc_.getId();
   }

   @Override
   public void adaptToExtendedFileType(String extendedType)
   {
   }

   @Override
   public String getExtendedFileType()
   {
      return null;
   }

   public HasValue<String> getName()
   {
      return name_;
   }

   public String getTitle()
   {
      return name_.getValue();
   }

   public String getContext()
   {
      return null;
   }

   public ImageResource getIcon()
   {
      return new ImageResource2x(FileIconResources.INSTANCE.iconProfiler2x());
   }

   @Override
   public TextFileType getTextFileType()
   {
      return null;
   }

   public String getTabTooltip()
   {
      return "R Profiler";
   }

   public HashSet<AppCommand> getSupportedCommands()
   {
      HashSet<AppCommand> commands = fileType_.getSupportedCommands(commands_);
      if (SourceWindowManager.isMainSourceWindow())
         commands.add(commands_.popoutDoc());
      else
         commands.add(commands_.returnDocToMain());
      commands.add(commands_.printSourceDoc());
      return commands;
   }
   
   @Override
   public void manageCommands()
   {
   }
   
   @Override
   public boolean canCompilePdf()
   {
      return false;
   }
   
   @Override
   public void verifyCppPrerequisites()
   {
   }

   @Override
   public Position search(String regex)
   {
      return null;
   }

   @Override
   public Position search(Position startPos, String regex)
   {
      return null;
   }

   @Override
   public void forceLineHighlighting()
   {
   }

   public void focus()
   {
   }

   public void onActivate()
   {
      activeProfilerEditingTarger_ = this;
      
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         public void execute()
         {
            commands_.gotoProfileSource().setEnabled(hasValidPath_);
         }
      });
      
      final Operation activateOperation = new Operation()
      {
         
         @Override
         public void execute()
         {
            if (!htmlPathInitialized_) {
               htmlPathInitialized_ = true;
               
               htmlPath_ = getContents().getHtmlPath();
               htmlLocalPath_ = getContents().getHtmlLocalPath();
               isUserSaved_ = getContents().isUserSaved();
               
               if (htmlPath_ == null)
               {
                  presenter_.buildHtmlPath(new OperationWithInput<ProfileOperationResponse>()
                  {
                     @Override
                     public void execute(ProfileOperationResponse response)
                     {
                        htmlPath_ = response.getHtmlPath();
                        htmlLocalPath_ = response.getHtmlLocalPath();
                        
                        persistDocumentProperty("htmlPath", htmlPath_);
                        persistDocumentProperty("htmlLocalPath", htmlLocalPath_);
                        
                        view_.showProfilePage(htmlPath_);
                        
                        pSourceWindowManager_.get().maximizeSourcePaneIfNecessary();
                     }
                     
                  }, new Operation()
                  {
                     @Override
                     public void execute()
                     {
                        server_.clearProfile(getPath(), new ServerRequestCallback<JavaScriptObject>()
                        {
                           @Override
                           public void onResponseReceived(JavaScriptObject response)
                           {
                              commands_.closeSourceDoc().execute();
                           }
                           
                           @Override
                           public void onError(ServerError error)
                           {
                              Debug.logError(error);
                              commands_.closeSourceDoc().execute();
                           }
                        });
                     }
                  }, getPath());
               }
               else
               {
                  view_.showProfilePage(htmlPath_);
      
                  Scheduler.get().scheduleDeferred(new ScheduledCommand()
                  {
                     public void execute()
                     {
                        pSourceWindowManager_.get().maximizeSourcePaneIfNecessary();
                     }
                  });
               }
            }
         }
      };
      
      if (getId() != null && !SourceWindowManager.isMainSourceWindow()) {
         sourceServer_.getSourceDocument(getId(), new ServerRequestCallback<SourceDocument>()
         {
            @Override
            public void onResponseReceived(SourceDocument document)
            {
               doc_ = document;
               activateOperation.execute();
            }
            
            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
            }
         });
      }
      else {
         activateOperation.execute();
      }
      
      // If we're already hooked up for some reason, unhook. 
      // This shouldn't happen though.
      if (commandHandlerReg_ != null)
      {
         Debug.log("Warning: onActivate called twice without intervening onDeactivate");
         commandHandlerReg_.removeHandler();
         commandHandlerReg_ = null;
      }
      commandHandlerReg_ = commandBinder.bind(commands_, this);
   }

   public void onDeactivate()
   {
      if (activeProfilerEditingTarger_ == this)
      {
         activeProfilerEditingTarger_ = null;
      }
      
      recordCurrentNavigationPosition();
      
      commandHandlerReg_.removeHandler();
      commandHandlerReg_ = null;
   }

   @Override
   public void onInitiallyLoaded()
   {
   }

   @Override
   public void recordCurrentNavigationPosition()
   {
      events_.fireEvent(new SourceNavigationEvent(
            SourceNavigation.create(
            getId(), 
            getPath(), 
            SourcePosition.create(0, 0))));
   }

   @Override
   public void navigateToPosition(SourcePosition position,
                                  boolean recordCurrent)
   {
   }
   
   @Override
   public void navigateToPosition(SourcePosition position,
                                  boolean recordCurrent,
                                  boolean highlightLine)
   {
   }

   @Override
   public void restorePosition(SourcePosition position)
   {
   }

   @Override
   public SourcePosition currentPosition()
   {
      return null;
   }

   @Override
   public void setCursorPosition(Position position)
   {
   }

   @Override
   public void ensureCursorVisible()
   {
   }

   @Override
   public boolean isAtSourceRow(SourcePosition position)
   {
      // always true because profiler docs don't have the
      // concept of a position
      return true;
   }

   @Override
   public void highlightDebugLocation(SourcePosition startPos,
                                      SourcePosition endPos,
                                      boolean executing)
   {
   }

   @Override
   public void endDebugHighlighting()
   {
   }

   @Override
   public void beginCollabSession(CollabEditStartParams params)
   {
   }

   @Override
   public void endCollabSession()
   {
   }

   public boolean onBeforeDismiss()
   {
      return true;
   }

   public ReadOnlyValue<Boolean> dirtyState()
   {
      return neverDirtyState_;
   }

   @Override
   public boolean isSaveCommandActive()
   {
      return !isUserSaved_;
   }

   @Override
   public void forceSaveCommandActive()
   {
   }

   public void save(Command onCompleted)
   {
      onCompleted.execute();
   }

   public void saveWithPrompt(Command onCompleted, Command onCancelled)
   {
      onCompleted.execute();
   }

   public void revertChanges(Command onCompleted)
   {
      onCompleted.execute();
   }
   
   @Handler
   void onPrintSourceDoc()
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         public void execute()
         {
            view_.print();
         }
      });
   }
   
   private String getAndSetInitialName()
   {
      String name = getContents().getName();
      boolean createProfile = getContents().getCreateProfile();
      
      if (!StringUtil.isNullOrEmpty(name)) {
         return name;
      }
      else if (createProfile) {
         String defaultName = defaultNameProvider_.get();
         persistDocumentProperty("name", defaultName);
         return defaultName;
      }
      else {
         String nameFromFile = FileSystemItem.getNameFromPath(getPath());
         persistDocumentProperty("name", nameFromFile);
         return nameFromFile;
      }
   }

   public void initialize(SourceDocument document,
                          FileSystemContext fileContext,
                          FileType type,
                          Provider<String> defaultNameProvider)
   {
      // initialize doc, view, and presenter
      doc_ = document;
      
      PublishHtmlSource publishHtmlSource = new PublishHtmlSource() {

         @Override
         public void generatePublishHtml(CommandWithArg<String> onComplete)
         {
            onComplete.execute(htmlLocalPath_) ;
         }

         @Override
         public String getTitle()
         {
            return "Profile";
         }
      };
      
      view_ = new ProfilerEditingTargetWidget(commands_, publishHtmlSource);
      defaultNameProvider_ = defaultNameProvider;
      
      getName().setValue(getAndSetInitialName());
      
      presenter_.attach(doc_, view_);
   }

   public void onDismiss(int dismissType)
   {
      presenter_.detach();
   }

   public long getFileSizeLimit()
   {
      return Long.MAX_VALUE;
   }

   public long getLargeFileSize()
   {
      return Long.MAX_VALUE;
   }

   public Widget asWidget()
   {
      return view_.asWidget();
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return new HandlerRegistration()
      {
         public void removeHandler()
         {
         }
      };
   }

   public HandlerRegistration addEnsureHeightHandler(EnsureHeightHandler handler)
   {
      return new HandlerRegistration()
      {
         public void removeHandler()
         {
         }
      };
   }

   @Override
   public HandlerRegistration addCloseHandler(CloseHandler<java.lang.Void> handler)
   {
      return new HandlerRegistration()
      {
         public void removeHandler()
         {
         }
      };
   }

   public void fireEvent(GwtEvent<?> event)
   {
      assert false : "Not implemented";
   }

   public String getPath()
   {
      return getContents().getPath();
   }
   
   @Override
   public HandlerRegistration addSelectionCommitHandler(SelectionCommitHandler<CodeNavigationTarget> handler)
   {
      return null;
   }
   
   @Handler
   void onSaveSourceDoc()
   {
      saveNewFile(null);
   }

   @Handler
   void onSaveSourceDocAs()
   {
      saveNewFile(isUserSaved_ ? getPath() : null);
   }
   
   @Handler
   public void onGotoProfileSource()
   {
      FilePosition filePosition = FilePosition.create(selectedLine_, 0);
      fileTypeRegistry_.editFile(FileSystemItem.createFile(selectedPath_),
                                 filePosition);
   }

   public String getDefaultNamePrefix()
   {
      return "Profile";
   }
   
   private void savePropertiesWithPath(String path)
   {
      String name = FileSystemItem.getNameFromPath(path);
      persistDocumentProperty("name", name);
      persistDocumentProperty("path", path);
      
      getName().setValue(name, true);
      name_.fireChangeEvent();
   }

   private ProfilerContents getContents()
   {
      return doc_.getProperties().cast();
   }
   
   private void persistDocumentProperty(String property, String value)
   {
      HashMap<String, String> props = new HashMap<String, String>();   
      props.put(property, value);
      
      sourceServer_.modifyDocumentProperties(
         doc_.getId(),
         props,
         new SimpleRequestCallback<Void>("Error")
         {
            @Override
            public void onResponseReceived(Void response)
            {
            }

            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
               globalDisplay_.showErrorMessage("Failed to Save Profile Properties",
                     error.getMessage());
            }
      });
   }

   private void saveNewFile(final String suggestedPath)
   {
      FileSystemItem fsi;
      if (suggestedPath != null)
         fsi = FileSystemItem.createFile(suggestedPath).getParentPath();
      else
         fsi = workbenchContext_.getDefaultFileDialogDir();
 
      fileDialogs_.saveFile(
            "Save File - " + getName().getValue(),
            fileContext_,
            fsi,
            fileType_.getDefaultExtension(),
            false,
            new ProgressOperationWithInput<FileSystemItem>()
            {
               public void execute(final FileSystemItem saveItem,
                                   final ProgressIndicator indicator)
               {
                  if (saveItem == null)
                     return;

                  workbenchContext_.setDefaultFileDialogDir(
                        saveItem.getParentPath());
                  
                  final String toPath = saveItem.getPath();
                  server_.copyProfile(
                     htmlLocalPath_,
                     toPath,
                     new ServerRequestCallback<JavaScriptObject>() {
                        @Override
                        public void onResponseReceived(JavaScriptObject response)
                        {
                           savePropertiesWithPath(saveItem.getPath());
                           
                           persistDocumentProperty("isUserSaved", "saved");
                           isUserSaved_ = true;
                           
                           indicator.onCompleted();
                        }

                        @Override
                        public void onError(ServerError error)
                        {
                           Debug.logError(error);
                           indicator.onCompleted();
                           globalDisplay_.showErrorMessage("Failed to Save Profile",
                                 error.getMessage());
                        }
                  });
               }
            });
   }

   private Command postSaveProfileCommand()
   {
      return new Command()
      {
         public void execute()
         {
         }
      };
   }
   
   private void onMessage(final String message,
                          final String file,
                          final String normPath,
                          final String details,
                          final int line)
   {
      if (message == "sourcefile")
      {
         server_.profileSources(file, normPath, new ServerRequestCallback<String>()
         {
            @Override
            public void onResponseReceived(String response)
            {
               selectedLine_ = line;
               hasValidPath_ = !StringUtil.isNullOrEmpty(response);
               selectedPath_ = hasValidPath_ ? response : file;
               
               commands_.gotoProfileSource().setEnabled(hasValidPath_);
               
               if (details == "open")
               {
                  if (hasValidPath_)
                  {
                     FilePosition filePosition = FilePosition.create(line, 0);
                     CodeNavigationTarget navigationTarget = new CodeNavigationTarget(response, filePosition);
                     
                     fileTypeRegistry_.editFile(
                           FileSystemItem.createFile(navigationTarget.getFile()),
                           filePosition);
                  }
                  else if (selectedPath_.indexOf("<expr>") == -1)
                  {
                     globalDisplay_.showMessage(GlobalDisplay.MSG_ERROR,
                           "Error while opening profiler source",
                           "The source file " + selectedPath_ + " does not exist.");
                  }
               }
            }
            
            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
            }
         });
      }
   }
   
   public static void onGlobalMessage(final String message,
                                final String file,
                                final String normPath,
                                final String details,
                                final int line)
   {
      if (activeProfilerEditingTarger_ != null)
      {
         
         activeProfilerEditingTarger_.onMessage(message, file, normPath, details, line);
      }
   }

   @Handler
   void onPopoutDoc()
   {
      events_.fireEvent(new PopoutDocEvent(getId(), currentPosition()));
   }

   @Handler
   void onReturnDocToMain()
   {
      events_.fireEventToMainWindow(new DocWindowChangedEvent(
            getId(), SourceWindowManager.getSourceWindowId(), "",
            DocTabDragParams.create(getId(), currentPosition()),
            null, 0));
   }

   private native static void initializeEvents() /*-{
      var handler = $entry(function(e) {
         if (typeof e.data != 'object')
            return;
         if (e.origin.substr(0, e.origin.length) != $wnd.location.origin)
            return;
         if (e.data.source != "profvis")
            return;
            
         @org.rstudio.studio.client.workbench.views.source.editors.profiler.ProfilerEditingTarget::onGlobalMessage(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)(
            e.data.message,
            e.data.file,
            e.data.normpath,
            e.data.details,
            e.data.line
         );
      });
      $wnd.addEventListener("message", handler, true);
   }-*/;
   
   private SourceDocument doc_;
   private ProfilerEditingTargetWidget view_;
   private final ProfilerPresenter presenter_;
   
   private final Value<Boolean> neverDirtyState_ = new Value<Boolean>(false);

   private final EventBus events_;
   private final Commands commands_;
   private final ProfilerServerOperations server_;
   private final SourceServerOperations sourceServer_;
   private final GlobalDisplay globalDisplay_;
   private Provider<SourceWindowManager> pSourceWindowManager_;
   private final FileDialogs fileDialogs_;
   private final RemoteFileSystemContext fileContext_;
   private final WorkbenchContext workbenchContext_;
   private final EventBus eventBus_;
   private Provider<String> defaultNameProvider_;
   private final FileTypeRegistry fileTypeRegistry_;
   
   private ProfilerType fileType_ = new ProfilerType();
   
   private HandlerRegistration commandHandlerReg_;
   
   private boolean htmlPathInitialized_;
   
   private static boolean initializedEvents_;
   
   private Value<String> name_ = new Value<String>(null);
   private String tempName_;
   
   private String htmlPath_;
   private String htmlLocalPath_;
   private boolean isUserSaved_;
   
   private static ProfilerEditingTarget activeProfilerEditingTarger_;
   private String selectedPath_;
   private int selectedLine_;
   private Boolean hasValidPath_ = false;
}
