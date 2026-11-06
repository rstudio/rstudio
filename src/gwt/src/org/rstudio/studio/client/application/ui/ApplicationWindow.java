/*
 * ApplicationWindow.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.application.ui;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.widget.AriaLiveStatusWidget;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.application.StudioClientApplicationConstants;
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
import org.rstudio.studio.client.workbench.events.PushClientStateEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleActivateEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.helper.StringStateValue;

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
                            Session session,
                            AriaLiveService ariaLive,
                            CodeSearchLauncher launcher,
                            CommandPaletteLauncher paletteLauncher)
   {
      globalDisplay_ = globalDisplay;
      events_ = events;
      pPrefs_ = pPrefs;
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

      warnings_ = new WarningBarList(
            pWarningBar,
            ariaLive,
            this::updateWarnings,
            () -> events.fireEvent(new PushClientStateEvent(true)),
            () -> events.fireEvent(new ConsoleActivateEvent(true)));
      Widget announcements = warnings_.getAnnouncementWidget();
      applicationPanel_.add(announcements);
      A11y.setVisuallyHidden(applicationPanel_.getWidgetContainerElement(announcements));
      Roles.getContentinfoRole().set(warnings_.getElement());
      Roles.getContentinfoRole().setAriaLabelProperty(warnings_.getElement(), constants_.warningBarText());
      applicationPanel_.add(warnings_);
      applicationPanel_.setWidgetLeftRight(warnings_, COMPONENT_SPACING, Unit.PX, COMPONENT_SPACING, Unit.PX);
      // Hide the layer, not just the list: until a warning sizes it, the empty
      // layer fills the panel and, following the workbench, would cover it.
      applicationPanel_.setWidgetVisible(warnings_, false);

      session.withSessionInfo(info -> new StringStateValue(
            "applicationWarnings", "dismissed", ClientState.TEMPORARY, info.getClientState())
      {
         @Override
         protected void onInit(String value)
         {
            warnings_.restoreDismissedState(value);
         }

         @Override
         protected String getValue()
         {
            return warnings_.getDismissedState();
         }
      });
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
               showToolbar ? constants_.toolbarVisibleText() : constants_.toolbarHiddenText(),
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
               constants_.focusToolbarText(),
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
   public void showMemoryLimitExceeded(String status, boolean abort)
   {
      if (abort)
         ApplicationEndedPopupPanel.showMemoryLimitExceeded(status);
      else
         globalDisplay_.showErrorMessage(constants_.memoryLimitExceededCaption(),
                                         constants_.memoryLimitExceededMessage() + "\n\n" + status);
   }

   @Override
   public void showMemoryLimitWarning(String status, boolean updateOnly, boolean overLimit)
   {
      if (!updateOnly || warnings_.contains(MEMORY_WARNING))
         showWarning(MEMORY_WARNING, true,
               (overLimit ? constants_.overMemoryLimit() : constants_.approachingMemoryLimit()) + " " + status,
               null, null);
   }

   @Override
   public void showApplicationUpdateRequired()
   {
      globalDisplay_.showMessage(
            GlobalDisplay.MSG_INFO,
            constants_.applicationUpdatedCaption(),
            constants_.applicationUpdatedMessage(),
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
      applicationPanel_.insert(workbenchScreen_, applicationPanel_.getWidgetIndex(warnings_));
      Element mainEl = applicationPanel_.getWidgetContainerElement(workbenchScreen_);
      // LayoutPanel.insert orders its widgets but appends their DOM layers.
      // Keep reading and tab order consistent with the visual layout too.
      mainEl.getParentElement().insertBefore(mainEl, applicationPanel_.getWidgetContainerElement(warnings_));
      Roles.getMainRole().set(mainEl);
      Roles.getMainRole().setAriaLabelProperty(mainEl, constants_.workbenchLabel());
      updateWorkbenchTopBottom();
      applicationPanel_.setWidgetLeftRight(workbenchScreen_,
                                           COMPONENT_SPACING,
                                           Style.Unit.PX,
                                           COMPONENT_SPACING,
                                           Style.Unit.PX);
   }

   private void updateWarnings()
   {
      int height = warnings_.getPreferredHeight();
      applicationPanel_.setWidgetVisible(warnings_, height > 0);
      applicationPanel_.setWidgetBottomHeight(warnings_, COMPONENT_SPACING, Unit.PX, height, Unit.PX);
      workbenchBottom_ = height == 0 ? COMPONENT_SPACING : COMPONENT_SPACING * 2 + height;
      if (workbenchScreen_ != null)
         updateWorkbenchTopBottom();
      applicationPanel_.forceLayout();
   }

   @Override
   public void showLicenseWarning(boolean severe, String message)
   {
      if (message == null || message.isEmpty())
         warnings_.hide(LICENSE_WARNING);
      else
         warnings_.show(LICENSE_WARNING, severe, message, true, null, null);
   }

   @Override
   public void showWarning(boolean severe, String message)
   {
      showWarning(severe, message, null, null);
   }

   @Override
   public void showWarning(boolean severe, String message, String actionLabel, Command action)
   {
      showWarning(WarningBarList.messageId(message, actionLabel), severe, message, actionLabel, action);
   }

   @Override
   public void showWarning(String id, boolean severe, String message, String actionLabel, Command action)
   {
      warnings_.show(id, severe, message, false, actionLabel, action);
   }

   @Override
   public void hideWarning(String id)
   {
      warnings_.hide(id);
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
      warnings_.clear();
   }

   @Override
   public void showSessionAbendWarning()
   {
      globalDisplay_.showErrorMessage(
            constants_.rSessionErrorCaption(), constants_.previousRSessionsMessage());
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
      // reset / cancel a previous serialization operation
      resetState();

      // create and show progress
      activeSerializationProgress_ =
                    new ApplicationSerializationProgress(msg, modal, delayMs,
                          !ariaLive_.isDisabled(AriaLiveService.SESSION_STATE));
      
      // create timers for showing, hiding output
      showTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            if (activeSerializationProgress_ != null)
            {
               activeSerializationProgress_.showProgress();
            }
         }
      };
      
      hideTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            if (activeSerializationProgress_ != null)
            {
               activeSerializationProgress_.hide();
            }
         }
      };

      // start the timers
      showTimer_.schedule(delayMs);
      hideTimer_.schedule(timeoutMs);
   }

   @Override
   public void hideSerializationProgress()
   {
      resetState();
   }
   
   private void resetState()
   {
      if (activeSerializationProgress_ != null)
      {
         activeSerializationProgress_.hide();
         activeSerializationProgress_ = null;
      }
      
      if (showTimer_ != null)
      {
         showTimer_.cancel();
         showTimer_ = null;
      }
      
      if (hideTimer_ != null)
      {
         hideTimer_.cancel();
         hideTimer_ = null;
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
   private Timer showTimer_;
   private Timer hideTimer_;

   private static final int COMPONENT_SPACING = 6;
   private Widget workbenchScreen_;
   private static final String MEMORY_WARNING = "memory-limit";
   private static final String LICENSE_WARNING = "license";
   private final WarningBarList warnings_;
   private final AriaLiveStatusWidget ariaLiveStatusWidget_;
   private int workbenchBottom_ = COMPONENT_SPACING;
   private final GlobalDisplay globalDisplay_;
   @SuppressWarnings("unused")
   private final EventBus events_;
   @SuppressWarnings("unused")
   private final Provider<UserPrefs> pPrefs_;
   private final AriaLiveService ariaLive_;
   private static final StudioClientApplicationConstants constants_ = GWT.create(StudioClientApplicationConstants.class);
}
