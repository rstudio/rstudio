/*
 * ShinyApplicationWindow.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.shiny.ui;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.SatelliteWindow;
import org.rstudio.studio.client.shiny.ShinyApplicationPresenter;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;

@Singleton
public class ShinyApplicationWindow extends SatelliteWindow
                                    implements ShinyApplicationView
{

   @Inject
   public ShinyApplicationWindow(Provider<EventBus> pEventBus,
                                 Provider<FontSizeManager> pFSManager, 
                                 Provider<ShinyApplicationPresenter> pPresenter)
   {
      super(pEventBus, pFSManager);
      pPresenter_ = pPresenter;
   }

   @Override
   protected void onInitialize(LayoutPanel mainPanel, JavaScriptObject params)
   {
      ShinyApplicationParams appParams = params.cast();
      Window.setTitle(appParams.getPath() + " - " + "Shiny");
      ShinyApplicationPresenter appPresenter = pPresenter_.get();
      appPresenter.loadApp(appParams);
      
      // make it fill the containing layout panel
      Widget presWidget = appPresenter.asWidget();
      mainPanel.add(presWidget);
      mainPanel.setWidgetLeftRight(presWidget, 0, Unit.PX, 0, Unit.PX);
      mainPanel.setWidgetTopBottom(presWidget, 0, Unit.PX, 0, Unit.PX);
   }

   @Override
   public void reactivate(JavaScriptObject params)
   {
   }
   
   @Override 
   public Widget getWidget()
   {
      return this;
   }
   
   private Provider<ShinyApplicationPresenter> pPresenter_;
}
