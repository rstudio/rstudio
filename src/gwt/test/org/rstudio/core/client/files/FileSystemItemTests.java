/*
 * FileSystemItemTests.java
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

public class FileSystemItemTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   // Mirrors a listing entry for a macOS Finder alias as emitted by
   // createFileSystemItem (SessionModuleContext.cpp): 'dir' carries the
   // target's directory-ness and 'alias_target' its resolved path.
   private static native FileSystemItem createAliasEntry(String path,
                                                         boolean dir,
                                                         String aliasTarget) /*-{
      return {
         path: path,
         dir: dir,
         length: -1,
         lastModified: 0,
         alias_target: aliasTarget
      };
   }-*/;

   public void testRegularFileHasNoAliasTarget()
   {
      FileSystemItem file = FileSystemItem.createFile("/home/user/file.txt");
      assertNull(file.getAliasTarget());
   }

   public void testResolveAliasTargetIsIdentityForRegularFile()
   {
      // the documented contract is that non-aliases come back unchanged,
      // not as a rebuilt item that would drop other metadata
      FileSystemItem file = FileSystemItem.createFile("/home/user/file.txt");
      assertSame(file, file.resolveAliasTarget());
   }

   public void testResolveAliasTargetIsIdentityForDirectory()
   {
      FileSystemItem dir = FileSystemItem.createDir("/home/user/folder");
      assertSame(dir, dir.resolveAliasTarget());
   }

   public void testResolveAliasTargetForDirectoryAlias()
   {
      FileSystemItem alias =
            createAliasEntry("/home/user/alias", true, "/home/user/target");
      assertEquals("/home/user/target", alias.getAliasTarget());

      FileSystemItem resolved = alias.resolveAliasTarget();
      assertEquals("/home/user/target", resolved.getPath());
      assertTrue(resolved.isDirectory());
   }

   public void testResolveAliasTargetForFileAlias()
   {
      FileSystemItem alias =
            createAliasEntry("/home/user/alias", false, "/home/user/doc.txt");

      FileSystemItem resolved = alias.resolveAliasTarget();
      assertEquals("/home/user/doc.txt", resolved.getPath());
      assertFalse(resolved.isDirectory());
   }
}
