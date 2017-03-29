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

   @Source("dirseparator_2x.png")
   DataResource dirseparator2x();

   @Source("home_2x.png")
   DataResource home2x();
   
   @Source("project_2x.png")
   DataResource project2x();
   
   @Source("home_2x.png")
   ImageResource homeImage2x();
   
   @Source("project_2x.png")
   ImageResource projectImage2x();

   ImageResource fade();   

   @Source("browse_2x.png")
   ImageResource browse2x();
}
