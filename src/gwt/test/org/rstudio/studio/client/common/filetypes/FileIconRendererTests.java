/*
 * FileIconRendererTests.java
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

public class FileIconRendererTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   private static int countImages(String html)
   {
      return html.split("<img", -1).length - 1;
   }

   private static ImageResource badge()
   {
      return new ImageResource2x(FileIconResources.INSTANCE.iconLinkBadge2x());
   }

   public void testPlainIconRendersSingleImageWithoutWrapper()
   {
      String html = new FileIconRenderer().render(FileIcon.TEXT_ICON).asString();
      assertEquals(1, countImages(html));
      assertFalse("plain icon must not be wrapped", html.contains("<span"));
   }

   public void testBadgedIconWrapsBaseAndOverlaysBadge()
   {
      FileIcon badged =
            FileIcon.TEXT_ICON.withLinkBadge(badge(), "Symbolic link", "link -> target");
      String html = new FileIconRenderer().render(badged).asString();

      assertTrue("badged icon is wrapped", html.contains("<span"));
      assertEquals("base image plus badge image", 2, countImages(html));
      assertTrue("tooltip is rendered as a title", html.contains("title="));
   }

   public void testNullTooltipAndDescriptionDoNotEmitLiteralNull()
   {
      // FilesList always supplies a description, but the renderer must not turn
      // a null tooltip/description into the literal string "null"
      FileIcon badged = FileIcon.TEXT_ICON.withLinkBadge(badge(), null, null);
      String html = new FileIconRenderer().render(badged).asString();

      assertEquals(2, countImages(html));
      // the template uses single-quoted attributes; null must become "" (via
      // StringUtil.notNull), never the literal string "null"
      assertFalse(html.contains("title='null'"));
      assertFalse(html.contains("alt='null'"));
      assertTrue("null tooltip becomes an empty title", html.contains("title=''"));
   }
}
