/*
 * ViewerPane.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.viewer;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.Size;
import org.rstudio.core.client.URIUtils;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.AutoGlassPanel;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.viewer.events.ViewerNavigatedEvent;

public class ViewerPane extends WorkbenchPane implements ViewerPresenter.Display
{
   @Inject
   public ViewerPane(Commands commands, GlobalDisplay globalDisplay, EventBus events)
   {
      super("Viewer");
      commands_ = commands;
      globalDisplay_ = globalDisplay;
      events_ = events;
      ensureWidget();
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      toolbar_ = new Toolbar();
      
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
      exportMenu.addSeparator();
      exportMenu.addItem(commands_.viewerPublishToRPubs().createMenuItem(false));
      
      exportButton_ = new ToolbarButton(
            "Export", StandardIcons.INSTANCE.export_menu(),
            exportMenu);
      toolbar_.addLeftWidget(exportButton_);  
      exportButton_.setVisible(false);
      exportButtonSeparator_.setVisible(false);
      
      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(commands_.viewerClear().createToolbarButton());
      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(commands_.viewerClearAll().createToolbarButton());
      
      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(commands_.viewerRefresh().createToolbarButton());
     
      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(commands_.viewerPopout().createToolbarButton());
     
      
      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(commands_.viewerStop().createToolbarButton());
     
      // add html document publish button 
      publishButton_ = commands_.publishHTML().createToolbarButton(false);
      toolbar_.addRightWidget(publishButton_);
      publishButton_.setVisible(false);
      
      // add rpubs publish button
      rpubsPublishButton_ = toolbar_.addRightWidget(
                commands_.viewerPublishToRPubs().createToolbarButton(false));
      rpubsPublishButton_.setVisible(false);
      
      return toolbar_;
   }
   
   @Override 
   protected Widget createMainWidget()
   {
      frame_ = new RStudioFrame() ;
      frame_.setSize("100%", "100%");
      navigate(ABOUT_BLANK, false);
      return new AutoGlassPanel(frame_);
   }
   
   @Override
   public void navigate(String url)
   {
      navigate(url, false);
      publishButton_.setVisible(false);
      rpubsPublishButton_.setVisible(commands_.viewerPublishToRPubs().isVisible());
      rmdPreviewParams_ = null;
   }

   @Override
   public void previewRmd(RmdPreviewParams params)
   {
      navigate(params.getOutputUrl(), true);
      rpubsPublishButton_.setVisible(false);
      publishButton_.setVisible(!params.isShinyDocument());
      if (!params.isShinyDocument())
         publishButton_.setText(params.getResult().getRpubsPublished() ? 
               "Republish" : "Publish");
      rmdPreviewParams_ = params;
      toolbar_.invalidateSeparators();
   }
   
   @Override
   public void setExportEnabled(boolean exportEnabled)
   {
      exportButton_.setVisible(exportEnabled);
      exportButtonSeparator_.setVisible(exportEnabled);
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
   public Size getViewerFrameSize()
   {
      return new Size(frame_.getOffsetWidth(), frame_.getOffsetHeight());
   }
   
   private void navigate(String url, boolean useRawURL)
   {
      // save the unmodified URL for pop-out
      unmodifiedUrl_ = url;
      
      // append the viewer_pane query parameter
      if ((unmodifiedUrl_ != null) && 
          !unmodifiedUrl_.equals(ABOUT_BLANK) &&
          !useRawURL)
      {
         String viewerUrl = URIUtils.addQueryParam(unmodifiedUrl_, 
                                                   "viewer_pane", 
                                                   "1");
         frame_.setUrl(viewerUrl);
      }
      else
      {
         frame_.setUrl(unmodifiedUrl_);
      }
      
      events_.fireEvent(new ViewerNavigatedEvent(url, frame_));
   }

   private RStudioFrame frame_;
   private String unmodifiedUrl_;
   private RmdPreviewParams rmdPreviewParams_;
   private final Commands commands_;
   private final GlobalDisplay globalDisplay_;
   private final EventBus events_;
   
   private Toolbar toolbar_;
   
   private ToolbarButton publishButton_;
   private ToolbarButton rpubsPublishButton_;
   
   private ToolbarButton exportButton_;
   private Widget exportButtonSeparator_;

   public static final String ABOUT_BLANK = "about:blank";
}
