/*
 * UrlContentEditingTarget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.urlcontent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.EnsureHeightHandler;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.ReadOnlyValue;
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.common.filetypes.FileIconResources;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.events.CollabEditStartParams;
import org.rstudio.studio.client.workbench.views.source.events.DocWindowChangedEvent;
import org.rstudio.studio.client.workbench.views.source.events.SourceNavigationEvent;
import org.rstudio.studio.client.workbench.views.source.model.ContentItem;
import org.rstudio.studio.client.workbench.views.source.model.DocTabDragParams;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourceNavigation;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import java.util.HashSet;

public class UrlContentEditingTarget implements EditingTarget
{
   public interface Display extends IsWidget
   {
      void print();
   }

   interface MyBinder extends CommandBinder<Commands, UrlContentEditingTarget>
   {}

   @Inject
   public UrlContentEditingTarget(SourceServerOperations server,
                                  Commands commands,
                                  GlobalDisplay globalDisplay,
                                  EventBus events)
   {
      server_ = server;
      commands_ = commands;
      globalDisplay_ = globalDisplay;
      events_ = events;
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
      String title = getContentTitle();
      return new Value<String>(title);
   }
   
   public String getTitle()
   {
      return getContentTitle();
   }

   public String getPath()
   {
      return null;
   }
   
   public String getContext()
   {
      return null;
   }

   public ImageResource getIcon()
   {
      return new ImageResource2x(FileIconResources.INSTANCE.iconText2x());
   }
   
   @Override
   public TextFileType getTextFileType()
   {
      return null;
   }


   public String getTabTooltip()
   {
      return null;
   }

   public HashSet<AppCommand> getSupportedCommands()
   {
      HashSet<AppCommand> commands = new HashSet<AppCommand>();
      commands.add(commands_.printSourceDoc());
      if (SourceWindowManager.isMainSourceWindow())
         commands.add(commands_.popoutDoc());
      else
         commands.add(commands_.returnDocToMain());
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

   @Handler
   void onPrintSourceDoc()
   {
      view_.print();
   }

   @Handler
   void onPopoutDoc()
   {
      popoutDoc();
   }

   public void popoutDoc()
   {
      globalDisplay_.openWindow(getContentUrl());
   }
   
   
   @Handler
   void onReturnDocToMain()
   {
      events_.fireEventToMainWindow(new DocWindowChangedEvent(
            getId(), SourceWindowManager.getSourceWindowId(), "",
            DocTabDragParams.create(getId(), currentPosition()), null, 0));
   }
   
   public void focus()
   {
   }

   public void onActivate()
   {
      if (commandReg_ != null)
      {
         Debug.log("Warning: onActivate called twice without intervening onDeactivate");
         commandReg_.removeHandler();
         commandReg_ = null;
      }
      commandReg_ = binder_.bind(commands_, this);
   }

   public void onDeactivate()
   {
      if (commandReg_ != null)
         commandReg_.removeHandler();
      commandReg_ = null;
      
      recordCurrentNavigationPosition();
     
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
      // always true because url content editing targets don't have the
      // concept of a position
      return true;
   }
     
   @Override
   public void highlightDebugLocation(
         SourcePosition startPos,
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

   public void onDismiss(int dismissType)
   {
      server_.removeContentUrl(getContentUrl(),
                               new ServerRequestCallback<org.rstudio.studio.client.server.Void>()
                               {
                                  @Override
                                  public void onError(ServerError error)
                                  {
                                     Debug.logError(error);
                                  }
                               });
   }

   protected String getContentTitle()
   {
      return getContentItem().getTitle();
   }

   protected String getContentUrl()
   {
      return getContentItem().getContentUrl();
   }

   public ReadOnlyValue<Boolean> dirtyState()
   {
      return dirtyState_;
   }
   
   @Override
   public boolean isSaveCommandActive()
   {
      return dirtyState().getValue();
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

   public void initialize(SourceDocument document,
                          FileSystemContext fileContext,
                          FileType type,
                          Provider<String> defaultNameProvider)
   {
      doc_ = document;
      view_ = createDisplay();
   }

   protected Display createDisplay()
   {
      return new UrlContentEditingTargetWidget(commands_,
                                                getContentUrl());
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

   public HandlerRegistration addCloseHandler(
         CloseHandler<Void> voidCloseHandler)
   {
      return addEnsureVisibleHandler(null);
   }

   public void fireEvent(GwtEvent<?> event)
   {
      assert false : "Not implemented";
   }

   public String getDefaultNamePrefix()
   {
      return null;
   }

   private ContentItem getContentItem()
   {
      return (ContentItem)doc_.getProperties().cast();
   }

   protected SourceDocument doc_;
   private Value<Boolean> dirtyState_ = new Value<Boolean>(false);

   protected final SourceServerOperations server_;
   protected final Commands commands_;
   protected final GlobalDisplay globalDisplay_;
   private final EventBus events_;
   private Display view_;
   private HandlerRegistration commandReg_;

   private static final MyBinder binder_ = GWT.create(MyBinder.class);
}
