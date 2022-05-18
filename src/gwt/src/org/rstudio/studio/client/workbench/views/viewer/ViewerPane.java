/*
 * ViewerPane.java
 *
 * Copyright (C) 2022 by RStudio, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.viewer;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.HtmlMessageListener;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.URIConstants;
import org.rstudio.core.client.URIUtils;
import org.rstudio.core.client.files.FileSystemItem;
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
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.plumber.model.PlumberAPIParams;
import org.rstudio.studio.client.quarto.model.QuartoNavigate;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.model.PublishHtmlSource;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishButton;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.viewer.events.ViewerNavigatedEvent;
import org.rstudio.studio.client.workbench.views.viewer.model.ViewerServerOperations;
import org.rstudio.studio.client.workbench.views.viewer.quarto.QuartoConnection;

public class ViewerPane extends WorkbenchPane implements ViewerPresenter.Display
{
   @Inject
   public ViewerPane(Commands commands,
                     GlobalDisplay globalDisplay,
                     EventBus events,
                     ViewerServerOperations server,
                     FileTypeRegistry fileTypeRegistry,
                     Provider<UserState> pUserState,
                     HtmlMessageListener htmlMessageListener)
   {
      super("Viewer", events);
      commands_ = commands;
      globalDisplay_ = globalDisplay;
      server_ = server;
      fileTypeRegistry_ = fileTypeRegistry;
      pUserState_ = pUserState;
      htmlMessageListener_ = htmlMessageListener;
      quartoConnection_ = new QuartoConnection();
      ensureWidget();
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      toolbar_ = new Toolbar(constants_.viewerTabLabel());

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
            constants_.exportText(), ToolbarButton.NoTitle, new ImageResource2x(StandardIcons.INSTANCE.export_menu2x()),
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
                                                  {
                                                     removeQuartoUI();
                                                     commands_.viewerClear().execute();
                                                  }
                                               }));

      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(commands_.viewerClearAll().createToolbarButton());

      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(commands_.viewerPopout().createToolbarButton());

      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(commands_.viewerStop().createToolbarButton());
      
      // quarto specific widgets
      initQuartoUI();

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
                  globalDisplay_.showErrorMessage(constants_.couldNotPublishCaption(),
                        error.getMessage());
               }
            });
         }

         @Override
         public String getTitle()
         {
            String title = frame_.getTitle();
            if (StringUtil.isNullOrEmpty(title))
               title = constants_.viewerContentTitle();
            return title;
         }
      });

      return toolbar_;
   }

   @Override
   protected Widget createMainWidget()
   {
      frame_ = new RStudioFrame(constants_.viewerPaneTitle());
      frame_.setSize("100%", "100%");
      frame_.addStyleName("ace_editor_theme");
      navigate(URIConstants.ABOUT_BLANK, false);
      return new AutoGlassPanel(frame_);
   }

   @Override
   public void navigate(String url)
   {
      removeQuartoUI();
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
   public void previewQuarto(String url, QuartoNavigate quartoNav)
   {
      rmdPreviewParams_ = null;
      navigate(url, false, false);
      quartoConnection_.setQuartoUrl(url, quartoNav.isWebsite());
      publishButton_.setManuallyHidden(false);
      if (quartoNav.isWebsite())
         publishButton_.setQuartoSitePreview();
      else
         publishButton_.setQuartoDocPreview(quartoNav.getSourceFile(), quartoNav.getOutputFile());
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
      else if (quartoConnection_.getUrl() != null)
      {
         globalDisplay_.openWindow(quartoConnection_.getUrl());
      }
      else if (frame_ != null &&
          frame_.getIFrame().getCurrentUrl() != null &&
          !StringUtil.equals(urlWithoutHash(frame_.getIFrame().getCurrentUrl()), 
                             urlWithoutHash(getUrl())))
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
      try
      {
         frame_.getWindow().reload();
      }
      catch (Exception e)
      {
         String url = frame_.getUrl();
         if (url != null)
            frame_.setUrl(url);
      }
   }
   
   @Override
   public void editSource()
   {
      FileSystemItem srcFile = quartoConnection_.getSrcFile();
      if (srcFile != null)
      {
         fileTypeRegistry_.editFile(srcFile);
         new Timer() {
            @Override
            public void run()
            {
               commands_.activateSource();
            }
         }.schedule(200);
      }
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
   
   @Override
   public boolean hasNavigationHandlers()
   {
      return quartoConnection_.isWebsite();
   }

   @Override
   public void navigateForward()
   {
      quartoConnection_.navigateForward();
      
   }

   @Override
   public void navigateBack()
   {
     quartoConnection_.navigateBack();
   }
   
   
   private void initQuartoUI()
   {
      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(commands_.viewerEditSource().createToolbarButton());
      toolbar_.addLeftWidget(quartoSyncEditor_ = new CheckBox(constants_.syncEditorLabel()));
      quartoSyncEditor_.getElement().getStyle().setMarginLeft(3, Unit.PX);
      quartoSyncEditor_.setVisible(false);
      quartoSyncEditor_.setValue(pUserState_.get().quartoWebsiteSyncEditor().getValue());
      quartoSyncEditor_.addValueChangeHandler(event -> {
         pUserState_.get().quartoWebsiteSyncEditor().setGlobalValue(event.getValue());
         pUserState_.get().writeState();
      });
      quartoConnection_.addQuartoNavigationHandler(event -> {
         if (quartoConnection_.isWebsite() && quartoConnection_.getSrcFile() != null)
         {
            if (quartoSyncEditor_.getValue())
            {
               fileTypeRegistry_.editFile(quartoConnection_.getSrcFile());
            }
          
            quartoSyncEditor_.setVisible(true);
         }
         else
         {
            quartoSyncEditor_.setVisible(false);
         }
         toolbar_.invalidateSeparators();
      });
   }
   
   private void removeQuartoUI()
   {
      quartoConnection_.setQuartoUrl(null, false);
      quartoSyncEditor_.setVisible(false);
   }   

   
   private String urlWithoutHash(String url)
   {
      if (!StringUtil.isNullOrEmpty(url))
      {
         return url.split("#")[0];
      }
      else
      {
         return url;
      }
   }

   private native static String getOrigin() /*-{
     return $wnd.location.origin;
   }-*/;

   private void navigate(String url, boolean useRawURL)
   {
      navigate(url, useRawURL, !useRawURL);
   }
   
   private void navigate(String url, boolean useRawURL, boolean viewerPaneParam)
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
         String viewerUrl = unmodifiedUrl_;
         if (viewerPaneParam)
         {
            viewerUrl = URIUtils.addQueryParam(viewerUrl, "viewer_pane", "1");
         }

         viewerUrl = URIUtils.addQueryParam(viewerUrl,
                                            "capabilities",
                                            String.valueOf(1 << Capabilities.OpenFile.ordinal()));

         viewerUrl = URIUtils.addQueryParam(viewerUrl,
                                            "host",
                                            HtmlMessageListener.getOriginDomain());

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
   private final FileTypeRegistry fileTypeRegistry_;
   private final Provider<UserState> pUserState_;
   private final ViewerServerOperations server_;

   private Toolbar toolbar_;

   private CheckBox quartoSyncEditor_;
   private RSConnectPublishButton publishButton_;

   private ToolbarMenuButton exportButton_;
   private Widget exportButtonSeparator_;

   private HtmlMessageListener htmlMessageListener_;
   private QuartoConnection quartoConnection_;
   private static final ViewerConstants constants_ = com.google.gwt.core.client.GWT.create(ViewerConstants.class);
}