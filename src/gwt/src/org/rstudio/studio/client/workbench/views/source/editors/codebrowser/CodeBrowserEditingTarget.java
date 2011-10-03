/*
 * CodeBrowserEditingTarget.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.codebrowser;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.ReadOnlyValue;
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.common.filetypes.FileIconResources;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchServerOperations;
import org.rstudio.studio.client.workbench.codesearch.model.SearchPathFunctionDefinition;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.model.CodeBrowserContents;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import java.util.ArrayList;
import java.util.HashSet;

// TODO: joe on reflow ace call to fix issues

// TODO: token guessing must include explicit namespace qualifiers

// TODO: go to function definition inside code browser editing target
//       (no completion manager)

// TODO: implement navigation to CodeBrowser in Source.attemptNavigation

// TODO: when implementing the SourceDatabase side of the codebrowser
// editing target (and if we choose to write back to the Doc) we need to
// make sure the source_database is cogizent of the fact that it can 
// receive path arguments that are not actually file paths. we may actually
// need another source_database property indicating whether this represents
// an on-disk file or a url/data-frame/code-browser (this has been working
// to date because non-file back docs never call put). Note that we currently
// write (but never read) a durable properties entry for untitled docs with
// no path -- we should also clean this up

public class CodeBrowserEditingTarget implements EditingTarget
{
   public static final String PATH = "code_browser://";
   
   public interface Display extends TextDisplay                                                      
   {
      void showFunction(SearchPathFunctionDefinition functionDef);
   }

   interface MyBinder extends CommandBinder<Commands, CodeBrowserEditingTarget>
   {}

   @Inject
   public CodeBrowserEditingTarget(CodeSearchServerOperations server,
                                   Commands commands,
                                   EventBus events,
                                   UIPrefs prefs,
                                   FontSizeManager fontSizeManager,
                                   GlobalDisplay globalDisplay,
                                   DocDisplay docDisplay)
   {
      server_ = server;
      commands_ = commands;
      events_ = events;
      prefs_ = prefs;
      fontSizeManager_ = fontSizeManager;
      globalDisplay_ = globalDisplay;
      docDisplay_ = docDisplay;
      
      TextEditingTarget.addRecordNavigationPositionHandler(releaseOnDismiss_,
                                                           docDisplay_, 
                                                           events_, 
                                                           this);
   }
   
   @Override
   public void initialize(SourceDocument document,
                          FileSystemContext fileContext,
                          FileType type,
                          Provider<String> defaultNameProvider)
   {
      doc_ = document;
      view_ = new CodeBrowserEditingTargetWidget(commands_, docDisplay_);
      
      docDisplay_.setCode("", false);

      TextEditingTarget.registerPrefs(releaseOnDismiss_, prefs_, docDisplay_);
      
      TextEditingTarget.syncFontSize(releaseOnDismiss_, 
                                     events_, 
                                     view_, 
                                     fontSizeManager_);

      releaseOnDismiss_.add(prefs_.softWrapRFiles().addValueChangeHandler(
            new ValueChangeHandler<Boolean>()
            {
               public void onValueChange(ValueChangeEvent<Boolean> evt)
               {
                  view_.adaptToFileType(FileTypeRegistry.R);
               }
            }
      ));
   }
   
   public void showFunction(SearchPathFunctionDefinition functionDef)
   {
      view_.showFunction(functionDef);
   }
   
   
   @Handler
   void onPrintSourceDoc()
   {
      TextEditingTarget.onPrintSourceDoc(docDisplay_);
   }
   
   @Handler
   void onGoToFunctionDefinition()
   {
      docDisplay_.goToFunctionDefinition();
   } 


   @Override
   public String getId()
   {
      return doc_.getId();
   }

   @Override
   public HasValue<String> getName()
   {
      return new Value<String>("Code Browser");
   }
   
   @Override
   public String getTitle()
   {
      return getName().getValue();
   }

   @Override
   public String getPath()
   {
      return PATH;
   }

   @Override
   public ImageResource getIcon()
   {
      return FileIconResources.INSTANCE.iconRdoc();
   }

   @Override
   public String getTabTooltip()
   {
      return "R Code Browser";
   }

   @Override
   public HashSet<AppCommand> getSupportedCommands()
   {
      HashSet<AppCommand> commands = new HashSet<AppCommand>();
      commands.add(commands_.printSourceDoc());
      commands.add(commands_.goToFunctionDefinition());
      return commands;
   }

   @Override
   public void focus()
   {
      docDisplay_.focus();
   }

   
   @Override
   public void onActivate()
   {
      // IMPORTANT NOTE: most of this logic is duplicated in 
      // TextEditingTarget (no straightforward way to create a
      // re-usable implementation) so changes here need to be synced
      
      if (commandReg_ != null)
      {
         Debug.log("Warning: onActivate called twice without intervening onDeactivate");
         commandReg_.removeHandler();
         commandReg_ = null;
      }
      commandReg_ = binder_.bind(commands_, this);
      
      view_.onActivate();
   }

   @Override
   public void onDeactivate()
   {
      // IMPORTANT NOTE: most of this logic is duplicated in 
      // TextEditingTarget (no straightforward way to create a
      // re-usable implementation) so changes here need to be synced
      
      commandReg_.removeHandler();
      commandReg_ = null;
      
      // switching tabs is a navigation action
      try
      {
         docDisplay_.recordCurrentNavigationPosition();
      }
      catch(Exception e)
      {
         Debug.log("Exception recording nav position: " + e.toString());
      }
   }

   @Override
   public void onInitiallyLoaded()
   {
   }
   
   @Override
   public void recordCurrentNavigationPosition()
   {
      docDisplay_.recordCurrentNavigationPosition();
   }
   
   @Override
   public void navigateToPosition(SourcePosition position, 
                                  boolean recordCurrent)
   {
      docDisplay_.navigateToPosition(position, recordCurrent);
   }
   
   @Override
   public void restorePosition(SourcePosition position)
   {
      docDisplay_.restorePosition(position);
   }
   
   @Override
   public boolean isAtSourceRow(SourcePosition position)
   {
      return docDisplay_.isAtSourceRow(position);
   }
   
   
   @Override
   public boolean onBeforeDismiss()
   {
      return true;
   }

   @Override
   public void onDismiss()
   {
      while (releaseOnDismiss_.size() > 0)
         releaseOnDismiss_.remove(0).removeHandler();
   }

   @Override
   public ReadOnlyValue<Boolean> dirtyState()
   {
      return dirtyState_;
   }
   
   @Override
   public void save(Command onCompleted)
   {
      onCompleted.execute();
   }
   
   @Override
   public void saveWithPrompt(Command onCompleted)
   {
      onCompleted.execute();
   }
   
   @Override
   public void revertChanges(Command onCompleted)
   {
      onCompleted.execute();
   }
   
   @Override
   public long getFileSizeLimit()
   {
      return Long.MAX_VALUE;
   }

   @Override
   public long getLargeFileSize()
   {
      return Long.MAX_VALUE;
   }

   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
   }

   @Override
   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return new HandlerRegistration()
      {
         public void removeHandler()
         {
         }
      };
   }

   @Override
   public HandlerRegistration addCloseHandler(CloseHandler<Void> handler)
   {
      return new HandlerRegistration()
      {
         public void removeHandler()
         {
         }
      };
   }

   @Override
   public void fireEvent(GwtEvent<?> event)
   {
      assert false : "Not implemented";
   }

   @SuppressWarnings("unused")
   private CodeBrowserContents getContents()
   {
      return (CodeBrowserContents)doc_.getProperties().cast();
   }

   private SourceDocument doc_;
 
   private final Value<Boolean> dirtyState_ = new Value<Boolean>(false);
   private ArrayList<HandlerRegistration> releaseOnDismiss_ =
         new ArrayList<HandlerRegistration>();
   @SuppressWarnings("unused")
   private final CodeSearchServerOperations server_;
   private final Commands commands_;
   private final EventBus events_;
   @SuppressWarnings("unused")
   private final GlobalDisplay globalDisplay_;
   private final UIPrefs prefs_;
   private final FontSizeManager fontSizeManager_;
   private Display view_;
   private HandlerRegistration commandReg_;
   
   private DocDisplay docDisplay_;

   private static final MyBinder binder_ = GWT.create(MyBinder.class);

  
}
