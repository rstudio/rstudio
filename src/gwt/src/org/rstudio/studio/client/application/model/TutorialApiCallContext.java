/*
 * TutorialApiCallContext.java
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

public class TutorialApiCallContext
{
   /**
    * Stores context of an API call, both the name of the API that is being invoked, and
    * a string supplied by the caller to identify this call instance in callbacks
    * @param api name of the invoking API
    * @param callerID identifier for this call instance
    */
   public TutorialApiCallContext(String api, String callerID)
   {
      api_ = api;
      callerID_ = callerID;
   }
   
   public String getApi()
   {
      return api_;
   }
   
   public String getCallerID()
   {
      return callerID_;
   }
   
   private final String api_;
   private final String callerID_;
}
