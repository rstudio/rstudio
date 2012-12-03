/*
 * CodeBrowserEditingTarget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.codebrowser;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
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

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.ReadOnlyValue;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.common.filetypes.FileIconResources;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.codesearch.model.SearchPathFunctionDefinition;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FindRequestedEvent;
import org.rstudio.studio.client.workbench.views.source.model.CodeBrowserContents;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class CodeBrowserEditingTarget implements EditingTarget
{
   public static final String PATH = "code_browser://";
   
   public interface Display extends TextDisplay                                                      
   {
      void showFunction(SearchPathFunctionDefinition functionDef);
      void showFind(boolean defaultForward);
      void findNext();
      void findPrevious();
      void scrollToLeft();
   }

   interface MyBinder extends CommandBinder<Commands, CodeBrowserEditingTarget>
   {}

   @Inject
   public CodeBrowserEditingTarget(SourceServerOperations server,
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
      
      docDisplay_.addKeyDownHandler(new KeyDownHandler()
      {
         public void onKeyDown(KeyDownEvent event)
         {
            NativeEvent ne = event.getNativeEvent();
            int mod = KeyboardShortcut.getModifierValue(ne);
            if ((mod == KeyboardShortcut.META || 
                (mod == KeyboardShortcut.CTRL && !BrowseCap.hasMetaKey()))
                && ne.getKeyCode() == 'F')
            {
               event.preventDefault();
               event.stopPropagation();
               commands_.findReplace().execute();
            }
         }
      });
      
      docDisplay_.addFindRequestedHandler(new FindRequestedEvent.Handler() {
         
         @Override
         public void onFindRequested(FindRequestedEvent event)
         {
            view_.showFind(event.getDefaultForward());
         }
      });
   }
   
   @Override
   public void initialize(SourceDocument document,
                          FileSystemContext fileContext,
                          FileType type,
                          Provider<String> defaultNameProvider)
   {
      doc_ = document;
      view_ = new CodeBrowserEditingTargetWidget(commands_,
                                                 globalDisplay_,
                                                 events_,
                                                 prefs_, 
                                                 server_, 
                                                 docDisplay_);
      
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
    
      
      // if we have contents then set them
      CodeBrowserContents contents = getContents();
      if (contents.getContext().length() > 0)
      {
         ensureContext(contents.getContext(), new Command() {
            @Override
            public void execute()
            {
            }
         });
      }
      else
      {
         docDisplay_.setCode("", false);
      }
      
   }
   
   public void showFunction(SearchPathFunctionDefinition functionDef)
   {
      // set the current function
      currentFunction_ = functionDef;
      view_.showFunction(functionDef);
      view_.scrollToLeft();
      
      // update document properties if necessary
      final CodeBrowserContents contents = 
                        CodeBrowserContents.create(getContext());
      if (!contents.equalTo(getContents()))
      {
         HashMap<String, String> props = new HashMap<String, String>();
         contents.fillProperties(props);
         server_.modifyDocumentProperties(
               doc_.getId(),
               props,
               new SimpleRequestCallback<Void>("Error")
               {
                  @Override
                  public void onResponseReceived(Void response)
                  {
                     contents.fillProperties(doc_.getProperties()); 
                  }
               });
      }
   }
   
   
   @Handler
   void onPrintSourceDoc()
   {
      TextEditingTarget.onPrintSourceDoc(docDisplay_);
   }
   
   @Handler
   void onGoToHelp()
   {
      docDisplay_.goToHelp();
   } 
   
   @Handler
   void onGoToFunctionDefinition()
   {
      docDisplay_.goToFunctionDefinition();
   } 

   @Handler
   void onFindReplace()
   {
      view_.showFind(true);
   }

   @Handler
   void onFindNext()
   {
      view_.findNext();
   }
   
   @Handler
   void onFindPrevious()
   {
      view_.findPrevious();
   }
   
   
   @Override
   public String getId()
   {
      return doc_.getId();
   }

   @Override
   public HasValue<String> getName()
   {
      return new Value<String>("Source Viewer");
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
   public String getContext()
   {
      if (currentFunction_ != null)
      {
         return currentFunction_.getNamespace() + ":::" + 
                currentFunction_.getName();
      }
      else
      {
         return "";
      }
   }
   
   

   @Override
   public ImageResource getIcon()
   {
      return FileIconResources.INSTANCE.iconSourceViewer();
   }

   @Override
   public String getTabTooltip()
   {
      return "R Source Viewer";
   }

   @Override
   public HashSet<AppCommand> getSupportedCommands()
   {
      HashSet<AppCommand> commands = new HashSet<AppCommand>();
      commands.add(commands_.printSourceDoc());
      commands.add(commands_.findReplace());
      commands.add(commands_.findNext());
      commands.add(commands_.findPrevious());
      commands.add(commands_.goToHelp());
      commands.add(commands_.goToFunctionDefinition());
      return commands;
   }

   @Override
   public boolean canCompilePdf()
   {
      return false;
   }
   
   @Override
   public void verifyPrerequisites()
   {
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
   public void navigateToPosition(final SourcePosition position, 
                                  final boolean recordCurrent)
   {
      navigateToPosition(position, recordCurrent, false);
   }
   
   @Override
   public void navigateToPosition(final SourcePosition position, 
                                  final boolean recordCurrent,
                                  final boolean highlightLine)
   {
      ensureContext(position.getContext(), new Command() {
         @Override
         public void execute()
         {
            docDisplay_.navigateToPosition(position, 
                                           recordCurrent, 
                                           highlightLine);
            view_.scrollToLeft();
         }
      });
   }
   
   @Override
   public void restorePosition(final SourcePosition position)
   {
      ensureContext(position.getContext(), new Command() {
         @Override
         public void execute()
         {
            docDisplay_.restorePosition(position);
            view_.scrollToLeft();
         }
      }); 
   }
   
   @Override
   public boolean isAtSourceRow(SourcePosition position)
   {
      return getContext().equals(position.getContext()) &&
             docDisplay_.isAtSourceRow(position);
   }
   
   @Override
   public void setCursorPosition(Position position)
   {
      docDisplay_.setCursorPosition(position);
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
   public boolean isSaveCommandActive()
   {
      return dirtyState().getValue();
   }
   
   @Override
   public void save(Command onCompleted)
   {
      onCompleted.execute();
   }
   
   @Override
   public void saveWithPrompt(Command onCompleted, Command onCancelled)
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
   public HandlerRegistration addCloseHandler(CloseHandler<java.lang.Void> handler)
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

   private CodeBrowserContents getContents()
   {
      if (doc_.getProperties().keys().length() > 0)
         return (CodeBrowserContents)doc_.getProperties().cast();
      else
         return CodeBrowserContents.create("");
   }
   
   private void ensureContext(String context, final Command onRestored)
   {
      if (!context.equals(getContext()))
      {
         // get namespace and function
         String[] contextElements = context.split(":::");
         if (contextElements.length != 2)
            return;
         String namespace = contextElements[0];
         String name = contextElements[1];
         
         server_.getSearchPathFunctionDefinition(
               name, 
               namespace, 
               new SimpleRequestCallback<SearchPathFunctionDefinition>(
                        "Error Reading Function Definition") {
                  @Override
                  public void onResponseReceived(
                                    SearchPathFunctionDefinition functionDef)
                  {
                     showFunction(functionDef);
                     onRestored.execute();
                  }
         });
      }
      else
      {
         onRestored.execute();
      }
   }

   private SourceDocument doc_;
 
   private final Value<Boolean> dirtyState_ = new Value<Boolean>(false);
   private ArrayList<HandlerRegistration> releaseOnDismiss_ =
         new ArrayList<HandlerRegistration>();
   private final SourceServerOperations server_;
   private final Commands commands_;
   private final EventBus events_;
   private final GlobalDisplay globalDisplay_;
   private final UIPrefs prefs_;
   private final FontSizeManager fontSizeManager_;
   private Display view_;
   private HandlerRegistration commandReg_;
   
   private DocDisplay docDisplay_;
   
   private SearchPathFunctionDefinition currentFunction_ = null;

   private static final MyBinder binder_ = GWT.create(MyBinder.class);

  
}
