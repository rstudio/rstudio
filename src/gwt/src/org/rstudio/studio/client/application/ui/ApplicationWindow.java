/*
 * ApplicationWindow.java
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

package org.rstudio.studio.client.application.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.application.ApplicationView;
import org.rstudio.studio.client.application.ui.appended.ApplicationEndedPopupPanel;
import org.rstudio.studio.client.application.ui.serializationprogress.ApplicationSerializationProgress;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.codesearch.CodeSearch;
import org.rstudio.studio.client.workbench.codesearch.ui.CodeSearchDialog;

@Singleton
public class ApplicationWindow extends Composite 
                               implements ApplicationView, 
                                          RequiresResize,
                                          ProvidesResize
{
   @Inject
   public ApplicationWindow(ApplicationHeader applicationHeader,
                            GlobalDisplay globalDisplay,
                            Provider<CodeSearch> pCodeSearch)
   {
      globalDisplay_ = globalDisplay;
      pCodeSearch_ = pCodeSearch;
      
      // occupy full client area of the window
      Window.enableScrolling(false);
      Window.setMargin("0px");
      
      // app ui contained within a vertical panel
      applicationPanel_ = new LayoutPanel();
    
      // header bar
      applicationHeader_ = applicationHeader;
      Widget applicationHeaderWidget = applicationHeader_.asWidget();
      applicationHeaderWidget.setWidth("100%");
      applicationPanel_.add(applicationHeader_);
      updateHeaderTopBottom();
      applicationHeaderWidget.setVisible(false);

      // main view container
      initWidget(applicationPanel_);
   }
      
   public void showToolbar(boolean showToolbar)
   {
      applicationHeader_.showToolbar(showToolbar);
      updateHeaderTopBottom();
      updateWorkbenchTopBottom();
      applicationPanel_.forceLayout();  
   }
   
   public void performGoToFunction()
   {
      if (applicationHeader_.isToolbarVisible())
      {
         applicationHeader_.focusGoToFunction();
      }
      else
      {
         new CodeSearchDialog(pCodeSearch_).showModal();  
      }
   }
      
   public void showApplicationAgreement(String title,
                                        String contents,
                                        Operation doNotAcceptOperation,
                                        Operation acceptOperation)
   {
      new ApplicationAgreementDialog(title,
                                   contents,
                                   doNotAcceptOperation,
                                   acceptOperation).showModal();
   }
   
   public Widget getWidget()
   {
      return this ;
   }
   
   public void showApplicationQuit()
   {
      ApplicationEndedPopupPanel.showQuit();
   }
   
   public void showApplicationSuicide(String reason)
   {
      ApplicationEndedPopupPanel.showSuicide(reason);
   }
   
   public void showApplicationDisconnected()
   {
      ApplicationEndedPopupPanel.showDisconnected();
   }
   
   public void showApplicationOffline()
   {
      ApplicationEndedPopupPanel.showOffline();
   }
   
   public void showApplicationUpdateRequired()
   {
      globalDisplay_.showMessage(
            GlobalDisplay.MSG_INFO,
            "Application Updated",
            "An updated version of RStudio is available. Your browser will " +
            "now be refreshed with the new version. All current work and data " +
            "will be preserved during the update.",
            new Operation() {
               public void execute()
               {
                  Window.Location.reload();
               }

            });
   }
      
   public void showWorkbenchView(Widget workbenchScreen)
   {
      workbenchScreen_ = workbenchScreen;

      applicationHeader_.asWidget().setVisible(true);
      applicationPanel_.add(workbenchScreen_);
      updateWorkbenchTopBottom();
      applicationPanel_.setWidgetLeftRight(workbenchScreen_,
                                           COMPONENT_SPACING,
                                           Style.Unit.PX,
                                           COMPONENT_SPACING,
                                           Style.Unit.PX);
   }

   public void showWarning(boolean severe, String message)
   {
      if (warningBar_ == null)
      {
         warningBar_ = new WarningBar();
         warningBar_.addCloseHandler(new CloseHandler<WarningBar>()
         {
            public void onClose(CloseEvent<WarningBar> warningBarCloseEvent)
            {
               hideWarning();
            }
         });
         applicationPanel_.add(warningBar_);
         applicationPanel_.setWidgetBottomHeight(warningBar_,
                                                 COMPONENT_SPACING,
                                                 Unit.PX,
                                                 warningBar_.getHeight(),
                                                 Unit.PX);
         applicationPanel_.setWidgetLeftRight(warningBar_,
                                              COMPONENT_SPACING, Unit.PX,
                                              COMPONENT_SPACING, Unit.PX);

         workbenchBottom_ = COMPONENT_SPACING*2 + warningBar_.getHeight();
         if (workbenchScreen_ != null)
            updateWorkbenchTopBottom();

         applicationPanel_.animate(250);
      }
      warningBar_.setSeverity(severe);
      warningBar_.setText(message);
   }

   private void updateHeaderTopBottom()
   {
      int headerHeight = applicationHeader_.getPreferredHeight();
      applicationPanel_.setWidgetTopHeight(applicationHeader_,
                                           0,
                                           Style.Unit.PX,
                                           headerHeight,
                                           Style.Unit.PX);
      applicationPanel_.setWidgetLeftRight(applicationHeader_,
                                           0,
                                           Style.Unit.PX,
                                           0,
                                           Style.Unit.PX);
   }
   
   private void updateWorkbenchTopBottom()
   {
      applicationPanel_.setWidgetTopBottom(
            workbenchScreen_,
            applicationHeader_.getPreferredHeight(),
            Unit.PX,
            workbenchBottom_,
            Unit.PX);
   }

   public void hideWarning()
   {
      if (warningBar_ != null)
      {
         applicationPanel_.remove(warningBar_);
         warningBar_ = null;

         workbenchBottom_ = COMPONENT_SPACING;
         if (workbenchScreen_ != null)
            updateWorkbenchTopBottom();

         applicationPanel_.animate(250);
      }
   }

   public void showSessionAbendWarning()
   {
      globalDisplay_.showErrorMessage(
            "R Session Error",
            "The previous R session was abnormally terminated due to " +
            "an unexpected crash.\n\n" +
            "You may have lost workspace data as a result of this crash.");
   }
   
   public void showSerializationProgress(String msg, 
                                         boolean modal, 
                                         int delayMs,
                                         int timeoutMs)
   {
      // hide any existing progress
      hideSerializationProgress();
      
      // create and show progress
      activeSerializationProgress_ = 
                    new ApplicationSerializationProgress(msg, modal, delayMs);
      
      // implement timeout for *this* serialization progress instance if 
      // requested (check to ensure the same instance because another 
      // serialization progress could occur in the meantime and we don't 
      // want to hide it)
      if (timeoutMs > 0)
      {
         final ApplicationSerializationProgress timeoutSerializationProgress =
                                                   activeSerializationProgress_;
         new Timer() {
            @Override
            public void run()
            {
               if (timeoutSerializationProgress == activeSerializationProgress_)
                  hideSerializationProgress();
            }
         }.schedule(timeoutMs);     
      }
   }
   
   public void hideSerializationProgress()
   {
      if (activeSerializationProgress_ != null)
      {
         activeSerializationProgress_.hide();
         activeSerializationProgress_ = null;
      }
   }
  
   public void onResize()
   {
      applicationPanel_.onResize();
   }
   
   // main applilcation UI components
   private LayoutPanel applicationPanel_ ;
   private ApplicationHeader applicationHeader_ ;

   // active serialization progress message
   private ApplicationSerializationProgress activeSerializationProgress_;
  
   

   private static final int COMPONENT_SPACING = 6;
   private Widget workbenchScreen_;
   private WarningBar warningBar_;
   private int workbenchBottom_ = COMPONENT_SPACING;
   private final GlobalDisplay globalDisplay_;
   private final Provider<CodeSearch> pCodeSearch_;
}
