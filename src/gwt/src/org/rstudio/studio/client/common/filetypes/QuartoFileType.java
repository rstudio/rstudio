/*
 * QuartoFileType.java
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


import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.common.reditor.EditorLanguage;

public class QuartoFileType extends RWebContentFileType
{
   public QuartoFileType()
   {
      super("quarto_markdown", 
            "Quarto", 
            EditorLanguage.LANG_RMARKDOWN,
            ".qmd", 
            new ImageResource2x(FileIconResources.INSTANCE.iconQuarto2x()), 
            true,
            true,
            true);
   }
}
