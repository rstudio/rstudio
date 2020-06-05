/*
 * ApplicationWindow.java
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

package org.rstudio.studio.client.application.ui;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.widget.AriaLiveStatusWidget;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.application.ApplicationView;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Severity;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Timing;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.ui.appended.ApplicationEndedPopupPanel;
import org.rstudio.studio.client.application.ui.serializationprogress.ApplicationSerializationProgress;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.palette.CommandPaletteLauncher;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

@Singleton
public class ApplicationWindow extends Composite 
                               implements ApplicationView, 
                                          RequiresResize,
                                          ProvidesResize
{
   @Inject
   public ApplicationWindow(ApplicationHeader applicationHeader,
                            GlobalDisplay globalDisplay,
                            Provider<UserPrefs> pPrefs,
                            EventBus events,
                            Provider<WarningBar> pWarningBar,
                            AriaLiveService ariaLive,
                            CodeSearchLauncher launcher,
                            CommandPaletteLauncher paletteLauncher)
   {
      globalDisplay_ = globalDisplay;
      events_ = events;
      pPrefs_ = pPrefs;
      pWarningBar_ = pWarningBar;
      ariaLive_ = ariaLive;

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

      // aria-live status announcements
      ariaLiveStatusWidget_ = new AriaLiveStatusWidget();
      applicationPanel_.add(ariaLiveStatusWidget_);
      A11y.setVisuallyHidden(applicationPanel_.getWidgetContainerElement(ariaLiveStatusWidget_));

      // main view container
      initWidget(applicationPanel_);
   }

   @Override
   public void showToolbar(boolean showToolbar, boolean announce)
   {
      boolean currentVisibility = isToolbarShowing();
      applicationHeader_.showToolbar(showToolbar);
      updateHeaderTopBottom();
      updateWorkbenchTopBottom();
      applicationPanel_.forceLayout();  
      if (announce && showToolbar != currentVisibility)
         ariaLive_.announce(AriaLiveService.TOOLBAR_VISIBILITY,
               showToolbar ? "Main toolbar visible" : "Main toolbar hidden",
               Timing.IMMEDIATE, Severity.STATUS);
   }
   
   @Override
   public boolean isToolbarShowing()
   {
      return applicationHeader_.isToolbarVisible();
   }
   
   @Override
   public void focusToolbar()
   {
      if (!isToolbarShowing())
      {
         ariaLive_.announce(AriaLiveService.TOOLBAR_VISIBILITY,
               "Toolbar hidden, unable to focus.",
               Timing.IMMEDIATE, Severity.STATUS);
         return;
      }
      applicationHeader_.focusToolbar();
   }

   public Widget getWidget()
   {
      return this;
   }
   
   @Override
   public void showApplicationQuit()
   {
      ApplicationEndedPopupPanel.showQuit();
   }
   
   @Override
   public void showApplicationMultiSessionQuit()
   {
      ApplicationEndedPopupPanel.showMultiSessionQuit();
   }
   
   @Override
   public void showApplicationSuicide(String reason)
   {
      ApplicationEndedPopupPanel.showSuicide(reason);
   }
   
   @Override
   public void showApplicationDisconnected()
   {
      ApplicationEndedPopupPanel.showDisconnected();
   }
   
   @Override
   public void showApplicationOffline()
   {
      ApplicationEndedPopupPanel.showOffline();
   }
   
   @Override
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
      
   @Override
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

   private void showWarning(boolean severe, String message, boolean showLicenseButton)
   {
      if (warningBar_ == null)
      {
         warningBar_ = pWarningBar_.get();
         Roles.getContentinfoRole().set(warningBar_.getElement());
         Roles.getContentinfoRole().setAriaLabelProperty(warningBar_.getElement(), "Warning bar");
         warningBar_.addCloseHandler(warningBarCloseEvent -> hideWarning());
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
      warningBar_.showLicenseButton(showLicenseButton);
   }
   
   @Override
   public void showLicenseWarning(boolean severe, String message)
   {
      showWarning(severe, message, true);
      
   }

   @Override
   public void showWarning(boolean severe, String message)
   {
      showWarning(severe, message, false);
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

   @Override
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

   @Override
   public void showSessionAbendWarning()
   {
      globalDisplay_.showErrorMessage(
            "R Session Error",
            "The previous R session was abnormally terminated due to " +
            "an unexpected crash.\n\n" +
            "You may have lost workspace data as a result of this crash.");
   }
   
   @Override
   public void reportStatus(String message, int delayMs, Severity severity)
   {
      ariaLiveStatusWidget_.reportStatus(message, delayMs, severity);
   }

   @Override
   public void showSerializationProgress(String msg, 
                                         boolean modal, 
                                         int delayMs,
                                         int timeoutMs)
   {
      // hide any existing progress
      hideSerializationProgress();
      
      // create and show progress
      activeSerializationProgress_ = 
                    new ApplicationSerializationProgress(msg, modal, delayMs,
                          !ariaLive_.isDisabled(AriaLiveService.SESSION_STATE));
      
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
   
   @Override
   public void hideSerializationProgress()
   {
      if (activeSerializationProgress_ != null)
      {
         activeSerializationProgress_.hide();
         activeSerializationProgress_ = null;
      }
   }
  
   @Override
   public void onResize()
   {
      applicationPanel_.onResize();
   }
   
   // main application UI components
   private LayoutPanel applicationPanel_;
   private ApplicationHeader applicationHeader_;

   // active serialization progress message
   private ApplicationSerializationProgress activeSerializationProgress_;

   private static final int COMPONENT_SPACING = 6;
   private Widget workbenchScreen_;
   private WarningBar warningBar_;
   private final AriaLiveStatusWidget ariaLiveStatusWidget_;
   private int workbenchBottom_ = COMPONENT_SPACING;
   private final GlobalDisplay globalDisplay_;
   @SuppressWarnings("unused")
   private final EventBus events_;
   @SuppressWarnings("unused")
   private final Provider<UserPrefs> pPrefs_;
   private final AriaLiveService ariaLive_;
   private final Provider<WarningBar> pWarningBar_;
}
