/*
 * RetryConfig.java
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

package org.rstudio.studio.client.server.remote;

class RetryConfig
{
   public RetryConfig(int retryCount, int retrySleep)
   {
      retryCount_ = retryCount;
      retrySleep_ = retrySleep;
   }

   public int getCount() {
      return retryCount_;
   }

   public void setCount(int retryCount) {
      retryCount_ = retryCount;
   }

   public int getSleep() {
      return retrySleep_;
   }

   public void setSleep(int retrySleep) {
      retrySleep_ = retrySleep;
   }
   
   private int retryCount_;
   private int retrySleep_;
}
