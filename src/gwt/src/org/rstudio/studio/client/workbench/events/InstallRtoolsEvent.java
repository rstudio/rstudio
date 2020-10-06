/*
 * InstallRtoolsEvent.java
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
package org.rstudio.studio.client.workbench.events;


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class InstallRtoolsEvent extends GwtEvent<InstallRtoolsEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public final native String getVersion() /*-{
         return this.version;
      }-*/;

      public final native String getInstallerPath() /*-{
         return this.installer_path;
      }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onInstallRtools(InstallRtoolsEvent event);
   }

   public InstallRtoolsEvent(Data data)
   {
      data_ = data;
   }

   public String getVersion()
   {
      return data_.getVersion();
   }

   public String getInstallerPath()
   {
      return data_.getInstallerPath();
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onInstallRtools(this);
   }

   private final Data data_;

   public static final Type<Handler> TYPE = new Type<>();
}
