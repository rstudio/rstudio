/*
 * ServerErrorCause.java
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
package org.rstudio.studio.client.server;

public class ServerErrorCause
{
   public ServerErrorCause(int code, String category, String message)
   {
      code_ = code;
      category_ = category;
      message_ = message;
   }
  
   public int getCode() { return code_; }
   public String getCategory() { return category_; }
   public String getMessage() { return message_; }

   @Override
   public String toString()
   {
      return code_ + ": [" + category_ + "] " + message_;
   }

   private final int code_;
   private final String category_;
   private final String message_;
   
   public static final int FILE_NOT_FOUND = 2;
}
