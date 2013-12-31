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

import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.events.EnsureHeightHandler;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.ReadOnlyValue;
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.common.filetypes.FileIconResources;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.events.SourceNavigationEvent;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourceNavigation;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import java.util.HashSet;

public class ProfilerEditingTarget implements EditingTarget
{
   public static final String PATH = "profiler://";
   
   @Inject
   public ProfilerEditingTarget(ProfilerPresenter presenter,
                                Commands commands,
                                EventBus events)
   {
      presenter_ = presenter;
      commands_ = commands;
      events_ = events;
   }

   public String getId()
   {
      return doc_.getId();
   }

   public HasValue<String> getName()
   {
      return new Value<String>("Profiler");
   }
   
   public String getTitle()
   {
      return "Profiler";
   }

   public String getPath()
   {
      return PATH;
   }
   
   public String getContext()
   {
      return null;
   }

   public ImageResource getIcon()
   {
      return FileIconResources.INSTANCE.iconProfiler();
   }

   public String getTabTooltip()
   {
      return "R Profiler";
   }

   public HashSet<AppCommand> getSupportedCommands()
   {
      return new HashSet<AppCommand>();
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
   }

   public void onDeactivate()
   {      
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
      return dirtyState().getValue();
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
      // initialize doc, view, and presenter
      doc_ = document;
      view_ = new ProfilerEditingTargetWidget(commands_);
      presenter_.attatch(doc_,  view_);
   }
   
   public void onDismiss()
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
   
   private SourceDocument doc_;
   private ProfilerEditingTargetWidget view_;
   private final ProfilerPresenter presenter_;
   
   private final Value<Boolean> neverDirtyState_ = new Value<Boolean>(false);

   private final EventBus events_;
   private final Commands commands_;   
}
