/*
 * SynctexStatusChangedEvent.java
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
package org.rstudio.studio.client.common.synctex.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class SynctexStatusChangedEvent extends GwtEvent<SynctexStatusChangedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onSynctexStatusChanged(SynctexStatusChangedEvent event);
   }

   public SynctexStatusChangedEvent(String targetFile, String pdfPath)
   {
      targetFile_ = targetFile;
      pdfPath_ = pdfPath;
   }

   public boolean isAvailable()
   {
      return getPdfPath() != null;
   }

   public String getTargetFile()
   {
      return targetFile_;
   }

   public String getPdfPath()
   {
      return pdfPath_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onSynctexStatusChanged(this);
   }

   private final String pdfPath_;
   private final String targetFile_;

   public static final Type<Handler> TYPE = new Type<>();
}
