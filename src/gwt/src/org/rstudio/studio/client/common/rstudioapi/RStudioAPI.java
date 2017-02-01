/*
 * RStudioAPI.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.studio.client.common.rstudioapi;

import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.MessageDisplay.PromptWithOptionResult;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.rstudioapi.events.RStudioAPIShowDialogEvent;
import org.rstudio.studio.client.common.rstudioapi.model.RStudioAPIServerOperations;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.rstudio.studio.client.server.Void;

@Singleton
public class RStudioAPI implements RStudioAPIShowDialogEvent.Handler
{
   public RStudioAPI()
   {
   }

   @Inject
   private void initialize(EventBus events,
                           GlobalDisplay globalDisplay,
                           RStudioAPIServerOperations server)
   {
      events_ = events;
      globalDisplay_ = globalDisplay;
      server_ = server;

      events_.addHandler(RStudioAPIShowDialogEvent.TYPE, this);
   }
   
   public interface Styles extends CssResource
   {
      String textInfoWidget();
      String installLink();
   }
   
   public interface Resources extends ClientBundle
   {
      @Source("RStudioAPI.css")
      Styles styles();
   }
   
   public static Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }

   private void showDialog(String caption, 
                           String message,
                           final String url)
   {
      VerticalPanel verticalPanel = new VerticalPanel();
      verticalPanel.addStyleName(RES.styles().textInfoWidget());

      SafeHtml safeMsg = DialogHtmlSanitizer.sanitizeHtml(message);
      HTML msg = new HTML(safeMsg.asString());
      msg.setWidth("100%");
      verticalPanel.add(msg);
      
      if (!StringUtil.isNullOrEmpty(url))
      {
         HyperlinkLabel link = new HyperlinkLabel(url, 
               new ClickHandler() {
            @Override
            public void onClick(ClickEvent event)
            {
               RStudioGinjector.INSTANCE.getGlobalDisplay()
                  .openWindow(url);
            }

         });
         link.addStyleName(RES.styles().installLink());
         verticalPanel.add(link);
      }
      
      MessageDialog dlg = new MessageDialog(MessageDialog.INFO,
            caption,
            verticalPanel
            );

      dlg.addButton("OK", new Operation() {
         @Override
         public void execute()
         {
            server_.showDialogCompleted(null, false, new SimpleRequestCallback<Void>());
         }
      }, true, false);
      dlg.showModal();
   }

   private void showPrompt(String title, 
                           String message,
                           String prompt)
   {
      if (StringUtil.isNullOrEmpty(prompt)) prompt = null;

      globalDisplay_.promptForTextWithOption(
         title, 
         message, 
         prompt, 
         false, 
         null, 
         false, 
         new ProgressOperationWithInput<PromptWithOptionResult>() {
            @Override
            public void execute(PromptWithOptionResult input,
                                ProgressIndicator indicator)
            {
               indicator.onCompleted();
               
               server_.showDialogCompleted(input.input, false, new SimpleRequestCallback<Void>());
            }        
         }, 
         new Operation() {
            @Override
            public void execute()
            {
               server_.showDialogCompleted(null, false, new SimpleRequestCallback<Void>());
            }
         });
   }

   private void showQuestion(String title,
                             String message,
                             String ok,
                             String cancel)
   {
      if (StringUtil.isNullOrEmpty(ok)) ok = "OK";
      if (StringUtil.isNullOrEmpty(cancel)) cancel = "Cancel";

      globalDisplay_.showYesNoMessage(
         MessageDialog.QUESTION, 
         title,
         message,
         false,
         new Operation() {
            public void execute() {
               server_.showDialogCompleted(null, true, new SimpleRequestCallback<Void>());
            }
         },
         new Operation() {
            public void execute() {
               server_.showDialogCompleted(null, false, new SimpleRequestCallback<Void>());
            }
         },
         new Operation() {
            public void execute() {
               server_.showDialogCompleted(null, false, new SimpleRequestCallback<Void>());
            }
         },
         ok,
         cancel,
         true);
   }

   @Override
   public void onRStudioAPIShowDialogEvent(RStudioAPIShowDialogEvent event)
   {
      if (event.getPrompt()) {
         showPrompt(
            event.getTitle(), 
            event.getMessage(),
            event.getPromptDefault());
      } else if (event.getDialogIcon() == MessageDisplay.MSG_QUESTION) {
         showQuestion(
            event.getTitle(), 
            event.getMessage(),
            event.getOK(),
            event.getCancel());
      } else {
         showDialog(
            event.getTitle(), 
            event.getMessage(),
            event.getUrl());
      }
   }

   private EventBus events_;
   private GlobalDisplay globalDisplay_;
   private RStudioAPIServerOperations server_;
}
