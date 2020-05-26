/*
 * FileType.java
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

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;

import java.util.ArrayList;

public abstract class FileType
{
   // Language modes -- for multi-mode documents
   public static final String R_LANG_MODE = "R";
   public static final String MARKDOWN_LANG_MODE = "Markdown";
   public static final String C_CPP_LANG_MODE = "C_CPP";
   public static final String TEX_LANG_MODE = "TeX";
   public static final String SQL_LANG_MODE = "SQL";
   public static final String STAN_LANG_MODE = "Stan";
   public static final String PYTHON_LANG_MODE = "Python";
   
   static ArrayList<FileType> ALL_FILE_TYPES = new ArrayList<>();

   protected FileType(String id)
   {
      id_ = id;
      ALL_FILE_TYPES.add(this);
   }

   public String getTypeId()
   {
      return id_;
   }

   public void openFile(FileSystemItem file, 
                        FilePosition position,
                        int navMethod,
                        EventBus eventBus)
   {
      openFile(file, null, navMethod, eventBus);
   }
   
   protected abstract void openFile(FileSystemItem file, EventBus eventBus);
   protected abstract FileIcon getDefaultFileIcon();
   
   private final String id_;
}
