/*
 * FileDialogResources.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.core.client.files.filedialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.ImageResource;

public interface FileDialogResources extends ClientBundle
{
   public static final FileDialogResources INSTANCE =
         GWT.create(FileDialogResources.class);

   @Source("FileDialogStyles.css")
   FileDialogStyles styles();

   @Source("dirseparator.png")
   DataResource dirseparator();

   @Source("home.png")
   DataResource home();
   
   @Source("home.png")
   ImageResource homeImage();

   ImageResource fade();   

   ImageResource browse();
}
