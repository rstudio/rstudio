/*
 * ChatPane.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.chat;

import org.rstudio.core.client.theme.ThemeFonts;
import org.rstudio.core.client.widget.RStudioThemedFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.LocaleCookie;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.chat.server.ChatServerOperations;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ChatPane
      extends WorkbenchPane
      implements ChatPresenter.Display
{
   private enum ContentType
   {
      HTML,
      URL
   }

   @Inject
   protected ChatPane(GlobalDisplay globalDisplay,
                      EventBus events,
                      Commands commands,
                      Session session,
                      ChatServerOperations server)
   {
      super(constants_.chatTitle(), events);

      globalDisplay_ = globalDisplay;
      commands_ = commands;
      session_ = session;
      server_ = server;

      ensureWidget();
   }

   @Override
   protected Widget createMainWidget()
   {
      mainPanel_ = new LayoutPanel();
      mainPanel_.addStyleName("ace_editor_theme");

      // Create update notification bar
      updateNotificationPanel_ = new FlowPanel();
      updateNotificationPanel_.setVisible(false);
      updateNotificationPanel_.setStyleName("chatUpdateNotification");
      updateNotificationPanel_.getElement().getStyle().setBackgroundColor("#e3f2fd");
      updateNotificationPanel_.getElement().getStyle().setPadding(10, Unit.PX);
      updateNotificationPanel_.getElement().getStyle().setProperty("borderBottom", "1px solid #90caf9");

      updateMessageLabel_ = new HTML();
      updateButtonPanel_ = new HorizontalPanel();
      updateButtonPanel_.getElement().getStyle().setMarginLeft(10, Unit.PX);

      HorizontalPanel notificationContent = new HorizontalPanel();
      notificationContent.add(updateMessageLabel_);
      notificationContent.add(updateButtonPanel_);
      updateNotificationPanel_.add(notificationContent);

      frame_ = new RStudioThemedFrame(constants_.chatTitle());
      frame_.setSize("100%", "100%");

      // Store initial message to show after frame loads
      pendingMessage_ = generateMessageHTML(constants_.checkingInstallationMessage());

      // Add update notification and frame to layout
      mainPanel_.add(updateNotificationPanel_);
      mainPanel_.add(frame_);

      // Position update notification at top
      mainPanel_.setWidgetTopHeight(updateNotificationPanel_, 0, Unit.PX, 40, Unit.PX);
      mainPanel_.setWidgetLeftRight(updateNotificationPanel_, 0, Unit.PX, 0, Unit.PX);

      // Position frame below notification (initially at 0 since notification is hidden)
      updateFrameLayout();

      return mainPanel_;
   }

   private void updateFrameLayout()
   {
      if (updateNotificationPanel_.isVisible())
      {
         // Notification is showing, frame starts below it
         mainPanel_.setWidgetTopBottom(frame_, 40, Unit.PX, 0, Unit.PX);
      }
      else
      {
         // Notification hidden, frame takes full height
         mainPanel_.setWidgetTopBottom(frame_, 0, Unit.PX, 0, Unit.PX);
      }
      mainPanel_.setWidgetLeftRight(frame_, 0, Unit.PX, 0, Unit.PX);
   }

   /**
    * Generates centered HTML content for display in the iframe.
    *
    * @param message The message text to display
    * @return HTML string with proper styling for centered content
    */
   private String generateMessageHTML(String message)
   {
      StringBuilder html = new StringBuilder();
      html.append("<!DOCTYPE html>");
      html.append("<html lang='");
      html.append(LocaleCookie.getUiLanguage());
      html.append("'>");
      html.append("<head>");
      html.append("<meta charset='UTF-8'>");
      html.append("<style>");
      html.append("html, body {");
      html.append("  margin: 0;");
      html.append("  padding: 0;");
      html.append("  width: 100%;");
      html.append("  height: 100%;");
      html.append("  overflow: hidden;");
      html.append("}");
      html.append("body {");
      html.append("  display: flex;");
      html.append("  align-items: center;");
      html.append("  justify-content: center;");
      html.append("  font-family: ");
      html.append(ThemeFonts.getProportionalFont());
      html.append(";");
      html.append("  font-size: 12px;");
      html.append("  color: var(--rstudio-foreground, #000);");
      html.append("  background-color: var(--rstudio-background, #fff);");
      html.append("}");
      html.append(".message {");
      html.append("  text-align: center;");
      html.append("  padding: 20px;");
      html.append("}");
      html.append("</style>");
      html.append("</head>");
      html.append("<body>");
      html.append("<div class='message'>");
      html.append(message);
      html.append("</div>");
      html.append("</body>");
      html.append("</html>");

      return html.toString();
   }

   /**
    * Sets the HTML content of the iframe dynamically.
    *
    * @param frame The RStudioThemedFrame to update
    * @param html The HTML content to write to the iframe
    */
   private native void setFrameContent(RStudioThemedFrame frame, String html) /*-{
      try {
         var doc = frame.@org.rstudio.core.client.widget.RStudioFrame::getWindow()().document;
         doc.open();
         doc.write(html);
         doc.close();
      } catch (e) {
         console.error("Error setting frame content:", e);
      }
   }-*/;

   /**
    * Updates the iframe content and stores it for later refresh.
    *
    * @param html The HTML content to display
    */
   private void updateFrameContent(String html)
   {
      contentType_ = ContentType.HTML;
      currentContent_ = html;
      currentUrl_ = null;
      setFrameContent(frame_, html);
   }

   /**
    * Loads a URL in the iframe and stores it for later refresh.
    *
    * @param url The URL to load
    */
   @Override
   public void loadUrl(String url)
   {
      contentType_ = ContentType.URL;
      currentUrl_ = url;
      currentContent_ = null;
      frame_.setUrl(url);
   }

   @Override
   public void setObserver(ChatPresenter.Display.Observer observer)
   {
      observer_ = observer;
   }

   @Override
   public void setUpdateObserver(ChatPresenter.Display.UpdateObserver observer)
   {
      updateObserver_ = observer;
   }

   @Override
   public void showUpdateNotification(String newVersion)
   {
      updateMessageLabel_.setHTML("A new version (" + newVersion + ") is available.");
      updateButtonPanel_.clear();

      Button updateNowButton = new Button("Update");
      updateNowButton.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            if (updateObserver_ != null)
            {
               updateObserver_.onUpdateNow();
            }
         }
      });

      Button remindLaterButton = new Button("Ignore");
      remindLaterButton.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            if (updateObserver_ != null)
            {
               updateObserver_.onRemindLater();
            }
         }
      });

      updateButtonPanel_.add(updateNowButton);
      updateButtonPanel_.add(remindLaterButton);

      updateNotificationPanel_.setVisible(true);
      updateFrameLayout();
   }

   @Override
   public void showInstallNotification(String newVersion)
   {
      updateMessageLabel_.setHTML("Posit Assistant (" + newVersion + ") is available for install.");
      updateButtonPanel_.clear();

      // Change background to green for install (vs blue for update)
      updateNotificationPanel_.getElement().getStyle().setBackgroundColor("#e8f5e9");
      updateNotificationPanel_.getElement().getStyle().setProperty("borderBottom", "1px solid #81c784");

      Button installNowButton = new Button("Install");
      installNowButton.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            if (updateObserver_ != null)
            {
               updateObserver_.onUpdateNow();  // Reuse same callback
            }
         }
      });

      Button remindLaterButton = new Button("Ignore");
      remindLaterButton.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            if (updateObserver_ != null)
            {
               updateObserver_.onRemindLater();  // Reuse same callback
            }
         }
      });

      updateButtonPanel_.add(installNowButton);
      updateButtonPanel_.add(remindLaterButton);

      updateNotificationPanel_.setVisible(true);
      updateFrameLayout();
   }

   @Override
   public void showUpdatingStatus()
   {
      updateMessageLabel_.setHTML("Updating Posit Assistant...");
      updateButtonPanel_.clear();

      updateNotificationPanel_.setVisible(true);
      updateFrameLayout();
   }

   @Override
   public void showUpdateComplete()
   {
      updateMessageLabel_.setHTML("Update complete. Reloading...");
      updateButtonPanel_.clear();

      updateNotificationPanel_.setVisible(true);
      updateFrameLayout();
   }

   @Override
   public void showUpdateError(String errorMessage)
   {
      updateMessageLabel_.setHTML("Update failed: " + errorMessage);
      updateButtonPanel_.clear();

      Button retryButton = new Button("Retry");
      retryButton.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            if (updateObserver_ != null)
            {
               updateObserver_.onRetryUpdate();
            }
         }
      });

      Button dismissButton = new Button("Dismiss");
      dismissButton.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            hideUpdateNotification();
         }
      });

      updateButtonPanel_.add(retryButton);
      updateButtonPanel_.add(dismissButton);

      updateNotificationPanel_.setVisible(true);
      updateFrameLayout();
   }

   @Override
   public void hideUpdateNotification()
   {
      updateNotificationPanel_.setVisible(false);
      updateFrameLayout();
   }

   @Override
   public void showUpdateCheckFailure()
   {
      updateMessageLabel_.setHTML("Unable to download update information, continuing with currently installed version");
      updateButtonPanel_.clear();

      Button dismissButton = new Button("Dismiss");
      dismissButton.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            hideUpdateNotification();
         }
      });

      updateButtonPanel_.add(dismissButton);

      // Use different background color for info message (lighter blue/yellow)
      updateNotificationPanel_.getElement().getStyle().setBackgroundColor("#fff8dc");
      updateNotificationPanel_.setVisible(true);
      updateFrameLayout();
   }

   @Override
   public void setStatus(String status)
   {
      currentStatus_ = status;

      switch (status)
      {
         case "starting":
            showMessage(constants_.startingChatMessage());
            break;
         case "not_installed":
            showMessage(constants_.chatNotInstalledMessage());
            break;
         case "error":
            // Error message will be shown via showError()
            break;
         case "ready":
            // UI is loaded, hide messages
            hideMessage();
            break;
      }
   }

   @Override
   public void showError(String errorMessage)
   {
      showMessage("Error: " + errorMessage);
   }

   private void showMessage(String message)
   {
      updateFrameContent(generateMessageHTML(message));
   }

   private void hideMessage()
   {
      // Content will be replaced by iframe loading
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      toolbar_ = new Toolbar(constants_.chatTabLabel());

      return toolbar_;
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      if (!initialized_)
      {
         initialized_ = true;

         // Show pending message now that frame is loaded
         if (pendingMessage_ != null)
         {
            updateFrameContent(pendingMessage_);
            pendingMessage_ = null;
         }

         // Check if Chat features are installed
         server_.chatVerifyInstalled(new ServerRequestCallback<Boolean>()
         {
            @Override
            public void onResponseReceived(Boolean result)
            {
               if (!result)
               {
                  // Chat is not installed - show message but still check for updates
                  setStatus("not_installed");
                  // Still trigger observer so update/install check can happen
                  if (observer_ != null)
                  {
                     observer_.onPaneReady();
                  }
               }
               else
               {
                  // Chat is installed - start backend and load UI
                  if (observer_ != null)
                  {
                     observer_.onPaneReady();
                  }
               }
            }

            @Override
            public void onError(ServerError error)
            {
               updateFrameContent(generateMessageHTML(constants_.errorDetectingInstallationMessage()));
            }
         });
      }
   }

   @Override
   public void onSelected()
   {
      super.onSelected();
      // Refresh iframe content when pane becomes visible
      if (contentType_ == ContentType.HTML && currentContent_ != null)
      {
         setFrameContent(frame_, currentContent_);
      }
      else if (contentType_ == ContentType.URL && currentUrl_ != null)
      {
         frame_.setUrl(currentUrl_);
      }
   }

   // Resources ----
   public interface Resources extends ClientBundle
   {
      @Source("ChatPane.css")
      CssResource styles();
   }


   private LayoutPanel mainPanel_;
   private RStudioThemedFrame frame_;
   private Toolbar toolbar_;
   private boolean initialized_ = false;
   private String pendingMessage_ = null;
   private ContentType contentType_ = ContentType.HTML;
   private String currentContent_ = null;
   private String currentUrl_ = null;
   private String currentStatus_ = "idle";
   private ChatPresenter.Display.Observer observer_;
   private ChatPresenter.Display.UpdateObserver updateObserver_;

   // Update notification UI components
   private FlowPanel updateNotificationPanel_;
   private HTML updateMessageLabel_;
   private HorizontalPanel updateButtonPanel_;

   // Injected ----
   @SuppressWarnings("unused")
   private final GlobalDisplay globalDisplay_;
   @SuppressWarnings("unused")
   private final Commands commands_;
   @SuppressWarnings("unused")
   private final Session session_;
   private final ChatServerOperations server_;

   private static final ChatConstants constants_ = com.google.gwt.core.client.GWT.create(ChatConstants.class);
}
