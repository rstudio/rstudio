/*
 * RmdOutputPanel.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.rmarkdown.ui;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.AnchorableFrame;
import org.rstudio.core.client.widget.SatelliteFramePanel;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarLabel;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;
import org.rstudio.studio.client.workbench.commands.Commands;

public class RmdOutputPanel extends SatelliteFramePanel<AnchorableFrame>
                            implements RmdOutputPresenter.Display
{
   @Inject
   public RmdOutputPanel(Commands commands, 
                         RMarkdownServerOperations server)
   {
      super(commands);
      server_ = server;
   }
   
   @Override
   public void showOutput(RmdPreviewParams params, boolean refresh)
   {
      params_ = params;
      fileLabel_.setText(params.getOutputFile());
      // when refreshing, reapply the current scroll position 
      scrollPosition_ = refresh ? 
            getScrollPosition() : params.getScrollPosition();
      showUrl(server_.getApplicationURL(params.getOutputUrl()));
   }
   
   @Override
   protected void initToolbar (Toolbar toolbar, Commands commands)
   {
      toolbar.addLeftWidget(new ToolbarLabel("Viewing: "));
      fileLabel_ = new ToolbarLabel();
      fileLabel_.addStyleName(ThemeStyles.INSTANCE.subtitle());
      fileLabel_.getElement().getStyle().setMarginRight(7, Unit.PX);
      toolbar.addLeftWidget(fileLabel_);
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands.viewerRefresh().createToolbarButton());
   }
   
   @Override
   protected AnchorableFrame createFrame(String url)
   {
      AnchorableFrame frame = new AnchorableFrame();
      frame.navigate(url);
      frame.addLoadHandler(new LoadHandler()
      {
         @Override
         public void onLoad(LoadEvent event)
         {
            getFrame().getIFrame().getContentDocument().setScrollTop(
                  scrollPosition_);
         }
      });
      return frame;
   }
   
   @Override
   public void refresh()
   {
      // cache the scroll position, so we can re-apply it when the page loads
      scrollPosition_ = getScrollPosition();
      showUrl(server_.getApplicationURL(params_.getOutputUrl()));
   }

   @Override
   protected String openCommandText()
   {
      return "Open File";
   }

   @Override
   public int getScrollPosition()
   {
      return getFrame().getIFrame().getContentDocument().getScrollTop();
   }

   private Label fileLabel_;
   private RMarkdownServerOperations server_;
   private RmdPreviewParams params_;
   private int scrollPosition_ = 0;
}
