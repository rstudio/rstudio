/*
 * HelpPopoutWindow.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.help;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.SatelliteWindow;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;

@Singleton
public class HelpPopoutWindow extends SatelliteWindow
                              implements HelpPopoutView
{
   @Inject
   public HelpPopoutWindow(Provider<EventBus> pEventBus,
                           Provider<FontSizeManager> pFSManager,
                           Provider<HelpPopoutPanel> pPanel)
   {
      super(pEventBus, pFSManager);
      pPanel_ = pPanel;
   }

   @Override
   protected void onInitialize(LayoutPanel mainPanel, JavaScriptObject params)
   {
      HelpPopoutParams popoutParams = params.cast();

      String title = popoutParams.getTitle();
      if (StringUtil.isNullOrEmpty(title))
         title = constants_.helpText();
      Window.setTitle(title);

      HelpPopoutPanel panel = pPanel_.get();
      panel.showHelp(popoutParams.getUrl());

      Widget panelWidget = panel.asWidget();
      mainPanel.add(panelWidget);
      mainPanel.setWidgetLeftRight(panelWidget, 0, Unit.PX, 0, Unit.PX);
      mainPanel.setWidgetTopBottom(panelWidget, 0, Unit.PX, 0, Unit.PX);
   }

   @Override
   public boolean supportsThemes()
   {
      return true;
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

   private final Provider<HelpPopoutPanel> pPanel_;
   private static final HelpConstants constants_ = GWT.create(HelpConstants.class);
}
