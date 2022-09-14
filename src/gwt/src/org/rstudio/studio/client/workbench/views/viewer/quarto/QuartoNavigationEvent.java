/*
 * QuartoNavigationEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.viewer.quarto;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class QuartoNavigationEvent extends GwtEvent<QuartoNavigationEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onQuartoNavigation(QuartoNavigationEvent event);
   }

   public QuartoNavigationEvent(String url, String srcFile)
   {
      url_ = url;
      srcFile_ = srcFile;
   }

   public String getUrl()
   {
      return url_;
   }
   
   public String getSrcFile()
   {
      return srcFile_;
   }


   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onQuartoNavigation(this);
   }

   private final String url_;
   private final String srcFile_;

   public static final Type<Handler> TYPE = new Type<>();
}
