/*
 * Presentation2.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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

package org.rstudio.studio.client.workbench.views.presentation2;

import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.quarto.model.QuartoNavigate;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.presentation2.events.PresentationPreviewEvent;

import com.google.inject.Inject;

public class Presentation2 extends BasePresenter
{
   public interface Display extends WorkbenchView
   {
      void showPresentation(String url, QuartoNavigate nav);
      void refresh();
   }
   
   @Inject
   public Presentation2(Display display, 
                        Session session,
                        Commands commands,
                        EventBus eventBus)
   {
      super(display);
      display_ = display;
      commands_ = commands;
      commands_.refreshPresentation2().setEnabled(false);
   }

   public void onPresentationPreview(PresentationPreviewEvent event)
   {
      PresentationPreviewEvent.Data data = event.getData();
      display_.showPresentation(data.getUrl(),  data.getQuartoNavigation());
      commands_.refreshPresentation2().setEnabled(true);
   }
   
   @Handler
   void onRefreshPresentation2()
   {
      display_.refresh();
   }
   
   Display display_;
   Commands commands_;
}
