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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.http.client.URL;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.inject.Inject;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.ReloadEvent;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.filetypes.events.OpenPresentationSourceFileEvent;

import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;

import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.help.events.ShowHelpEvent;
import org.rstudio.studio.client.workbench.views.presentation.events.ShowPresentationPaneEvent;
import org.rstudio.studio.client.workbench.views.presentation.events.SourceDocumentSavedEvent;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationCommand;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationServerOperations;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationState;
import org.rstudio.studio.client.workbench.views.presentation.model.SlideInfo;

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
      void refresh(boolean resetAnchor);
      
      SlideMenu getSlideMenu();
   }
   
   public interface SlideMenu
   {
      void setCaption(String caption);
      void addItem(MenuItem menu);
      void clear();
   }
   
   @Inject
   public Presentation(Display display, 
                       PresentationServerOperations server,
                       GlobalDisplay globalDisplay,
                       EventBus eventBus,
                       FileTypeRegistry fileTypeRegistry,
                       Session session,
                       Binder binder,
                       Commands commands)
   {
      super(display);
      view_ = display;
      server_ = server;
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      commands_ = commands;
      fileTypeRegistry_ = fileTypeRegistry;
      session_ = session;
     
      binder.bind(commands, this);
      
      // auto-refresh for presentation files saved
      eventBus.addHandler(SourceDocumentSavedEvent.TYPE, 
                         new SourceDocumentSavedEvent.Handler() { 
         @Override
         public void onSourceDocumentSaved(SourceDocumentSavedEvent event)
         {
            if (currentState_ != null && currentState_.isAuthorMode())
            {
               if (event.getSourceFile().getPath().startsWith(
                                          currentState_.getDirectory()))
                  view_.refresh(false);
            }
            
         }
      });
      
      initPresentationCallbacks();
   }
   
   public void initialize(PresentationState state)
   {
      if (state.getSlideIndex() == 0)
         view_.bringToFront();
      
      init(state);
   }
   
   public void onShowPresentationPane(ShowPresentationPaneEvent event)
   {
      // if this isn't google chrome then show an error
      if (!BrowseCap.isChrome())
      {
         globalDisplay_.showErrorMessage(
               "Browser Not Supported",
               "RStudio presentations are currently only supported " +
               "in Google Chrome");
         return;
      }
      
      // always reload
      eventBus_.fireEvent(new ReloadEvent());
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
   void onRefreshPresentation()
   {
      boolean resetAnchor = Event.getCurrentEvent().getShiftKey();
      view_.refresh(resetAnchor);
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
      ProgressIndicator progress = new GlobalProgressDelayer(
            globalDisplay_,
            200,
            "Closing Presentation...").getIndicator();
      
      server_.closePresentationPane(new VoidServerRequestCallback(progress) {
         @Override
         public void onSuccess()
         {
            eventBus_.fireEvent(new ReloadEvent());
         }
      });
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
      if (currentState_.getSlideIndex() != 0)
         url = url + "#/" + currentState_.getSlideIndex();
      return url;
   }
   
   private boolean isPresentationActive()
   {
      return (currentState_ != null) && 
             (currentState_.isActive())&& 
             view_.hasSlides();
   }
   
   private void onPresentationSlideChanged(final int index, 
                                           final JavaScriptObject jsCmds)
   {  
      // note the slide index and save it
      currentState_.setSlideIndex(index);
      indexPersister_.setIndex(index);
      
      // set the title if we can (defensive code for dom timing issues)
      if (currentSlides_ != null && currentSlides_.length() > index)
         view_.getSlideMenu().setCaption(currentSlides_.get(index).getTitle());
      
      // execute commands if we stay on the slide for > 750ms
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
      }.schedule(750);  
   }
   
   private void initPresentationSlideList(JavaScriptObject jsSlides)
   {
      // record current slides
      currentSlides_ = jsSlides.cast();
      
      // reset the slides menu
      SlideMenu slideMenu = view_.getSlideMenu();
      slideMenu.clear(); 
      boolean inSection = false;
      for (int i=0; i<currentSlides_.length(); i++)
      {
         // get slide
         SlideInfo slideInfo = currentSlides_.get(i);
         
         // if there is no title then ignore the slide
         if (slideInfo.getTitle().trim().length() == 0)
            continue;
         
         // if this slide denotes a section then we are not in 
         // a section for this menu item
         if (slideInfo.isSection())
            inSection = false;
         
         // build html
         SafeHtmlBuilder menuHtml = new SafeHtmlBuilder();
         if (inSection)
            menuHtml.appendHtmlConstant("&nbsp;&nbsp;&nbsp;");
         menuHtml.appendEscaped(slideInfo.getTitle());
         
         final int index = i;
         slideMenu.addItem(new MenuItem(menuHtml.toSafeHtml(),
                                        new Command() {
            @Override
            public void execute()
            {
               view_.slide(index); 
            }
         }));
         
         if (slideInfo.isSection())
            inSection = true;
      }  
   }
   
   private final native void initPresentationCallbacks() /*-{
      var thiz = this;
      $wnd.presentationSlideChanged = $entry(function(index, cmds) {
         thiz.@org.rstudio.studio.client.workbench.views.presentation.Presentation::onPresentationSlideChanged(ILcom/google/gwt/core/client/JavaScriptObject;)(index, cmds);
      });
      $wnd.dispatchPresentationCommand = $entry(function(cmd) {
         thiz.@org.rstudio.studio.client.workbench.views.presentation.Presentation::dispatchCommand(Lcom/google/gwt/core/client/JavaScriptObject;)(cmd);
      });
      $wnd.initPresentationSlideList = $entry(function(slides) {
         thiz.@org.rstudio.studio.client.workbench.views.presentation.Presentation::initPresentationSlideList(Lcom/google/gwt/core/client/JavaScriptObject;)(slides);
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
   
   private void dispatchCommand(JavaScriptObject jsCommand)
   {
      // cast
      PresentationCommand command = jsCommand.cast();
      
      // crack parameters
      String param1 = null, param2 = null;
      String params = command.getParams();
      if (params.length() > 0)
      {
         // find the first space and split on that
         int spaceLoc = params.indexOf(' ');
         if (spaceLoc == -1)
         {
            param1 = params;
         }
         else
         {
            param1 = params.substring(0, spaceLoc);
            param2 = params.substring(spaceLoc+1);
         } 
      }
      
      String cmdName = command.getName().toLowerCase();
            
      if (cmdName.equals("help-doc"))
         performHelpDocCommand(param1, param2);
      else if (cmdName.equals("help-topic"))
         performHelpTopicCommand(param1, param2);
      else if (cmdName.equals("source"))
         performSourceCommand(param1, param2);
      else if (cmdName.equals("console"))
         performConsoleCommand(params);
      else if (cmdName.equals("console-input"))
         performConsoleInputCommand(params);
      else if (cmdName.equals("execute"))
         performExecuteCommand(params);
      else if (cmdName.equals("pause"))
         performPauseCommand();
      else 
      {
         globalDisplay_.showErrorMessage(
                        "Unknown Presentation Command", 
                        command.getName() + ": " + command.getParams());
      }
   }
   
   private void performHelpDocCommand(String param1, String param2)
   {
      if (param1 != null)
      {
         String docFile = getPresentationPath(param1);
         String url = "help/presentation/?file=" + URL.encodeQueryString(docFile);
         eventBus_.fireEvent(new ShowHelpEvent(url));  
      }
   }
   
   private void performHelpTopicCommand(String param1, String param2)
   {
      // split on :: if it's there
      if (param1 != null)
      {
         String topic = param1;
         String packageName = null;
         int delimLoc = param1.indexOf("::");
         if (delimLoc != -1)
         {
            packageName = param1.substring(0, delimLoc);
            topic = param1.substring(delimLoc+2);
         }
         
         server_.showHelpTopic(topic, packageName);
      }
   }
   
   private void performSourceCommand(String param1, String param2)
   {   
      if (param1 != null)
      {
         // get filename and type
         FileSystemItem file = FileSystemItem.createFile(
                                                  getPresentationPath(param1));
         TextFileType fileType = fileTypeRegistry_.getTextTypeForFile(file); 
         
         // check for a file position and/or pattern
         FilePosition pos = null;
         String pattern = null;
         if (param2 != null)
         {
            if (param2.length() > 2 && 
                param2.startsWith("/") && param2.endsWith("/"))
            {
               pattern = param2.substring(1, param2.length()-1);
            }
            else
            {
               int line = StringUtil.parseInt(param2, 0);
               if (line > 0)
                  pos = FilePosition.create(line, 1);
            }
         }
         
         // dispatch
         eventBus_.fireEvent(new OpenPresentationSourceFileEvent(file, 
                                                             fileType,
                                                             pos,
                                                             pattern));
      }  
   }
   
   private void performConsoleCommand(String params)
   {  
      String[] cmds = params.split(",");
      for (String cmd : cmds)
      {         
         cmd = cmd.trim();
         if (cmd.equals("maximize"))
            commands_.maximizeConsole().execute();
         else if (cmd.equals("clear"))
            commands_.consoleClear().execute();
         else
            globalDisplay_.showErrorMessage("Unknown Console Directive", cmd);
      }
   }
   
   private void performPauseCommand()
   {
      view_.pauseMedia();
   }
   
   private void performConsoleInputCommand(String params)
   {
      eventBus_.fireEvent(new SendToConsoleEvent(params, true, false, true));
   }
   
   private void performExecuteCommand(String params)
   {
      server_.presentationExecuteCode(params, new VoidServerRequestCallback());
   }
   
   
   private String getPresentationPath(String file)
   {
      FileSystemItem presentationDir = FileSystemItem.createDir(
                                                currentState_.getDirectory());
      return presentationDir.completePath(file);   
   }
   
   private final Display view_ ; 
   private final PresentationServerOperations server_;
   private final GlobalDisplay globalDisplay_;
   private final EventBus eventBus_;
   private final Commands commands_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final Session session_;
   private PresentationState currentState_ = null;
   private JsArray<SlideInfo> currentSlides_ = null;
   private boolean usingRmd_ = false;
}
