/*
 * RSConnectAuthPresenter.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.rsconnect.ui;

import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteUtils;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class RSConnectAuthPresenter implements IsWidget
{
   public interface Display extends IsWidget
   {
      void showClaimUrl(String serverName, String url);
   }
   
   @Inject
   public RSConnectAuthPresenter(Display view,
         Satellite satellite)
   {
      view_ = view;
      satellite.addCloseHandler(new CloseHandler<Satellite>()
      {
         @Override
         public void onClose(CloseEvent<Satellite> arg0)
         {
            notifyAuthClosed();
         }
      });
   }     

   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
   }
   
   public void showClaimUrl(String serverName, String url)
   {
      if (Desktop.isDesktop())
         Desktop.getFrame().prepareSatelliteNavigate(
               SatelliteUtils.getSatelliteWindowName(
                     RSConnectAuthSatellite.NAME), url);
      view_.showClaimUrl(serverName, url);
   }
   
   private final native void notifyAuthClosed() /*-{
      $wnd.opener.notifyAuthClosed();
   }-*/;

   private final Display view_;
}