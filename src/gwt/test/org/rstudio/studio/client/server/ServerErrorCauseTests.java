/*
 * ServerErrorCauseTests.java
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
package org.rstudio.studio.client.server;

import com.google.gwt.junit.client.GWTTestCase;

public class ServerErrorCauseTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testSharingViolationMatchesWhateverTheMessageLanguage()
   {
      ServerErrorCause cause = new ServerErrorCause(
         32, "system",
         "Le processus ne peut pas acceder au fichier car ce fichier est utilise par un autre processus");
      assertTrue(cause.isWindowsSharingViolation());
   }

   public void testOtherSystemCodeDoesNotMatch()
   {
      // ERROR_ACCESS_DENIED
      ServerErrorCause cause = new ServerErrorCause(5, "system", "Access is denied");
      assertFalse(cause.isWindowsSharingViolation());
   }

   public void testCode32InAnotherCategoryDoesNotMatch()
   {
      // EPIPE in the generic (errno) category
      ServerErrorCause cause = new ServerErrorCause(32, "generic", "Broken pipe");
      assertFalse(cause.isWindowsSharingViolation());
   }

   public void testMissingCategoryDoesNotMatch()
   {
      ServerErrorCause cause = new ServerErrorCause(32, null, "");
      assertFalse(cause.isWindowsSharingViolation());
   }
}
