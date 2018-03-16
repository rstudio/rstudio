/*
 * PythonFileType.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

public class PythonFileType extends TextFileType
{
   PythonFileType(
         String id,
         String label,
         EditorLanguage editorLanguage,
         String defaultExtension,
         ImageResource defaultIcon)
   {
      super(id,
            label,
            editorLanguage,
            defaultExtension,
            defaultIcon,
            false, // word wrap
            false, // source on save
            true,  // execute code
            true,  // execute all code
            true,  // execute to current line
            false, // preview HTML 
            false, // knit to HTML
            false, // compile PDF
            false, // execute chunks
            true,  // auto-indent
            false, // check spelling
            false, // scope tree
            false  // preview from R
            );
   }
}
