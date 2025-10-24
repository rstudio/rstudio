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
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ChatPane
      extends WorkbenchPane
      implements ChatPresenter.Display
{
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

      frame_ = new RStudioThemedFrame(constants_.chatTitle());
      frame_.setSize("100%", "100%");

      // Show initial "checking..." message
      updateFrameContent(generateMessageHTML(constants_.checkingInstallationMessage()));

      mainPanel_.add(frame_);
      mainPanel_.setWidgetTopHeight(frame_, 0, Unit.PCT, 100, Unit.PCT);
      mainPanel_.setWidgetLeftWidth(frame_, 0, Unit.PCT, 100, Unit.PCT);

      return mainPanel_;
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
      currentContent_ = html;
      setFrameContent(frame_, html);
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

         // Check if AI features are installed
         server_.chatVerifyInstalled(new ServerRequestCallback<Boolean>()
         {
            @Override
            public void onResponseReceived(Boolean result)
            {
               if (!result)
               {
                  // AI Chat is not installed
                  updateFrameContent(generateMessageHTML(constants_.aiChatNotInstalledMessage()));
               }
               else
               {
                  // TODO: AI Chat is installed - load the actual chat interface
                  updateFrameContent(generateMessageHTML("Coming Soon!"));
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
      if (currentContent_ != null)
      {
         setFrameContent(frame_, currentContent_);
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
   private String currentContent_ = null;

   // Injected ----
   @SuppressWarnings("unused")
   private final GlobalDisplay globalDisplay_;
   @SuppressWarnings("unused")
   private final Commands commands_;
   @SuppressWarnings("unused")
   private final Session session_;
   @SuppressWarnings("unused")
   private final ChatServerOperations server_;

   private static final ChatConstants constants_ = com.google.gwt.core.client.GWT.create(ChatConstants.class);
}
