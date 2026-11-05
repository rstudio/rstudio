/*
 * PosixFileSystemContextTests.java
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
package org.rstudio.core.client.files;

import com.google.gwt.junit.client.GWTTestCase;

public class PosixFileSystemContextTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testDotSegmentsCollapse()
   {
      assertEquals("/home/user", PosixFileSystemContext.normalizePath("/home/user/proj/.."));
      assertEquals("/home/user/b", PosixFileSystemContext.normalizePath("/home/user/./a/../b"));
      assertEquals("/home/user/a", PosixFileSystemContext.normalizePath("/home/user//a/"));
      assertEquals("~/b", PosixFileSystemContext.normalizePath("~/a/../b"));
      assertEquals("C:/Users", PosixFileSystemContext.normalizePath("C:/Users/me/.."));
   }

   public void testDotDotStopsAtRoot()
   {
      assertEquals("/", PosixFileSystemContext.normalizePath("/.."));
      assertEquals("/", PosixFileSystemContext.normalizePath("/home/../.."));
      assertEquals("C:/", PosixFileSystemContext.normalizePath("C:/.."));
      assertEquals("C:/", PosixFileSystemContext.normalizePath("C:/Users/../.."));
      assertEquals("//server/", PosixFileSystemContext.normalizePath("//server/share/../.."));
   }

   public void testHomeAliasKeepsLeadingDotDot()
   {
      assertEquals("~/..", PosixFileSystemContext.normalizePath("~/.."));
      assertEquals("~/../bob", PosixFileSystemContext.normalizePath("~/../bob"));
      assertEquals("~/../shared", PosixFileSystemContext.normalizePath("~/proj/../../shared"));
      assertEquals("~/..", PosixFileSystemContext.normalizePath("~/../a/.."));
   }

   public void testUncPathKeepsHostRoot()
   {
      assertEquals("//server/share/proj", PosixFileSystemContext.normalizePath("//server/share/proj"));
      assertEquals("//server/share", PosixFileSystemContext.normalizePath("//server/share/proj/.."));
      assertEquals("//server/share/b", PosixFileSystemContext.normalizePath("//server/share/./a/../b"));
      assertEquals("//server/", PosixFileSystemContext.normalizePath("//server"));
   }

   public void testRelativePathKeepsLeadingDotDot()
   {
      assertEquals("../x", PosixFileSystemContext.normalizePath("../x"));
      assertEquals("../../x", PosixFileSystemContext.normalizePath("../a/../../x"));
      assertEquals("x", PosixFileSystemContext.normalizePath("./x"));
   }

   public void testRootsAreUnchanged()
   {
      assertEquals("/", PosixFileSystemContext.normalizePath("/"));
      assertEquals("~", PosixFileSystemContext.normalizePath("~"));
      assertEquals("C:/", PosixFileSystemContext.normalizePath("C:/"));
      assertEquals("C:/", PosixFileSystemContext.normalizePath("C:"));
   }
}
