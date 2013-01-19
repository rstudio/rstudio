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

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.inject.Inject;

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

import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;

import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeEvent;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeHandler;
import org.rstudio.studio.client.workbench.views.learning.events.ShowLearningPaneEvent;
import org.rstudio.studio.client.workbench.views.learning.model.LearningServerOperations;
import org.rstudio.studio.client.workbench.views.learning.model.LearningState;

public class LearningPresenter extends BasePresenter 
{
   public interface Binder extends CommandBinder<Commands, LearningPresenter> {}
   
   public interface Display extends WorkbenchView
   {
      void load(String url, LearningState state);
      void next();
      void prev();
      void refresh(boolean resetAnchor);
   }
   
   @Inject
   public LearningPresenter(Display display, 
                            LearningServerOperations server,
                            GlobalDisplay globalDisplay,
                            EventBus eventBus,
                            Session session,
                            Binder binder,
                            Commands commands)
   {
      super(display);
      view_ = display;
      server_ = server;
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
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
               if (path.startsWith(currentState_.getDirectory()))
               {
                  if (fsi.getName().equals("slides.md"))
                  {
                     view_.refresh(false);
                  }
               }
            }
         }
      });
      
      initLearningCallbacks();
   }
   
   public void initialize(LearningState learningState)
   {
      init(learningState);
   }
   
   public void onShowLearningPane(ShowLearningPaneEvent event)
   {
      // if the learning pane wasn't previously shown in this 
      // session then reload
      if (!session_.getSessionInfo().getLearningState().isActive())
         eventBus_.fireEvent(new ReloadEvent());
      else
         init(event.getLearningState());
   }
   
   @Handler
   void onRefreshLearning()
   {
      boolean resetAnchor = Event.getCurrentEvent().getShiftKey();
      view_.refresh(resetAnchor);
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
   
   private void onLearningSlideChanged(int index)
   {
      lastSlideIndex_ = index;
      saveIndexCommand_.nudge();
   }
   
   public final native void initLearningCallbacks() /*-{
  
      var thiz = this;
      $wnd.learningSlideChanged = function(index) {
         thiz.@org.rstudio.studio.client.workbench.views.learning.LearningPresenter::onLearningSlideChanged(I)(index);
      };
      $wnd.learningKeydown = function(e) {
         thiz.@org.rstudio.studio.client.workbench.views.learning.LearningPresenter::handleKeyDown(Lcom/google/gwt/dom/client/NativeEvent;)(e);
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
   
   
   private final Display view_ ; 
   private final LearningServerOperations server_;
   private final GlobalDisplay globalDisplay_;
   private final EventBus eventBus_;
   private final Session session_;
   private int lastSlideIndex_ = 0;
   private LearningState currentState_ = null;
   
}
