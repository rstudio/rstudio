/*
 * OpenProfileEvent.java
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

package org.rstudio.studio.client.workbench.views.source.editors.profiler;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class OpenProfileEvent extends GwtEvent<OpenProfileEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onOpenProfileEvent(OpenProfileEvent event);
   }

   public OpenProfileEvent(String filePath,
                           String htmlPath,
                           String htmlLocalPath,
                           boolean createProfile,
                           String docId)
   {
      filePath_ = filePath;
      htmlPath_ = htmlPath;
      htmlLocalPath_ = htmlLocalPath;
      createProfile_ = createProfile;
      docId_ = docId;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onOpenProfileEvent(this);
   }

   public String getFilePath()
   {
      return filePath_;
   }

   public String getHtmlPath()
   {
      return htmlPath_;
   }

   public String getHtmlLocalPath()
   {
      return htmlLocalPath_;
   }

   public boolean getCreateProfile()
   {
      return createProfile_;
   }

   public String getDocId()
   {
      return docId_;
   }

   public static final Type<Handler> TYPE = new Type<>();
   private String filePath_;
   private String htmlPath_;
   private String htmlLocalPath_;
   private boolean createProfile_ = false;
   private String docId_;
}
