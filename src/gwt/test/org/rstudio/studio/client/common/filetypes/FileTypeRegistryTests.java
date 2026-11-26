/*
 * FileTypeRegistryTests.java
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

import org.rstudio.core.client.files.FileSystemItem;

public class FileTypeRegistryTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   @Override
   protected void gwtSetUp()
   {
      // file type and icon lookups don't use the injected dependencies
      registry_ = new FileTypeRegistry(null, null, null, null, null, null);
   }

   public void testContainerfileIsDockerfile()
   {
      FileSystemItem file = FileSystemItem.createFile("~/project/Containerfile");

      assertSame(FileTypeRegistry.DOCKERFILE, registry_.getTypeForFile(file));

      FileIcon dockerIcon =
            registry_.getIconForFile(FileSystemItem.createFile("~/project/Dockerfile"));
      assertEquals(dockerIcon.getImageResource().getName(),
                   registry_.getIconForFile(file).getImageResource().getName());
   }

   private FileTypeRegistry registry_;
}
