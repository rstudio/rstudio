/*
 * QWebChannel.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.resources.StaticDataResource;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.user.client.Command;

public class QWebChannel
{
   public static final void load(final Command continuation)
   {
      ScriptInjector.fromUrl("qrc:///qtwebchannel/qwebchannel.js")
         .setWindow(ScriptInjector.TOP_WINDOW)
         .setRemoveTag(false)
         .setCallback(new Callback<Void, Exception>()
         {
            @Override
            public void onSuccess(Void argument)
            {
               onLoadQWebChannelJs(continuation);
            }
            
            @Override
            public void onFailure(Exception e)
            {
               Debug.logException(e);
            }
         })
         .inject();
   }
   
   private static void onLoadQWebChannelJs(final Command continuation)
   {
      String uri = RES.qWebChannelJs().getSafeUri().asString();
      new ExternalJavaScriptLoader(uri)
         .addCallback(new ExternalJavaScriptLoader.Callback()
         {
            @Override
            public void onLoaded()
            {
               continuation.execute();
            }
         });
   }
   
   public interface Resources extends ClientBundle
   {
      @Source("QWebChannel.js")
      StaticDataResource qWebChannelJs();
   }
   
   private static final Resources RES = GWT.create(Resources.class);
}
