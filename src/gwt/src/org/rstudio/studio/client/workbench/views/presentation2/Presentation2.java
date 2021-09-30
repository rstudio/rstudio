/*
 * Presentation2.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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

package org.rstudio.studio.client.workbench.views.presentation2;

import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.quarto.model.QuartoNavigate;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.jobs.events.JobUpdatedEvent;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;
import org.rstudio.studio.client.workbench.views.jobs.model.JobConstants;
import org.rstudio.studio.client.workbench.views.presentation2.events.PresentationHashChangeEvent;
import org.rstudio.studio.client.workbench.views.presentation2.events.PresentationInitEvent;
import org.rstudio.studio.client.workbench.views.presentation2.events.PresentationPreviewEvent;
import org.rstudio.studio.client.workbench.views.presentation2.events.PresentationSlideChangeEvent;
import org.rstudio.studio.client.workbench.views.presentation2.model.RevealSlide;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;

public class Presentation2 extends BasePresenter
{
   public interface Display extends WorkbenchView
   {
      void activate();
      void navigate(String url, QuartoNavigate nav);
      void init(JsArray<RevealSlide> slides);
      void change(RevealSlide slide);
      void clear();
      
      void home();
      void next();
      void prev();
      
      void refresh();
      
      HandlerRegistration addPresentationInitHandler(PresentationInitEvent.Handler handler);
      HandlerRegistration addPresentationSlideChangeHandler(PresentationSlideChangeEvent.Handler handler);
      HandlerRegistration addPresentationHashChangeHandler(PresentationHashChangeEvent.Handler handler);
   }
   
   @Inject
   public Presentation2(Display display, 
                        Commands commands,
                        GlobalDisplay globalDisplay,
                        ApplicationServerOperations server,
                        FileTypeRegistry fileTypeRegistry,
                        EventBus eventBus)
   {
      super(display);
      display_ = display;
      commands_ = commands;
      globalDisplay_ = globalDisplay;
      server_ = server;
      fileTypeRegistry_ = fileTypeRegistry;
      
      enableCommands(false);
      
      display_.addPresentationInitHandler(event -> {
         // notify display that we are ready to rock
         display_.init(event.getSlides()); 
         
         // enable all commands
         enableCommands(true);
      });
      
      display_.addPresentationSlideChangeHandler(event -> {
         commands_.presentation2Prev().setEnabled(!event.isFirst());
         commands_.presentation2Next().setEnabled(!event.isLast());
         display_.change(event.getSlide());
      });
      
      display_.addPresentationHashChangeHandler(event -> {
         activeSlideHref_ = event.getHRef();
      });
      
      eventBus.addHandler(JobUpdatedEvent.TYPE, event -> {
         Job job = event.getData().job;
         if (activePresentation_ != null && 
             job.id == activePresentation_.getJobId() &&
             job.state != JobConstants.STATE_RUNNING)
         {
            activeUrl_ = null;
            activeSlideHref_ = null;
            activePresentation_ = null;
            enableCommands(false);
            display_.clear();
         }
      }); 
   }

   // request from server to preview a presentation
   public void onPresentationPreview(PresentationPreviewEvent event)
   {
      // get event data
      PresentationPreviewEvent.Data data = event.getData();
      String url = asApplicationUrl(data.getUrl());
      
      // activate pane
      display_.activate();
      
      // navigate (or refresh if it's the same target as what we have)
      if (!url.equals(activeUrl_))
      {
         activeUrl_ = url;
         activeSlideHref_ = url;
         activePresentation_ = data.getQuartoNavigation();
         if (Desktop.hasDesktopFrame())
            Desktop.getFrame().setPresentationUrl(activeUrl_);
         display_.navigate(activeUrl_, activePresentation_);
      }
      else
      {
         display_.refresh();
      }
   }

 
   @Handler
   void onPresentation2Home()
   {
      display_.home();
   }
   
   @Handler
   void onPresentation2Next()
   {
      display_.next();
   }
   
   @Handler
   void onPresentation2Prev()
   {
      display_.prev();
   }
   
   @Handler
   void onPresentation2Edit()
   {
      if (activePresentation_ != null)
      {
         fileTypeRegistry_.editFile(
            FileSystemItem.createFile(activePresentation_.getSourceFile())
         );
      }
   }
   
   @Handler
   void onPresentation2Print()
   {
      globalDisplay_.openWindow(activeUrl_ + "?print-pdf");
   }
   
   @Handler
   void onPresentation2ViewInBrowser()
   {
      globalDisplay_.openWindow(activeSlideHref_);
   }
   
   @Handler
   void onRefreshPresentation2()
   {
      display_.refresh();
   }
   
   private void enableCommands(boolean enable)
   {
      commands_.refreshPresentation2().setEnabled(enable);
      commands_.presentation2Home().setEnabled(enable);
      commands_.presentation2Next().setEnabled(enable);
      commands_.presentation2Prev().setEnabled(enable);
      commands_.presentation2Edit().setEnabled(enable);
      commands_.presentation2Print().setEnabled(enable);
      commands_.presentation2ViewInBrowser().setEnabled(enable);
   }
   
   private String asApplicationUrl(String url)
   {
      if (!url.startsWith("http"))
         url = server_.getApplicationURL(url);
      return url;
   }
   
   private String activeUrl_ = null;
   private String activeSlideHref_ = null;
   private QuartoNavigate activePresentation_ = null;
   
   private final Display display_;
   private final Commands commands_;
   private final GlobalDisplay globalDisplay_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final ApplicationServerOperations server_;
}
