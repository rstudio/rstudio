/*
 * HttpLogEntry.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.application.model;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import org.rstudio.studio.client.application.StudioClientApplicationConstants;

import java.util.Date;

public class HttpLogEntry extends JavaScriptObject
{
   public final static int CONNECTION_RECEIVED = 1;
   public final static int CONNECTION_DEQUEUED = 2;
   public final static int CONNECTION_RESPONDED = 3;
   public final static int CONNECTION_TERMINATED = 4;
   public final static int CONNECTION_ERROR = 5;

   protected HttpLogEntry()
   {

   }

   public native final int getType() /*-{
      return this.type;
   }-*/;

   public final String getTypeAsString()
   {
      switch(getType())
      {
      case CONNECTION_RECEIVED:
         return constants_.connectionReceivedType();
      case CONNECTION_DEQUEUED :
         return constants_.connectionDequeuedType();
      case CONNECTION_RESPONDED:
         return constants_.connectionRespondedType();
      case CONNECTION_TERMINATED:
         return constants_.connectionTerminatedType();
      case CONNECTION_ERROR:
         return constants_.connectionErrorType();
      default:
         return constants_.connectionUnknownType();
      }
   }

   public native final String getRequestId() /*-{
      return this.id;
   }-*/;


   public final Date getTimestamp()
   {
      Double timestamp = getTimestampNative();
      return new Date(timestamp.longValue());
   }

   private final native double getTimestampNative() /*-{
      return this.ts;
   }-*/;

   private static final StudioClientApplicationConstants constants_ = GWT.create(StudioClientApplicationConstants.class);
}
