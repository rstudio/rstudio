/*
 * DirEntryCheckBox.java
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
package org.rstudio.studio.client.rsconnect.ui;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;

public class DirEntryCheckBox extends Composite
{
   public DirEntryCheckBox(String path)
   {
      // create the appropriate filesystem object for the path
      FileSystemItem fsi = null;
      if (path.endsWith("/"))
      {
         path = path.substring(0, path.length() - 1);
         fsi = FileSystemItem.createDir(path);
      }
      else
      {
         fsi = FileSystemItem.createFile(path);
      }
      path_ = path;
      
      
      // add an icon representing the file
      ImageResource icon = 
            RStudioGinjector.INSTANCE.getFileTypeRegistry().getIconForFile(fsi);
      SafeHtmlBuilder hb = new SafeHtmlBuilder();
      hb.append(AbstractImagePrototype.create(icon).getSafeHtml());
      
      // insert the file/dir name into the checkbox
      hb.appendEscaped(path_);
      checkbox_ = new CheckBox(hb.toSafeHtml());
      
      initWidget(checkbox_);
   }
   
   public boolean getValue()
   {
      return checkbox_.getValue();
   }
   
   public void setValue(boolean checked)
   {
      checkbox_.setValue(checked);
   }
   
   public String getPath()
   {
      return path_;
   }
   
   public void setEnabled(boolean enabled)
   {
      checkbox_.setEnabled(enabled);
   }
   
   public boolean isEnabled()
   {
      return checkbox_.isEnabled();
   }
   
   private final String path_;
   private final CheckBox checkbox_;
}
