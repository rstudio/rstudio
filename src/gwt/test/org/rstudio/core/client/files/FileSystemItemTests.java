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
   // createFileSystemItem (SessionModuleContext.cpp): is_symlink / is_alias /
   // is_shortcut flags plus their targets. A null target models an absent
   // field. Shortcut targets travel in alias_target (see the backend contract).
   private static native FileSystemItem createLinkEntry(String path,
                                                        boolean dir,
                                                        boolean isSymlink,
                                                        boolean isAlias,
                                                        boolean isShortcut,
                                                        String symlinkTarget,
                                                        String aliasTarget) /*-{
      return {
         path: path,
         dir: dir,
         length: -1,
         lastModified: 0,
         is_symlink: isSymlink,
         is_alias: isAlias,
         is_shortcut: isShortcut,
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
            createLinkEntry("/home/user/link", false, true, false, false, "../shared/doc.txt", null);
      assertTrue(link.isSymlink());
      assertFalse(link.isAlias());
      assertTrue(link.isLink());
      assertEquals("../shared/doc.txt", link.getSymlinkTarget());
      assertEquals("../shared/doc.txt", link.getLinkTarget());
   }

   public void testAliasIsLinkAndFallsBackToAliasTarget()
   {
      FileSystemItem alias =
            createLinkEntry("/home/user/alias", false, false, true, false, null, "/home/user/doc.txt");
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
            createLinkEntry("/home/user/broken", false, false, true, false, null, null);
      assertTrue(alias.isAlias());
      assertTrue(alias.isLink());
      assertNull(alias.getLinkTarget());
   }

   public void testSymlinkWithUnreadableTargetIsStillLink()
   {
      // when the backend cannot read the target it omits symlink_target; the
      // entry is still a link, with no target for the tooltip
      FileSystemItem link =
            createLinkEntry("/home/user/deadlink", false, true, false, false, null, null);
      assertTrue(link.isSymlink());
      assertTrue(link.isLink());
      assertNull(link.getSymlinkTarget());
      assertNull(link.getLinkTarget());
   }

   public void testDirectorySymlinkReportsDirectory()
   {
      // a symlink to a directory follows the link, so dir is true; it is still
      // flagged as a symlink and badged
      FileSystemItem link =
            createLinkEntry("/home/user/dirlink", true, true, false, false, "/home/user/dir", null);
      assertTrue(link.isDirectory());
      assertTrue(link.isSymlink());
      assertTrue(link.isLink());
      assertEquals("/home/user/dir", link.getLinkTarget());
   }

   public void testRegularFileIsNotShortcut()
   {
      FileSystemItem file = FileSystemItem.createFile("/home/user/file.txt");
      assertFalse(file.isShortcut());
   }

   public void testShortcutIsLinkAndFallsBackToAliasTarget()
   {
      // Windows shortcut targets travel in alias_target, so getLinkTarget
      // and resolveAliasTarget work for shortcuts without shortcut-specific
      // plumbing; is_shortcut is what makes the entry a link at all
      // (isLink) and selects the badge label
      FileSystemItem shortcut =
            createLinkEntry("/home/user/doc.txt.lnk", false, false, false, true, null, "/home/user/doc.txt");
      assertFalse(shortcut.isSymlink());
      assertFalse(shortcut.isAlias());
      assertTrue(shortcut.isShortcut());
      assertTrue(shortcut.isLink());
      assertNull(shortcut.getSymlinkTarget());
      assertEquals("/home/user/doc.txt", shortcut.getLinkTarget());
   }

   public void testBrokenShortcutIsStillLinkWithNoTarget()
   {
      // a broken shortcut is flagged (is_shortcut) even though its target did
      // not resolve, so it is still badged; it has no target for the tooltip
      FileSystemItem shortcut =
            createLinkEntry("/home/user/broken.lnk", false, false, false, true, null, null);
      assertTrue(shortcut.isShortcut());
      assertTrue(shortcut.isLink());
      assertNull(shortcut.getLinkTarget());
   }

   public void testDirectoryShortcutReportsDirectory()
   {
      // the backend derives dir-ness from the shortcut's target, and
      // resolveAliasTarget must yield the navigable target directory --
      // this is the contract the Files pane navigation relies on
      FileSystemItem shortcut =
            createLinkEntry("/home/user/dir.lnk", true, false, false, true, null, "/home/user/dir");
      assertTrue(shortcut.isDirectory());
      assertTrue(shortcut.isLink());

      FileSystemItem resolved = shortcut.resolveAliasTarget();
      assertEquals("/home/user/dir", resolved.getPath());
      assertTrue(resolved.isDirectory());
   }
}
