/*
 * ObjectExplorerType.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.filetypes;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.application.events.EventBus;

public class ObjectExplorerFileType extends EditableFileType
{
   public ObjectExplorerFileType()
   {
      super("object_explorer",
            "Object Explorer",
            new ImageResource2x(FileIconResources.INSTANCE.iconObjectExplorer2x()));
   }

   @Override
   protected void openFile(FileSystemItem file, EventBus eventBus)
   {
      assert false :
         "Object explorer doesn't operate on filesystem files";
   }
}
