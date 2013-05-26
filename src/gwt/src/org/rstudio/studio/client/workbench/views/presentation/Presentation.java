/*
 * Presentation.java

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
package org.rstudio.studio.client.workbench.views.presentation;

import java.util.Iterator;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.inject.Inject;

import org.rstudio.core.client.Barrier;
import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.Barrier.Token;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.BarrierReleasedEvent;
import org.rstudio.core.client.events.BarrierReleasedHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.ReloadEvent;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.rpubs.ui.RPubsUploadDialog;

import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;

import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.LastChanceSaveEvent;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.StringStateValue;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.edit.ui.EditDialog;
import org.rstudio.studio.client.workbench.views.presentation.events.ShowPresentationPaneEvent;
import org.rstudio.studio.client.workbench.views.presentation.events.SourceFileSaveCompletedEvent;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationRPubsSource;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationServerOperations;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationState;
import org.rstudio.studio.client.workbench.views.presentation.model.SlideNavigation;
import org.rstudio.studio.client.workbench.views.presentation.model.SlideNavigationItem;
import org.rstudio.studio.client.workbench.views.source.events.EditPresentationSourceEvent;

public class Presentation extends BasePresenter 
{
   public interface Binder extends CommandBinder<Commands, Presentation> {}
   
   public interface Display extends WorkbenchView
   {  
      void load(String url);
      void zoom(String title, String url, Command onClosed);
      void clear();
      boolean hasSlides();
      
      void home();
      void slide(int index);
      void next();
      void prev();
      
      void pauseMedia();
      
      SlideMenu getSlideMenu();
      
      String getPresentationTitle();
   }
   
   public interface SlideMenu
   {
      void setCaption(String caption);
      void setDropDownVisible(boolean visible);
      void addItem(MenuItem menu);
      void clear();
   }
   
   @Inject
   public Presentation(Display display, 
                       PresentationServerOperations server,
                       GlobalDisplay globalDisplay,
                       FileDialogs fileDialogs,
                       RemoteFileSystemContext fileSystemContext,
                       EventBus eventBus,
                       FileTypeRegistry fileTypeRegistry,
                       Session session,
                       Binder binder,
                       Commands commands,
                       PresentationDispatcher dispatcher)
   {
      super(display);
      view_ = display;
      server_ = server;
      globalDisplay_ = globalDisplay;
      fileDialogs_ = fileDialogs;
      fileSystemContext_ = fileSystemContext;
      eventBus_ = eventBus;
      commands_ = commands;
      fileTypeRegistry_ = fileTypeRegistry;
      session_ = session;
      dispatcher_ = dispatcher;
      dispatcher_.setContext(new PresentationDispatcher.Context() 
      {
         @Override
         public void pauseMedia()
         {
            view_.pauseMedia();
         }
         
         @Override
         public String getPresentationFilePath()
         {
            return currentState_.getFilePath();
         }
      });
     
      binder.bind(commands, this);
      
      // auto-refresh for presentation files saved
      eventBus.addHandler(SourceFileSaveCompletedEvent.TYPE, 
                         new SourceFileSaveCompletedEvent.Handler() { 
         @Override
         public void onSourceFileSaveCompleted(SourceFileSaveCompletedEvent event)
         {
            if (currentState_ != null)
            {
               FileSystemItem file = event.getSourceFile();
               if (file.getPath().equals(currentState_.getFilePath()))
               {    
                  int index = detectSlideIndex(event.getContents(),
                                               event.getCursorPos().getRow());
                  if (index != -1)
                     currentState_.setSlideIndex(index);
                  
                  view_.load(buildPresentationUrl());
               }
               else if (file.getParentPathString().equals(getCurrentPresDir()) 
                          &&
                        file.getExtension().toLowerCase().equals(".css"))
               {
                  view_.load(buildPresentationUrl());
               }
            }
         }
      });
      
      new StringStateValue(
            MODULE_PRESENTATION,
            KEY_SAVEAS_DIR,
            ClientState.PERSISTENT,
            session_.getSessionInfo().getClientState())
      {
         @Override
         protected void onInit(String value)
         {
            saveAsStandaloneDir_ = value;
         }

         @Override
         protected String getValue()
         {
            return saveAsStandaloneDir_;
         }
      };
      
      initPresentationCallbacks();
   }
   
   public void initialize(PresentationState state)
   {
      if ((state.getSlideIndex() == 0) || state.isTutorial())
         view_.bringToFront();
      
      init(state);
   }
   
   public void onShowPresentationPane(ShowPresentationPaneEvent event)
   {
      globalDisplay_.showProgress("Opening Presentation...");
      reloadWorkbench();
   }
   
   @Handler
   void onPresentationHome()
   {
      view_.home();
   }
   
   @Handler
   void onPresentationNext()
   {
      view_.next();
   }
   
   @Handler
   void onPresentationPrev()
   {
      view_.prev();
   }
   
   @Handler
   void onPresentationEdit()
   {
      eventBus_.fireEvent(new EditPresentationSourceEvent(
            FileSystemItem.createFile(currentState_.getFilePath()),
            currentState_.getSlideIndex()));
   }
   
   @Handler
   void onPresentationFullscreen()
   {
      // clear the internal iframe so there is no conflict over handling
      // presentation events (we'll restore it on zoom close)
      view_.clear();
      
      // show the zoomed version of the presentation. after it closes
      // restore the inline version
      view_.zoom(session_.getSessionInfo().getPresentationName(),
                 buildPresentationUrl("zoom"), 
                 new Command() {
         @Override
         public void execute()
         {
            view_.load(buildPresentationUrl()); 
         } 
      });
   }
   
   @Handler
   void onPresentationViewInBrowser()
   {
      if (Desktop.isDesktop())
      {
         server_.createDesktopViewInBrowserPresentation(
            new SimpleRequestCallback<String>() {
               @Override
               public void onResponseReceived(String path)
               {
                  Desktop.getFrame().showFile(path);
               }
            });
      }
      else
      {
         globalDisplay_.openWindow(
                           server_.getApplicationURL("presentation/view"));
      }
   }
   
   @Handler
   void onPresentationSaveAsStandalone()
   {
      FileSystemItem defaultDir = saveAsStandaloneDir_ != null ?
            FileSystemItem.createDir(saveAsStandaloneDir_) :
            FileSystemItem.home();
       
      fileDialogs_.saveFile(
         "Save Presentation As", 
          fileSystemContext_, 
          defaultDir, 
          ".html",
          false, 
          new ProgressOperationWithInput<FileSystemItem>(){

            @Override
            public void execute(final FileSystemItem targetFile,
                                ProgressIndicator indicator)
            {
               if (targetFile == null)
               {
                  indicator.onCompleted();
                  return;
               }
               
               indicator.onProgress("Saving Presentation...");
   
               server_.createStandalonePresentation(
                  targetFile.getPath(), 
                  new VoidServerRequestCallback(indicator) {
                     @Override
                     public void onSuccess()
                     {
                        saveAsStandaloneDir_ = targetFile.getParentPathString();
                        session_.persistClientState();
                     }
                  });
            }
      }); 
   }
   
   private void saveAsStandalone(String targetFile, 
                                 final ProgressIndicator indicator,
                                 final Command onSuccess)
   {
      server_.createStandalonePresentation(
            targetFile, new VoidServerRequestCallback(indicator) {
               @Override
               public void onSuccess()
               {
                  onSuccess.execute();
               }
            });
   }
   
   @Handler
   void onPresentationPublishToRpubs()
   {
      server_.createPresentationRPubsSource(
         new SimpleRequestCallback<PresentationRPubsSource>() {
            
            @Override
            public void onResponseReceived(PresentationRPubsSource source)
            {
               RPubsUploadDialog dlg = new RPubsUploadDialog(
                     "Presentation",
                     view_.getPresentationTitle(),
                     source.getSourceFilePath(),
                     source.isPublished());
               dlg.showModal();
            }
            
            @Override
            public void onError(ServerError error)
            {
               globalDisplay_.showErrorMessage("Error Saving Presentation",
                                               getErrorMessage(error));
            }
      });
   }
   
   
   
   @Handler
   void onRefreshPresentation()
   {
      if (Event.getCurrentEvent().getShiftKey())
         currentState_.setSlideIndex(0);
      
      view_.load(buildPresentationUrl());
   }
   
   @Handler
   void onTutorialFeedback()
   {
      EditDialog editDialog = new EditDialog("Provide Feedback",
                                             "Submit", 
                                             "",
                                             false, 
                                             true,
                                             new Size(450,300),
                     new ProgressOperationWithInput<String>() {
         @Override
         public void execute(String input, ProgressIndicator indicator)
         {
            if (input == null)
            {
               indicator.onCompleted();
               return;
            }
            
            indicator.onProgress("Saving feedback...");
            
            server_.tutorialFeedback(input, 
                                     new VoidServerRequestCallback(indicator));
            
         }
      });
      
      editDialog.showModal();
      
   }
   
   
   @Override
   public void onSelected()
   {
      super.onSelected();
      
      // after doing a pane reconfig the frame gets wiped (no idea why)
      // workaround this by doing a check for an active state with
      // no slides currently displayed
      if (currentState_ != null && 
          currentState_.isActive() && 
          !view_.hasSlides())
      {
         init(currentState_);
      }
   }
   
   
   public void confirmClose(Command onConfirmed)
   {
      // don't allow close if this is a tutorial
      if (currentState_.isTutorial())
      {
         globalDisplay_.showMessage(
               MessageDisplay.MSG_WARNING,
               "Unable to Close",
               "Tutorials cannot be closed");
         return;
      }
      
      final ProgressIndicator progress = new GlobalProgressDelayer(
            globalDisplay_,
            0,
            "Closing Presentation...").getIndicator();
      
      server_.closePresentationPane(new ServerRequestCallback<Void>(){
         @Override
         public void onResponseReceived(Void resp)
         {
            reloadWorkbench();
         }

         @Override
         public void onError(ServerError error)
         {
            progress.onError(error.getUserMessage());
            
         }
      });
   }
   
   private void reloadWorkbench()
   { 
      Barrier barrier = new Barrier();
      barrier.addBarrierReleasedHandler(new BarrierReleasedHandler() {

         @Override
         public void onBarrierReleased(BarrierReleasedEvent event)
         {
            eventBus_.fireEvent(new ReloadEvent());
         }
      });
      
      Token token = barrier.acquire();
      try
      {
         eventBus_.fireEvent(new LastChanceSaveEvent(barrier));
      }
      finally
      {
         token.release();
      }  
   }
   
   
   private void init(PresentationState state)
   {
      currentState_ = state;
      view_.load(buildPresentationUrl());
   }
   
   private String buildPresentationUrl()
   {
      return buildPresentationUrl(null);
   }
   
   private String buildPresentationUrl(String extraPath)
   {
      String url = server_.getApplicationURL("presentation/");
      if (extraPath != null)
         url = url + extraPath;
      url = url + "#/" + currentState_.getSlideIndex();
      return url;
   }
   
   private boolean isPresentationActive()
   {
      return (currentState_ != null) && 
             (currentState_.isActive())&& 
             view_.hasSlides();
   }
   
   private String getCurrentPresDir()
   {
      if (currentState_ == null)
         return "";
      
      FileSystemItem presFilePath = FileSystemItem.createFile(
                                               currentState_.getFilePath());
      return presFilePath.getParentPathString();
   }
   
   private void onPresentationSlideChanged(final int index, 
                                           final JavaScriptObject jsCmds)
   {  
      // note the slide index and save it
      currentState_.setSlideIndex(index);
      indexPersister_.setIndex(index);
      
      
      // find the first navigation item that is <= to the index
      if (slideNavigation_ != null)
      {
         JsArray<SlideNavigationItem> items = slideNavigation_.getItems();
         for (int i=(items.length()-1); i>=0; i--)
         {
            if (items.get(i).getIndex() <= index)
            {
               String caption = items.get(i).getTitle();
               caption += " (" + (index+1) + "/" + 
                          slideNavigation_.getTotalSlides() + ")";
               
               
               view_.getSlideMenu().setCaption(caption);
               break;
            }
         }
      }
         
      // execute commands if we stay on the slide for > 500ms
      new Timer() {
         @Override
         public void run()
         {
            // execute commands if we're still on the same slide
            if (index == currentState_.getSlideIndex())
            {
               JsArray<JavaScriptObject> cmds = jsCmds.cast();
               for (int i=0; i<cmds.length(); i++)
                  dispatchCommand(cmds.get(i));
            }
         }   
      }.schedule(500);  
   }
   
   private void dispatchCommand(JavaScriptObject jsCommand)
   {
      dispatcher_.dispatchCommand(jsCommand);
   }
   
   private void initPresentationNavigator(JavaScriptObject jsNavigator)
   {
      // record current slides
      slideNavigation_ = jsNavigator.cast();
      JsArray<SlideNavigationItem> items = slideNavigation_.getItems();
      
      // reset the slides menu
      SlideMenu slideMenu = view_.getSlideMenu();
      slideMenu.clear(); 
      for (int i=0; i<items.length(); i++)
      {
         // get slide
         final SlideNavigationItem item = items.get(i);
          
         // build html
         SafeHtmlBuilder menuHtml = new SafeHtmlBuilder();
         for (int j=0; j<item.getIndent(); j++)
            menuHtml.appendHtmlConstant("&nbsp;&nbsp;&nbsp;");
         menuHtml.appendEscaped(item.getTitle());
         
      
         slideMenu.addItem(new MenuItem(menuHtml.toSafeHtml(),
                                        new Command() {
            @Override
            public void execute()
            {
               view_.slide(item.getIndex()); 
            }
         })); 
      }  
      
      slideMenu.setDropDownVisible(slideNavigation_.getItems().length() > 1);
   }
   
   private void recordPresentationQuizAnswer(int slideIndex, 
                                             int answer, 
                                             boolean correct)
   {
      server_.tutorialQuizResponse(slideIndex, 
                                   answer, 
                                   correct, 
                                   new VoidServerRequestCallback());
   }
   
   private final native void initPresentationCallbacks() /*-{
      var thiz = this;
      $wnd.presentationSlideChanged = $entry(function(index, cmds) {
         thiz.@org.rstudio.studio.client.workbench.views.presentation.Presentation::onPresentationSlideChanged(ILcom/google/gwt/core/client/JavaScriptObject;)(index, cmds);
      });
      $wnd.dispatchPresentationCommand = $entry(function(cmd) {
         thiz.@org.rstudio.studio.client.workbench.views.presentation.Presentation::dispatchCommand(Lcom/google/gwt/core/client/JavaScriptObject;)(cmd);
      });
      $wnd.initPresentationNavigator = $entry(function(slides) {
         thiz.@org.rstudio.studio.client.workbench.views.presentation.Presentation::initPresentationNavigator(Lcom/google/gwt/core/client/JavaScriptObject;)(slides);
      });
      $wnd.recordPresentationQuizAnswer = $entry(function(index, answer, correct) {
         thiz.@org.rstudio.studio.client.workbench.views.presentation.Presentation::recordPresentationQuizAnswer(IIZ)(index, answer, correct);
      });
   }-*/;   
   
   private class IndexPersister extends TimeBufferedCommand
   {
      public IndexPersister()
      {
         super(500);
      }
      
      public void setIndex(int index)
      {
         index_ = index;
         nudge();
      }
      
      @Override
      protected void performAction(boolean shouldSchedulePassive)
      {
         server_.setPresentationSlideIndex(index_, 
                                           new VoidServerRequestCallback());
      }
      
      private int index_ = 0;
   };
   private IndexPersister indexPersister_ = new IndexPersister();
   
   
   
   
   
   private static int detectSlideIndex(String contents, int cursorLine)
   {
      int currentLine = 0;
      int slideIndex = -1; 
      String slideRegex = "^\\={3,}\\s*$";
      
      Iterator<String> it = StringUtil.getLineIterator(contents).iterator();
      while (it.hasNext())
      {
         String line = it.next();
         if (line.matches(slideRegex))
            slideIndex++;
         
         if (currentLine++ >= cursorLine)
         {
            // bump the slide index if the next line is a header
            if (it.hasNext() && it.next().matches(slideRegex))
               slideIndex++;
            
            return slideIndex;
         }
      }
      
      
      return -1;
   } 
   
   
   private String getErrorMessage(ServerError error)
   {
      String message = error.getUserMessage();
      JSONString userMessage = error.getClientInfo().isString();
      if (userMessage != null)
         message = userMessage.stringValue();
      return message;
   }
   
   private final Display view_ ; 
   private final PresentationServerOperations server_;
   private final GlobalDisplay globalDisplay_;
   private final EventBus eventBus_;
   private final Commands commands_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final FileDialogs fileDialogs_;
   private final RemoteFileSystemContext fileSystemContext_;
   private final Session session_;
   private final PresentationDispatcher dispatcher_;
   private PresentationState currentState_ = null;
   private SlideNavigation slideNavigation_ = null;
   private boolean usingRmd_ = false;
  
   
   private String saveAsStandaloneDir_;
   private static final String MODULE_PRESENTATION = "presentation";
   private static final String KEY_SAVEAS_DIR = "saveAsStandaloneDir";
}
