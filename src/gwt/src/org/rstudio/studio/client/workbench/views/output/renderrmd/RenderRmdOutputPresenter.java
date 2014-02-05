/*
 * RenderRmdOutputPresenter.java
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

package org.rstudio.studio.client.workbench.views.output.renderrmd;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rmarkdown.events.RmdRenderStartedEvent;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.output.common.CompileOutputPaneDisplay;
import org.rstudio.studio.client.workbench.views.output.common.CompileOutputPaneFactory;

public class RenderRmdOutputPresenter extends BasePresenter
   implements RmdRenderStartedEvent.Handler
{
   @Inject
   public RenderRmdOutputPresenter(CompileOutputPaneFactory outputFactory,
                                   GlobalDisplay globalDisplay,
                                   Commands commands)
   {
      super(outputFactory.create("Render R Markdown", 
                                 "View the R Markdown render log"));
      view_ = (CompileOutputPaneDisplay) getView();
      globalDisplay_ = globalDisplay;
   }
   
   public void initialize()
   {
      // TODO: Set initial state
   }

   public void confirmClose(Command onConfirmed)
   {
      // TODO: Prompt for confirmation if necessary
      onConfirmed.execute();
   }

   @Override
   public void onRmdRenderStarted(RmdRenderStartedEvent event)
   {
      view_.ensureVisible(true);
   }
   
   CompileOutputPaneDisplay view_;
   GlobalDisplay globalDisplay_;
}