/*
 * RequestLog.java
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
package org.rstudio.core.client.jsonrpc;

import java.util.ArrayList;

public class RequestLog
{
   public static RequestLogEntry log(String requestId, String requestData)
   {
      RequestLogEntry entry = new RequestLogEntry(System.currentTimeMillis(),
                                                  requestId, requestData);
      entries_.add(entry);

      for (int i = 0; entries_.size() > MAX_ENTRIES && i < entries_.size(); i++)
      {
         RequestLogEntry oldEntry = entries_.get(i);
         if (!oldEntry.isAlive())
         {
            entries_.remove(i);
            i--;
         }
      }

      return entry;
   }

   public static RequestLogEntry[] getEntries()
   {
      RequestLogEntry[] entries = new RequestLogEntry[entries_.size()];
      for (int i = 0; i < entries.length; i++)
         entries[i] = entries_.get(i).clone();
      return entries;
   }

   private static final ArrayList<RequestLogEntry> entries_ =
         new ArrayList<RequestLogEntry>();

   private static final int MAX_ENTRIES = 50;
}
