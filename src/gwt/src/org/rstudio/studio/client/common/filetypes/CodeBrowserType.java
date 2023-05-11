/*
 * CodeBrowserType.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.StudioClientCommonConstants;

public class CodeBrowserType extends EditableFileType
{
   public CodeBrowserType()
   {
      super("r_code_browser", constants_.rCodeBrowserLabel(),
            new ImageResource2x(FileIconResources.INSTANCE.iconRdoc2x()));
   }

   @Override
   public void openFile(FileSystemItem file, EventBus eventBus)
   {
      assert false : "CodeBrowserType doesn't operate on filesystem files";
   }
   private static final StudioClientCommonConstants constants_ = GWT.create(StudioClientCommonConstants.class);
}
