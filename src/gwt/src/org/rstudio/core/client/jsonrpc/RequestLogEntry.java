/*
 * RequestLogEntry.java
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

import org.rstudio.core.client.CsvWriter;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;

public class RequestLogEntry
{
   public static class ResponseType
   {
      public static final int None = 0;
      public static final int Normal = 1;
      public static final int Error = 2;
      public static final int Cancelled = 3;
      public static final int Unknown = 4;
   }

   public RequestLogEntry(long requestTime,
                          String requestId,
                          String requestData)
   {
      requestTime_ = requestTime;
      requestId_ = requestId;
      requestData_ = requestData;
   }

   public long getRequestTime()
   {
      return requestTime_;
   }

   public String getRequestId()
   {
      return requestId_;
   }

   public String getRequestData()
   {
      return requestData_;
   }

   public Long getResponseTime()
   {
      return responseTime_;
   }

   public String getResponseData()
   {
      return responseData_;
   }

   public void logResponse(int responseType, String data)
   {
      responseType_ = responseType;
      responseTime_ = System.currentTimeMillis();
      responseData_ = data;
   }

   public int getResponseType()
   {
      return responseType_;
   }

   public boolean isAlive()
   {
      return responseType_ == ResponseType.None;
   }

   public String getRequestMethodName()
   {
      if (requestData_ == "[REDACTED]")
         return requestData_;

      Pattern p = Pattern.create("\\\"method\\\":\\s*\\\"([^\"]+)\\\"");
      Match match = p.match(requestData_, 0);
      if (match == null)
         return null;
      return match.getGroup(1);
   }

   public RequestLogEntry clone()
   {
      RequestLogEntry clone = new RequestLogEntry(requestTime_,
                                                  requestId_,
                                                  requestData_);
      clone.responseType_ = responseType_;
      clone.responseData_ = responseData_;
      clone.responseTime_ = responseTime_;
      return clone;
   }

   public void toCsv(CsvWriter writer)
   {
      writer.writeValue(requestTime_ + "");
      writer.writeValue(requestId_);
      writer.writeValue(requestData_);
      writer.writeValue(responseType_ + "");
      if (responseType_ != ResponseType.None)
      {
         writer.writeValue(responseTime_.toString());
         writer.writeValue(responseData_);
      }
      writer.endLine();
   }

   public static RequestLogEntry fromValues(String[] line)
   {
      if (line.length == 0 || (line.length == 1 && line[0].length() == 0))
         return null;
      
      long reqTime = Long.parseLong(line[0]);
      String reqId = line[1];
      String reqData = line[2];
      int respType = Integer.parseInt(line[3]);
      Long respTime = null;
      String respData = null;
      if (respType != ResponseType.None)
      {
         respTime = Long.parseLong(line[4]);
         respData = line[5];
      }
      RequestLogEntry entry = new RequestLogEntry(reqTime, reqId, reqData);
      entry.responseType_ = respType;
      entry.responseTime_ = respTime;
      entry.responseData_ = respData;
      return entry;
   }

   private final long requestTime_;
   private final String requestId_;
   private final String requestData_;
   private Long responseTime_;
   private String responseData_;
   private int responseType_ = ResponseType.None;
}
