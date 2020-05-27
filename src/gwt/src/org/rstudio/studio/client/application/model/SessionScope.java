/*
 * SessionScope.java
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

import org.rstudio.core.client.dom.DomUtils;

/**
 * Utility to crack apart components of a full or partially qualified
 * scope as found in session Urls.
 *
 * Assumes the session ID is present. The userId+ProjectId component
 * is optional.
 *
 * Caller needs to check each field for null.
 */
public class SessionScope
{
   public SessionScope(String sessionScope)
   {
      userId_ = null;
      projectId_ = null;
      sessionId_ = null;
      
      if (sessionScope.length() == SESSION_ID_LEN)
      {
         sessionId_ = sessionScope;
         return;
      }

      if (sessionScope.length() == (USER_ID_LEN + PROJECT_ID_LEN + SESSION_ID_LEN))
      {
         userId_ = sessionScope.substring(0, USER_ID_LEN);
         projectId_ = sessionScope.substring(USER_ID_LEN, USER_ID_LEN + PROJECT_ID_LEN);
         sessionId_ = sessionScope.substring(USER_ID_LEN + PROJECT_ID_LEN);
      }
      
      // Otherwise leave everything null.
   }
   
   /**
    * Parse session scope from a session URL.
    */
   public static SessionScope scopeFromUrl(String url)
   {
      String path = DomUtils.getUrlPath(url);
   
      // remove trailing slash, if any
      if (path.endsWith("/"))
         path = path.substring(0, path.length() - 1);
      
      // everything after final slash, if any
      int pos = path.lastIndexOf('/');
      if (pos >= 0)
         path = path.substring(pos + 1);

      return new SessionScope(path);
   }
   
   public String getUserId()
   {
      return userId_;
   }
   
   public String getProjectId()
   {
      return projectId_;
   }
   
   public String getSessionId()
   {
      return sessionId_;
   }
   
   public static final int USER_ID_LEN = 5;
   public static final int PROJECT_ID_LEN= 8;
   public static final int SESSION_ID_LEN = 8;
   
   private String userId_;
   private String projectId_;
   private String sessionId_;
}
