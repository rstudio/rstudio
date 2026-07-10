/*
 * FileIconTests.java
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
package org.rstudio.studio.client.common.filetypes;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.resources.client.ImageResource;

import org.rstudio.core.client.resources.ImageResource2x;

public class FileIconTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testWithLinkBadgeReturnsDecoratedCopy()
   {
      FileIcon base = FileIcon.TEXT_ICON;
      ImageResource badge =
            new ImageResource2x(FileIconResources.INSTANCE.iconLinkBadge2x());

      FileIcon badged = base.withLinkBadge(badge, "Symbolic link", "link -> target");

      assertNotSame(base, badged);
      assertSame(badge, badged.getBadgeResource());
      assertEquals("Symbolic link", badged.getBadgeDescription());
      assertEquals("link -> target", badged.getTooltip());

      // the base image and description are carried onto the copy
      assertSame(base.getImageResource(), badged.getImageResource());
      assertEquals(base.getDescription(), badged.getDescription());
   }

   public void testWithLinkBadgeDoesNotMutateSharedIcon()
   {
      // FileTypeRegistry hands back shared singleton icons; decorating one must
      // not leak the badge/tooltip onto every other row using the same icon
      FileIcon shared = FileIcon.FOLDER_ICON;
      assertNull(shared.getBadgeResource());
      assertNull(shared.getBadgeDescription());
      assertNull(shared.getTooltip());

      ImageResource badge =
            new ImageResource2x(FileIconResources.INSTANCE.iconLinkBadge2x());
      shared.withLinkBadge(badge, "Alias", "a -> b");

      assertNull(shared.getBadgeResource());
      assertNull(shared.getBadgeDescription());
      assertNull(shared.getTooltip());
   }
}
