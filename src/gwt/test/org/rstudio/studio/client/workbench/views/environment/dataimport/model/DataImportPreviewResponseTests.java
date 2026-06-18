/*
 * DataImportPreviewResponseTests.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.views.environment.dataimport.model;

import com.google.gwt.junit.client.GWTTestCase;

public class DataImportPreviewResponseTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   // Regression #17985: getErrorMessage assumed error.message was always an
   // array and called .join on it. When the C++ preview subprocess returned a
   // bare string (e.g. "Operation finished with error code.") this threw a
   // TypeError and the error was silently swallowed, leaving the preview
   // hanging. getErrorMessage must tolerate both shapes.

   public void testArrayMessageIsJoined()
   {
      DataImportPreviewResponse response = responseWithArrayMessage();
      assertEquals("Operation failed. details", response.getErrorMessage());
   }

   public void testStringMessageIsReturnedVerbatim()
   {
      DataImportPreviewResponse response =
            responseWithStringMessage("Operation finished with error code.");
      assertEquals("Operation finished with error code.", response.getErrorMessage());
   }

   public void testMissingErrorReturnsNull()
   {
      assertNull(responseWithoutError().getErrorMessage());
   }

   public void testMissingMessageReturnsNull()
   {
      assertNull(responseWithoutMessage().getErrorMessage());
   }

   private static native DataImportPreviewResponse responseWithArrayMessage() /*-{
      return { error: { message: ["Operation failed.", "details"] } };
   }-*/;

   private static native DataImportPreviewResponse responseWithStringMessage(String msg) /*-{
      return { error: { message: msg } };
   }-*/;

   private static native DataImportPreviewResponse responseWithoutError() /*-{
      return {};
   }-*/;

   private static native DataImportPreviewResponse responseWithoutMessage() /*-{
      return { error: {} };
   }-*/;
}
