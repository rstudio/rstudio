/*
 * PublishPdfEvent.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.event.shared.GwtEvent;

public class PublishPdfEvent extends GwtEvent<PublishPdfHandler>
{
   public static final GwtEvent.Type<PublishPdfHandler> TYPE =
      new GwtEvent.Type<PublishPdfHandler>();
   
   public PublishPdfEvent(String path)
   {
      path_ = path;
   }
   
   public String getPath()
   {
      return path_;
   }
   
   @Override
   protected void dispatch(PublishPdfHandler handler)
   {
      handler.onPublishPdf(this);
   }

   @Override
   public GwtEvent.Type<PublishPdfHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   
   private String path_;
}

