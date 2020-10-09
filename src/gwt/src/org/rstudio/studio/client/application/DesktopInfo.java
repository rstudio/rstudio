/*
 * DesktopInfo.java
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
package org.rstudio.studio.client.application;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DesktopInfo
{
   @Inject
   public DesktopInfo(Session session,
                      Commands commands,
                      EventBus events)
   {
      // do no work in server mode
      if (!Desktop.hasDesktopFrame())
         return;

      events.addHandler(SessionInitEvent.TYPE, (SessionInitEvent sie) ->
      {
         // disable devtools command when unavailable
         boolean devtoolsEnabled = getChromiumDevtoolsPort() > 0;
         commands.openDeveloperConsole().setEnabled(devtoolsEnabled);
         commands.openDeveloperConsole().setVisible(devtoolsEnabled);

         if (Satellite.isCurrentWindowSatellite())
            return;

         SessionInfo info = session.getSessionInfo();
         if (info.getSumatraPdfExePath() != null)
            setSumatraPdfExePath(info.getSumatraPdfExePath());
      });
   }

   public static final native String getPlatform()
   /*-{
      return $wnd.desktopInfo.platform;
   }-*/;

   public static final native String getVersion()
   /*-{
      return $wnd.desktopInfo.version;
   }-*/;

   public static final native String getScrollingCompensationType()
   /*-{
      return $wnd.desktopInfo.scrollingCompensationType;
   }-*/;

   public static final native String getFixedWidthFontList()
   /*-{
      return $wnd.desktopInfo.fixedWidthFontList;
   }-*/;

   public static final native String getFixedWidthFont()
   /*-{
      return $wnd.desktopInfo.fixedWidthFont;
   }-*/;

   public static final native String getProportionalFont()
   /*-{
      return $wnd.desktopInfo.proportionalFont;
   }-*/;

   public static final native String getDesktopSynctexViewer()
   /*-{
      return $wnd.desktopInfo.desktopSynctexViewer;
   }-*/;

   public static final native String getSumatraPdfExePath()
   /*-{
      return $wnd.desktopInfo.sumatraPdfExePath;
   }-*/;

   public static final native void setSumatraPdfExePath(String path)
   /*-{
      $wnd.desktopInfo.sumatraPdfExePath = path;
   }-*/;

   public static final native double getZoomLevel()
   /*-{
      return $wnd.desktopInfo.zoomLevel;
   }-*/;

   public static final native void setZoomLevel(double zoomLevel)
   /*-{
      $wnd.desktopInfo.zoomLevel = zoomLevel;
   }-*/;

   public static final native int getChromiumDevtoolsPort()
   /*-{
      return $wnd.desktopInfo.chromiumDevtoolsPort || 0;
   }-*/;

}
