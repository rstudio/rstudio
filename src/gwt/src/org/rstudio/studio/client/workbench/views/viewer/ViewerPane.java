/*
 * ViewerPane.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.viewer;

import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.HtmlMessageListener;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.URIConstants;
import org.rstudio.core.client.URIUtils;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.AutoGlassPanel;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.plumber.model.PlumberAPIParams;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.model.PublishHtmlSource;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishButton;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.viewer.events.ViewerNavigatedEvent;
import org.rstudio.studio.client.workbench.views.viewer.model.ViewerServerOperations;

public class ViewerPane extends WorkbenchPane implements ViewerPresenter.Display
{
   @Inject
   public ViewerPane(Commands commands,
                     GlobalDisplay globalDisplay,
                     EventBus events,
                     ViewerServerOperations server,
                     HtmlMessageListener htmlMessageListener)
   {
      super("Viewer", events);
      commands_ = commands;
      globalDisplay_ = globalDisplay;
      server_ = server;
      htmlMessageListener_ = htmlMessageListener;
      ensureWidget();
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      toolbar_ = new Toolbar("Viewer Tab");

      // add html widget buttons
      toolbar_.addLeftWidget(commands_.viewerBack().createToolbarButton());
      toolbar_.addLeftWidget(commands_.viewerForward().createToolbarButton());
      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(commands_.viewerZoom().createToolbarButton());

      // export commands
      exportButtonSeparator_ = toolbar_.addLeftSeparator();
      ToolbarPopupMenu exportMenu = new ToolbarPopupMenu();
      exportMenu.addItem(commands_.viewerSaveAsImage().createMenuItem(false));
      exportMenu.addItem(commands_.viewerCopyToClipboard().createMenuItem(false));
      exportMenu.addSeparator();
      exportMenu.addItem(commands_.viewerSaveAsWebPage().createMenuItem(false));

      exportButton_ = new ToolbarMenuButton(
            "Export", ToolbarButton.NoTitle, new ImageResource2x(StandardIcons.INSTANCE.export_menu2x()),
            exportMenu);
      toolbar_.addLeftWidget(exportButton_);
      exportButton_.setVisible(false);
      exportButtonSeparator_.setVisible(false);

      // Each pane requires a focusable widget so rather than using the AppCommand method
      // createToolbarButton here, we manually create the button so it can be enabled
      // with a no-op when the command is disabled rather than throwing an exception.
      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(new ToolbarButton(commands_.viewerClear().getButtonLabel(),
                                               commands_.viewerClear().getDesc(),
                                               commands_.viewerClear().getImageResource(),
                                               event -> {
                                                  if (commands_.viewerClear().isEnabled())
                                                     commands_.viewerClear().execute();
                                               }));

      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(commands_.viewerClearAll().createToolbarButton());

      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(commands_.viewerPopout().createToolbarButton());

      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(commands_.viewerStop().createToolbarButton());

      // add publish button
      publishButton_ = new RSConnectPublishButton(
            RSConnectPublishButton.HOST_VIEWER,
            RSConnect.CONTENT_TYPE_DOCUMENT, true, null);
      toolbar_.addRightWidget(publishButton_);

      toolbar_.addRightSeparator();
      toolbar_.addRightWidget(commands_.viewerRefresh().createToolbarButton());


      // create an HTML generator
      publishButton_.setPublishHtmlSource(new PublishHtmlSource()
      {
         @Override
         public void generatePublishHtml(
               final CommandWithArg<String> onCompleted)
         {
            server_.viewerCreateRPubsHtml(
                  getTitle(), "", new ServerRequestCallback<String>()
            {
               @Override
               public void onResponseReceived(String htmlFile)
               {
                  onCompleted.execute(htmlFile);
               }
               @Override
               public void onError(ServerError error)
               {
                  globalDisplay_.showErrorMessage("Could Not Publish",
                        error.getMessage());
               }
            });
         }

         @Override
         public String getTitle()
         {
            String title = frame_.getTitle();
            if (StringUtil.isNullOrEmpty(title))
               title = "Viewer Content";
            return title;
         }
      });

      return toolbar_;
   }

   @Override
   protected Widget createMainWidget()
   {
      frame_ = new RStudioFrame("Viewer Pane");
      frame_.setSize("100%", "100%");
      frame_.addStyleName("ace_editor_theme");
      navigate(URIConstants.ABOUT_BLANK, false);
      return new AutoGlassPanel(frame_);
   }

   @Override
   public void navigate(String url)
   {
      htmlMessageListener_.setUrl(url);
      navigate(url, false);

      rmdPreviewParams_ = null;
      if (url == URIConstants.ABOUT_BLANK)
      {
         publishButton_.setContentType(RSConnect.CONTENT_TYPE_NONE);
      }
      else
      {
         publishButton_.setContentType(RSConnect.CONTENT_TYPE_HTML);
      }
   }

   @Override
   public void previewRmd(RmdPreviewParams params)
   {
      navigate(params.getOutputUrl(), true);
      publishButton_.setManuallyHidden(false);
      publishButton_.setRmdPreview(params);
      rmdPreviewParams_ = params;
      toolbar_.invalidateSeparators();
   }

   @Override
   public void previewShiny(ShinyApplicationParams params)
   {
      navigate(params.getUrl(), true);
      publishButton_.setManuallyHidden(false);
      publishButton_.setShinyPreview(params);
      toolbar_.invalidateSeparators();
   }

   @Override
   public void previewPlumber(PlumberAPIParams params)
   {
      navigate(params.getUrl(), true);
      publishButton_.setManuallyHidden(false);
      publishButton_.setPlumberPreview(params);
      toolbar_.invalidateSeparators();
   }

   @Override
   public void setExportEnabled(boolean exportEnabled)
   {
      exportButton_.setVisible(exportEnabled);
      exportButtonSeparator_.setVisible(exportEnabled);
      publishButton_.setManuallyHidden(!exportEnabled);
      toolbar_.invalidateSeparators();
   }

   @Override
   public String getUrl()
   {
      return frame_.getUrl();
   }

   @Override
   public String getTitle()
   {
      return frame_.getTitle();
   }

   @Override
   public void popout()
   {
      if (rmdPreviewParams_ != null &&
          !rmdPreviewParams_.isShinyDocument())
      {
         globalDisplay_.showHtmlFile(rmdPreviewParams_.getOutputFile());
      }
      else if (frame_ != null &&
          frame_.getIFrame().getCurrentUrl() != null &&
          !StringUtil.equals(frame_.getIFrame().getCurrentUrl(), getUrl()))
      {
         // Typically we navigate to the unmodified URL (i.e. without the
         // viewer_pane=1 query params, etc.) However, if the URL currently
         // loaded in the frame is different, the user probably navigated away
         // from original URL, so load that URL as-is.
         globalDisplay_.openWindow(frame_.getIFrame().getCurrentUrl());
      }
      else if (unmodifiedUrl_ != null)
      {
         globalDisplay_.openWindow(unmodifiedUrl_);
      }
   }

   @Override
   public void refresh()
   {
      String url = frame_.getUrl();
      if (url != null)
         frame_.setUrl(url);
   }


   @Override
   public HandlerRegistration addLoadHandler(LoadHandler handler)
   {
      return frame_.addLoadHandler(handler);
   }

   @Override
   public Size getViewerFrameSize()
   {
      return new Size(frame_.getOffsetWidth(), frame_.getOffsetHeight());
   }


   @Override
   public void onResize()
   {
      super.onResize();
      int width = getOffsetWidth();
      if (width == 0)
         return;

      publishButton_.setShowCaption(width > 500);
   }

   private native static String getOrigin() /*-{
     return $wnd.location.origin;
   }-*/;

   private void navigate(String url, boolean useRawURL)
   {
      // save the unmodified URL for pop-out
      unmodifiedUrl_ = url;

      // in desktop mode we need to be careful about loading URLs which are
      // non-local; before changing the URL, set the iframe to be sandboxed
      // based on whether we're working with a local URL (note that prior to
      // RStudio 1.2 local URLs were forbidden entirely)
      if (Desktop.hasDesktopFrame())
      {
         if (URIUtils.isLocalUrl(url))
         {
            frame_.getElement().removeAttribute("sandbox");
         }
         else
         {
            frame_.getElement().setAttribute("sandbox", "allow-scripts");
         }
      }

      // append the viewer_pane query parameter
      if ((unmodifiedUrl_ != null) &&
          !unmodifiedUrl_.equals(URIConstants.ABOUT_BLANK) &&
          !useRawURL)
      {
         String viewerUrl = URIUtils.addQueryParam(unmodifiedUrl_,
                                                   "viewer_pane",
                                                   "1");

         viewerUrl = URIUtils.addQueryParam(viewerUrl,
                                            "capabilities",
                                            String.valueOf(1 << Capabilities.OpenFile.ordinal()));

         viewerUrl = URIUtils.addQueryParam(viewerUrl,
                                            "host",
                                            htmlMessageListener_.getOriginDomain());

         frame_.setUrl(viewerUrl);
      }
      else
      {
         frame_.setUrl(unmodifiedUrl_);
      }

      if (unmodifiedUrl_ != null && !unmodifiedUrl_.equals(URIConstants.ABOUT_BLANK)) {
         frame_.getElement().getStyle().setBackgroundColor("#FFF");
      }
      else {
         frame_.getElement().getStyle().clearBackgroundColor();
      }

      events_.fireEvent(new ViewerNavigatedEvent(url, frame_));
   }

   private enum Capabilities
   {
      OpenFile
   }

   private RStudioFrame frame_;
   private String unmodifiedUrl_;
   private RmdPreviewParams rmdPreviewParams_;
   private final Commands commands_;
   private final GlobalDisplay globalDisplay_;
   private final ViewerServerOperations server_;

   private Toolbar toolbar_;

   private RSConnectPublishButton publishButton_;

   private ToolbarMenuButton exportButton_;
   private Widget exportButtonSeparator_;

   private HtmlMessageListener htmlMessageListener_;
}
