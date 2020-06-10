/*
 * SessionScopeTests.java
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

import com.google.gwt.junit.client.GWTTestCase;


public class SessionScopeTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }
   
   public void testFullScopeString()
   {
      SessionScope scope = new SessionScope("12345abcdefghijklmnop");
      assertEquals(scope.getUserId(), "12345");
      assertEquals(scope.getProjectId(), "abcdefgh");
      assertEquals(scope.getSessionId(), "ijklmnop");
   }
   
   public void testSessionOnlyScopeString()
   {
      SessionScope scope = new SessionScope("12345678");
      assertEquals(scope.getUserId(), null);
      assertEquals(scope.getProjectId(), null);
      assertEquals(scope.getSessionId(), "12345678");
   }

   public void testInvalidString()
   {
      SessionScope scope = new SessionScope("foo");
      assertEquals(scope.getUserId(), null);
      assertEquals(scope.getProjectId(), null);
      assertEquals(scope.getSessionId(), null);
   }
   
   public void testFullLocalHostUrl()
   {
      SessionScope scope = SessionScope.scopeFromUrl("http://localhost:8787/s/d5de8cfc78a31979f75db/");
      assertEquals(scope.getUserId(), "d5de8");
      assertEquals(scope.getProjectId(), "cfc78a31");
      assertEquals(scope.getSessionId(), "979f75db");
   }
   
   public void testFullLocalHostUrlNoTrailingSlash()
   {
      SessionScope scope = SessionScope.scopeFromUrl("http://localhost:8787/s/d5de8cfc78a31979f75db");
      assertEquals(scope.getUserId(), "d5de8");
      assertEquals(scope.getProjectId(), "cfc78a31");
      assertEquals(scope.getSessionId(), "979f75db");
   }
   
   public void testFullUrl()
   {
      SessionScope scope = SessionScope.scopeFromUrl("https://rsp.rstudioservices.com/s/f96bb2bd7dace6d420ad2/");
      assertEquals(scope.getUserId(), "f96bb");
      assertEquals(scope.getProjectId(), "2bd7dace");
      assertEquals(scope.getSessionId(), "6d420ad2");
   }
}
