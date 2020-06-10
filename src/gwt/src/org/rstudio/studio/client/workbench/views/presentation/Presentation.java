/*
 * Presentation.java

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
package org.rstudio.studio.client.workbench.views.presentation;

import java.util.Iterator;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperation;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.ReloadWithLastChanceSaveEvent;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.presentation.SlideNavigationMenu;
import org.rstudio.studio.client.common.presentation.SlideNavigationPresenter;
import org.rstudio.studio.client.common.presentation.events.SlideIndexChangedEvent;
import org.rstudio.studio.client.common.presentation.events.SlideNavigationChangedEvent;
import org.rstudio.studio.client.common.presentation.model.SlideNavigation;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.presentation.events.PresentationPaneRequestCompletedEvent;
import org.rstudio.studio.client.workbench.views.presentation.events.ShowPresentationPaneEvent;
import org.rstudio.studio.client.workbench.views.presentation.events.SourceFileSaveCompletedEvent;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationServerOperations;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationState;
import org.rstudio.studio.client.workbench.views.source.events.EditPresentationSourceEvent;

public class Presentation extends BasePresenter
                          implements SlideNavigationPresenter.Display
{
   public interface Binder extends CommandBinder<Commands, Presentation> {}
   
   public interface Display extends WorkbenchView
   {  
      void load(String url, String sourceFile);
      void zoom(String title, String url, Command onClosed);
      void clear();
      boolean hasSlides();
      
      void home();
      void navigate(int index);
      void next();
      void prev();
      
      SlideNavigationMenu getNavigationMenu();
      
      void pauseMedia();
          
      String getPresentationTitle();
      
      void showBusy();
      void hideBusy();
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
      navigationPresenter_ = new SlideNavigationPresenter(this);
     
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
               if (file.getPath() == currentState_.getFilePath())
               {    
                  int index = detectSlideIndex(event.getContents(),
                                               event.getCursorPos().getRow());
                  if (index != -1)
                     currentState_.setSlideIndex(index);
                  
                  refreshPresentation();
               }
               else if (file.getParentPathString() == getCurrentPresDir() 
                          &&
                        file.getExtension().toLowerCase().equals(".css"))
               {
                  refreshPresentation();
               }
            }
         }
      });
      
      eventBus.addHandler(PresentationPaneRequestCompletedEvent.TYPE,
                          new PresentationPaneRequestCompletedEvent.Handler()
      {
         @Override
         public void onPresentationRequestCompleted(
               PresentationPaneRequestCompletedEvent event)
         {
            view_.hideBusy();
         }
      });
      
      initPresentationCallbacks();
   }
   
   public void initialize(PresentationState state)
   {
      if ((state.getSlideIndex() == 0))
         view_.bringToFront();
      
      init(state);
   }
   
   public void onShowPresentationPane(ShowPresentationPaneEvent event)
   {
      globalDisplay_.showProgress("Opening Presentation...");
      reloadWorkbench();
   }
   
   @Override
   public void editCurrentSlide()
   {
      eventBus_.fireEvent(new EditPresentationSourceEvent(
            FileSystemItem.createFile(currentState_.getFilePath()),
            currentState_.getSlideIndex())); 
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
            view_.load(buildPresentationUrl(), currentState_.getFilePath()); 
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
                  Desktop.getFrame().showFile(StringUtil.notNull(path));
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
      // determine the default file name
      if (saveAsStandaloneDefaultPath_ == null)
      {
         FileSystemItem presFilePath = FileSystemItem.createFile(
                                             currentState_.getFilePath());
         saveAsStandaloneDefaultPath_ = FileSystemItem.createFile(
               presFilePath.getParentPath().completePath(presFilePath.getStem() 
                                                  + ".html"));
      }
            
      fileDialogs_.saveFile(
         "Save Presentation As", 
          fileSystemContext_, 
          saveAsStandaloneDefaultPath_, 
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
                        saveAsStandaloneDefaultPath_ = targetFile;
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
   void onClearPresentationCache()
   {
      globalDisplay_.showYesNoMessage(
            MessageDialog.INFO, 
            "Clear Knitr Cache", 
            "Clearing the Knitr cache will discard previously cached " +
            "output and re-run all of the R code chunks within the " +
            "presentation.\n\n" +
            "Are you sure you want to clear the cache now?",
            false,
            new ProgressOperation() {

               @Override
               public void execute(final ProgressIndicator indicator)
               {
                  indicator.onProgress("Clearing Knitr Cache...");
                  server_.clearPresentationCache(
                        new ServerRequestCallback<Void>() {
                           @Override
                           public void onResponseReceived(Void response)
                           {
                              indicator.onCompleted();
                              refreshPresentation();
                           }

                           @Override
                           public void onError(ServerError error)
                           {
                              indicator.onCompleted();
                              globalDisplay_.showErrorMessage(
                                                "Error Clearing Cache",
                                                 getErrorMessage(error));
                           }
                        });
               }
               
            },
            new ProgressOperation() {

               @Override
               public void execute(ProgressIndicator indicator)
               {
                  indicator.onCompleted();
               }   
            },
            true);  
   }
   
   
   @Handler
   void onRefreshPresentation()
   {
      if (Event.getCurrentEvent().getShiftKey())
         currentState_.setSlideIndex(0);
      
      refreshPresentation();
   }
   
   private void refreshPresentation()
   {
      view_.showBusy();
      view_.load(buildPresentationUrl(), currentState_.getFilePath());
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
   

   @Override
   public void navigate(int index)
   {
     view_.navigate(index);
      
   }

   @Override
   public SlideNavigationMenu getNavigationMenu()
   {
      return view_.getNavigationMenu();
   }

   @Override
   public HandlerRegistration addSlideNavigationChangedHandler(
                              SlideNavigationChangedEvent.Handler handler)
   {
      return handlerManager_.addHandler(SlideNavigationChangedEvent.TYPE, 
                                        handler);
   }

   @Override
   public HandlerRegistration addSlideIndexChangedHandler(
                              SlideIndexChangedEvent.Handler handler)
   {
      return handlerManager_.addHandler(SlideIndexChangedEvent.TYPE, handler);
   }
   
   public static String getErrorMessage(ServerError error)
   {
      String message = error.getUserMessage();
      JSONString userMessage = error.getClientInfo().isString();
      if (userMessage != null)
         message = userMessage.stringValue();
      return message;
   }
   
   private void reloadWorkbench()
   { 
      eventBus_.fireEvent(new ReloadWithLastChanceSaveEvent());
   }
   
   
   private void init(PresentationState state)
   {
      currentState_ = state;
      view_.load(buildPresentationUrl(), currentState_.getFilePath());
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
      
      handlerManager_.fireEvent(new SlideIndexChangedEvent(index));
        
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
      SlideNavigation navigation = jsNavigator.cast();
      handlerManager_.fireEvent(
               new SlideNavigationChangedEvent(navigation));
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
   }

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
   
   private final Display view_;
   private final PresentationServerOperations server_;
   private final GlobalDisplay globalDisplay_;
   private final EventBus eventBus_;
   private final Commands commands_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final FileDialogs fileDialogs_;
   private final RemoteFileSystemContext fileSystemContext_;
   private final Session session_;
   private final PresentationDispatcher dispatcher_;
   private final SlideNavigationPresenter navigationPresenter_;
   private PresentationState currentState_ = null;
  
   private FileSystemItem saveAsStandaloneDefaultPath_ = null;
   
   private HandlerManager handlerManager_ = new HandlerManager(this);
}
