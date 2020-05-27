/*
 * PlainTextFileType.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import com.google.gwt.resources.client.ImageResource;
import org.rstudio.studio.client.common.reditor.EditorLanguage;

public class PlainTextFileType extends TextFileType
{
   PlainTextFileType(String id,
                     String label,
                     String defaultExtension,
                     ImageResource defaultIcon,
                     boolean wordWrap)
   {
      super(id, 
            label, 
            EditorLanguage.LANG_PLAIN, 
            defaultExtension, 
            defaultIcon, 
            wordWrap,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            true,
            false,
            false);
      
   }
}
