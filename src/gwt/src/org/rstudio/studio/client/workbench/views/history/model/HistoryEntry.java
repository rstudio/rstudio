/*
 * HistoryEntry.java
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
package org.rstudio.studio.client.workbench.views.history.model;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.Date;

public class HistoryEntry extends JavaScriptObject
{
   protected HistoryEntry()
   {
   }

   public static final native HistoryEntry create(int index, String command) /*-{
      var entry = new Object();
      entry.index = index;
      entry.timestamp = 0;
      entry.command = command;
      return entry;
   }-*/;

   public final long getIndex()
   {
      return Double.valueOf(getIndexNative()).longValue();
   }

   public final Date getTimestamp()
   {
      Double lastModified = getTimestampNative();
      return new Date(lastModified.longValue());
   }

   public final native String getCommand() /*-{
      return this.command;
   }-*/;

   public final String asString()
   {
      return getIndex() + " - " +
             getTimestamp().toString() + " - " +
             getCommand();
   }

   private final native double getIndexNative() /*-{
      return this.index;
   }-*/;

   private final native double getTimestampNative() /*-{
      return this.timestamp;
   }-*/;
}
