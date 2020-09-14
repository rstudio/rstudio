/*
 * HttpLogEntry.java
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
package org.rstudio.studio.client.application.model;

import com.google.gwt.core.client.JavaScriptObject;

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
         return "Connection Received";
      case CONNECTION_DEQUEUED :
         return "Connection Dequeued";
      case CONNECTION_RESPONDED:
         return "Connection Responded";
      case CONNECTION_TERMINATED:
         return "Connection Terminated";
      case CONNECTION_ERROR:
         return "Connection Error";
      default:
         return "(Unknown)";
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

}
