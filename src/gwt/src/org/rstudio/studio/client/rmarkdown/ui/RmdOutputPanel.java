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

import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

import org.rstudio.core.client.widget.AnchorableFrame;
import org.rstudio.core.client.widget.SatelliteFramePanel;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.rmarkdown.model.RmdRenderResult;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.common.FilePathUtils;

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
   public void showOutput(RmdRenderResult result)
   {
      fileLabel_.setText(
            FilePathUtils.friendlyFileName(result.getOutputFile()));
      showUrl(server_.getApplicationURL(result.getOutputUrl()));
   }
   
   @Override
   protected void initToolbar (Toolbar toolbar, Commands commands)
   {
      fileLabel_ = new Label();
      toolbar.addLeftWidget(fileLabel_);
   }
   
   @Override
   protected AnchorableFrame createFrame(String url)
   {
      AnchorableFrame frame = new AnchorableFrame();
      frame.navigate(url);
      return frame;
   }
   
   private Label fileLabel_;
   private RMarkdownServerOperations server_;
}
