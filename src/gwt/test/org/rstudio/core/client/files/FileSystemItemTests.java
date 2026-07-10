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

   // Models the alias-relevant fields of a listing entry as emitted by
   // createFileSystemItem (SessionModuleContext.cpp): 'dir' carries the
   // target's directory-ness and 'alias_target' its resolved path. Not a
   // complete fixture -- fields like 'exists' are omitted.
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

   // Models the link-relevant fields of a listing entry as emitted by
   // createFileSystemItem (SessionModuleContext.cpp): is_symlink / is_alias
   // flags plus their targets. A null target models an absent field.
   private static native FileSystemItem createLinkEntry(String path,
                                                        boolean isSymlink,
                                                        boolean isAlias,
                                                        String symlinkTarget,
                                                        String aliasTarget) /*-{
      return {
         path: path,
         dir: false,
         length: -1,
         lastModified: 0,
         is_symlink: isSymlink,
         is_alias: isAlias,
         symlink_target: symlinkTarget,
         alias_target: aliasTarget
      };
   }-*/;

   public void testRegularFileIsNotLink()
   {
      FileSystemItem file = FileSystemItem.createFile("/home/user/file.txt");
      assertFalse(file.isSymlink());
      assertFalse(file.isAlias());
      assertFalse(file.isLink());
      assertNull(file.getSymlinkTarget());
      assertNull(file.getLinkTarget());
   }

   public void testSymlinkReportsTargetVerbatim()
   {
      // relative targets are surfaced verbatim, like `ls -l`
      FileSystemItem link =
            createLinkEntry("/home/user/link", true, false, "../shared/doc.txt", null);
      assertTrue(link.isSymlink());
      assertFalse(link.isAlias());
      assertTrue(link.isLink());
      assertEquals("../shared/doc.txt", link.getSymlinkTarget());
      assertEquals("../shared/doc.txt", link.getLinkTarget());
   }

   public void testAliasIsLinkAndFallsBackToAliasTarget()
   {
      FileSystemItem alias =
            createLinkEntry("/home/user/alias", false, true, null, "/home/user/doc.txt");
      assertFalse(alias.isSymlink());
      assertTrue(alias.isAlias());
      assertTrue(alias.isLink());
      assertNull(alias.getSymlinkTarget());
      assertEquals("/home/user/doc.txt", alias.getLinkTarget());
   }

   public void testBrokenAliasIsStillLinkWithNoTarget()
   {
      // a broken alias is flagged (is_alias) even though its target did not
      // resolve, so it is still badged; it has no target for the tooltip
      FileSystemItem alias =
            createLinkEntry("/home/user/broken", false, true, null, null);
      assertTrue(alias.isAlias());
      assertTrue(alias.isLink());
      assertNull(alias.getLinkTarget());
   }
}
