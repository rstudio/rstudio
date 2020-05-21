/*
 * FileChange.java
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
package org.rstudio.studio.client.workbench.views.files.model;

import com.google.gwt.core.client.JavaScriptObject;
import org.rstudio.core.client.files.FileSystemItem;

public class FileChange extends JavaScriptObject
{
   // NOTE: missing 2 because we got rid of RENAME
   public final static int ADD = 1;
   public final static int DELETE = 3;
   public final static int MODIFIED = 4;
   
   public static final FileChange createAdd(FileSystemItem file)
   {
      return create(ADD, file);
   }
   
   public static final FileChange createDelete(FileSystemItem file)
   {
      return create(DELETE, file);
   }
   
   public static final FileChange createModified(FileSystemItem file)
   {
      return create(MODIFIED, file);
   }
  
   private static final native FileChange create(int type, 
                                                 FileSystemItem file) /*-{
      var fileViewAction = new Object();
      fileViewAction.type = type;
      fileViewAction.file = file;
      return fileViewAction;
   }-*/;
   
   protected FileChange()
   {
   }
   
   public final native int getType() /*-{
      return this.type;
   }-*/;

   public final native FileSystemItem getFile() /*-{
      return this.file;
   }-*/;
}
