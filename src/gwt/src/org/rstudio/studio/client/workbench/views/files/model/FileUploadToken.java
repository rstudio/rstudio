/*
 * FileUploadToken.java
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
package org.rstudio.studio.client.workbench.views.files.model;

import com.google.gwt.core.client.JavaScriptObject;

public class FileUploadToken extends JavaScriptObject
{
   protected FileUploadToken()
   {
   }

   public final native String getFilename() /*-{
      return this.filename;
   }-*/;

   public final native String getUploadTempFile() /*-{
      return this.uploadedTempFile;
   }-*/;

   public final native String getTargetDirectory() /*-{
      return this.targetDirectory;
   }-*/;

   public final native boolean getUnzipFound() /*-{
      return this.unzipFound;
   }-*/;

   public final native boolean getIsZip() /*-{
      return this.isZip;
   }-*/;
}
