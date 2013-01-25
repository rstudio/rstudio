/*
 * LearningPresenter.java
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
package org.rstudio.studio.client.workbench.views.learning;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.inject.Inject;

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.events.NativeKeyDownEvent;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.ReloadEvent;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.filetypes.events.OpenLearningSourceFileEvent;

import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;

import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeEvent;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeHandler;
import org.rstudio.studio.client.workbench.views.help.events.ShowHelpEvent;
import org.rstudio.studio.client.workbench.views.learning.events.ShowLearningPaneEvent;
import org.rstudio.studio.client.workbench.views.learning.model.LearningCommand;
import org.rstudio.studio.client.workbench.views.learning.model.LearningServerOperations;
import org.rstudio.studio.client.workbench.views.learning.model.LearningState;

public class LearningPresenter extends BasePresenter 
{
   public interface Binder extends CommandBinder<Commands, LearningPresenter> {}
   
   public interface Display extends WorkbenchView
   {
      void load(String url, LearningState state);
      boolean hasSlides();
      void next();
      void prev();
      void refresh(boolean resetAnchor);
   }
   
   @Inject
   public LearningPresenter(Display display, 
                            LearningServerOperations server,
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
      
      eventBus.addHandler(FileChangeEvent.TYPE, new FileChangeHandler() {
         @Override
         public void onFileChange(FileChangeEvent event)
         {  
            if (currentState_ != null)
            {
               FileSystemItem fsi = event.getFileChange().getFile(); 
               String path = fsi.getPath();
               if (path.startsWith(currentState_.getDirectory()) &&
                   fsi.mimeType().equals("text/x-markdown"))
               {
                  refreshCommand_.nudge();
               }
            }
         }
      });
      
      initLearningCallbacks();
   }
   
   public void initialize(LearningState learningState)
   {
      if (learningState.getSlideIndex() == 0)
         view_.bringToFront();
      
      init(learningState);
   }
   
   public void onShowLearningPane(ShowLearningPaneEvent event)
   {
      // if the learning pane wasn't previously shown in this 
      // session then reload
      if (!session_.getSessionInfo().getLearningState().isActive())
         eventBus_.fireEvent(new ReloadEvent());
      else
      {
         view_.bringToFront();
         init(event.getLearningState());
      }
   }
   
   @Handler
   void onRefreshLearning()
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
            "Closing Learning Tab...").getIndicator();
      
      server_.closeLearningPane(new VoidServerRequestCallback(progress) {
         @Override
         public void onSuccess()
         {
            eventBus_.fireEvent(new ReloadEvent());
         }
      });
   }
   
   private void init(LearningState state)
   {
      currentState_ = state;
      
      String url = server_.getApplicationURL("learning/");
      if (currentState_.getSlideIndex() != 0)
         url = url + "#/" + currentState_.getSlideIndex();
      
      view_.load(url, state);
   }
   
   private void onLearningSlideChanged(int index, JavaScriptObject jsCmds)
   {
      // record index
      lastSlideIndex_ = index;
      saveIndexCommand_.nudge();
      
      // execute commands
      JsArray<JavaScriptObject> cmds = jsCmds.cast();
      for (int i=0; i<cmds.length(); i++)
         dispatchCommand(cmds.get(i));
   }
   
   public final native void initLearningCallbacks() /*-{
  
      var thiz = this;
      $wnd.learningSlideChanged = function(index, cmds) {
         thiz.@org.rstudio.studio.client.workbench.views.learning.LearningPresenter::onLearningSlideChanged(ILcom/google/gwt/core/client/JavaScriptObject;)(index, cmds);
      };
      $wnd.learningKeydown = function(e) {
         thiz.@org.rstudio.studio.client.workbench.views.learning.LearningPresenter::handleKeyDown(Lcom/google/gwt/dom/client/NativeEvent;)(e);
      };
      $wnd.dispatchLearningCommand = function(cmd) {
         thiz.@org.rstudio.studio.client.workbench.views.learning.LearningPresenter::dispatchCommand(Lcom/google/gwt/core/client/JavaScriptObject;)(cmd);
      };
   }-*/;

   private void handleKeyDown(NativeEvent e)
   {  
      NativeKeyDownEvent evt = new NativeKeyDownEvent(e);
      ShortcutManager.INSTANCE.onKeyDown(evt);
      if (evt.isCanceled())
      {
         e.preventDefault();
         e.stopPropagation();
         
         // since this is a shortcut handled by the main window
         // we set focus to it
         WindowEx.get().focus();
      } 
   }
   
   
   TimeBufferedCommand saveIndexCommand_ = new TimeBufferedCommand(500)
   {
      @Override
      protected void performAction(boolean shouldSchedulePassive)
      {
         server_.setLearningSlideIndex(lastSlideIndex_, 
                                       new VoidServerRequestCallback());
      }
   };
   
   TimeBufferedCommand refreshCommand_ = new TimeBufferedCommand(500)
   {
      @Override
      protected void performAction(boolean shouldSchedulePassive)
      {
         view_.refresh(false);
      }
   };
   
   private void dispatchCommand(JavaScriptObject jsCommand)
   {
      // cast
      LearningCommand command = jsCommand.cast();
      
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
      else 
      {
         globalDisplay_.showErrorMessage(
                        "Unknown Learning Command", 
                        command.getName() + ": " + command.getParams());
      }
   }
   
   private void performHelpDocCommand(String param1, String param2)
   {
      if (param1 != null)
      {
         String docFile = getLearningPath(param1);
         String url = "help/learning/?file=" + URL.encodeQueryString(docFile);
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
                                                  getLearningPath(param1));
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
         eventBus_.fireEvent(new OpenLearningSourceFileEvent(file, 
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
   
   private void performConsoleInputCommand(String params)
   {
      eventBus_.fireEvent(new SendToConsoleEvent(params, true, false, true));
   }
   
   
   private String getLearningPath(String file)
   {
      FileSystemItem learningDir = FileSystemItem.createDir(
                                                currentState_.getDirectory());
      return learningDir.completePath(file);   
   }
   
   private final Display view_ ; 
   private final LearningServerOperations server_;
   private final GlobalDisplay globalDisplay_;
   private final EventBus eventBus_;
   private final Commands commands_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final Session session_;
   private int lastSlideIndex_ = 0;
   private LearningState currentState_ = null;
   
}
