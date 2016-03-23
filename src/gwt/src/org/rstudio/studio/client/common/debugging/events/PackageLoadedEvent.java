/*
 * PackageLoadedEvent.java
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
package org.rstudio.studio.client.common.debugging.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class PackageLoadedEvent
        extends GwtEvent<PackageLoadedEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data() {}
      
      public final native String getPackageName()    /*-{ return this["package_name"]; }-*/;
      public final native String getPackageVersion() /*-{ return this["package_version"]; }-*/;
   }
   
   public interface Handler extends EventHandler
   {
      void onPackageLoaded(PackageLoadedEvent event);
   }

   public PackageLoadedEvent(Data data)
   {
      packageName_ = data.getPackageName();
      packageVersion_ = data.getPackageVersion();
   }
   
   public String getPackageName()
   {
      return packageName_;
   }
   
   public String getPackageVersion()
   {
      return packageVersion_;
   }
   
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onPackageLoaded(this);
   }

   public static final Type<Handler> TYPE = new Type<Handler>();
   
   private final String packageName_;
   private final String packageVersion_;
}
