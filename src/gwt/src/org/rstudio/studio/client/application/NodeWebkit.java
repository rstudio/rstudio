/*
 * NodeWebkit.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import org.rstudio.studio.client.RStudioGinjector;


public class NodeWebkit
{
   public static native boolean isNodeWebkit() /*-{
      return !!$wnd.NodeWebkit;
   }-*/;
   
   public static native void showDevtools() /*-{
      var win = $wnd.NodeWebkit.Window.get();
      win.showDevTools();
   }-*/;
   
   public static native void closeWindow() /*-{
      var win = $wnd.NodeWebkit.Window.get();
      win.close(true);
   }-*/;
   
   public static native void raiseWindow() /*-{
      var win = $wnd.NodeWebkit.Window.get();
      win.requestAttention(true);
   }-*/;
   
   public static native void browseURL(String url) /*-{
      var shell = $wnd.NodeWebkit.Shell;
      shell.openExternal(url);
   }-*/;
   
   public static native void registerCloseHandler() /*-{
      $wnd.NodeWebkitClose = $entry(
         function() {
            @org.rstudio.studio.client.application.NodeWebkit::quitSession()();
         }
       );
   }-*/;
   
   private static void quitSession()
   {
      RStudioGinjector.INSTANCE.getCommands().quitSession().execute();
   }
   
}
