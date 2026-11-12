/*
 * PackageNamePatternTests.java
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
package org.rstudio.studio.client.projects;

import com.google.gwt.junit.client.GWTTestCase;

// Names R's package.skeleton() accepts and rejects; the wizard must agree
// with it, or invalid names fail only after the wizard has closed
public class PackageNamePatternTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testAcceptsValidNames()
   {
      assertValid("mypkg");
      assertValid("ab");
      assertValid("my.pkg");
      assertValid("pkg2");
      assertValid("R6");
   }

   public void testRejectsHyphenAndUnderscore()
   {
      assertInvalid("test-cpp");
      assertInvalid("my_pkg");
   }

   public void testRejectsLeadingDigitOrDot()
   {
      assertInvalid("1pkg");
      assertInvalid(".pkg");
   }

   public void testRejectsTrailingDot()
   {
      assertInvalid("pkg.");
   }

   public void testRejectsSingleCharacter()
   {
      assertInvalid("x");
   }

   public void testRejectsEmptyAndWhitespace()
   {
      assertInvalid("");
      assertInvalid("my pkg");
   }

   private static void assertValid(String name)
   {
      assertTrue(name, Projects.PACKAGE_NAME_PATTERN.test(name));
   }

   private static void assertInvalid(String name)
   {
      assertFalse(name, Projects.PACKAGE_NAME_PATTERN.test(name));
   }
}
