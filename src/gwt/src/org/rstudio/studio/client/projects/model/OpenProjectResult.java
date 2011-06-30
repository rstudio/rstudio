/*
 * OpenProjectResult.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects.model;

import com.google.gwt.core.client.JavaScriptObject;

public class OpenProjectResult extends JavaScriptObject
{
   public final static int STATUS_OK = 0;
   public final static int STATUS_NOT_EXISTS = 1;
   public final static int STATUS_NO_WRITE_ACCESS = 3;
   
   protected OpenProjectResult()
   {
   }
   
   public native final int getStatus() /*-{
      return this.status;
   }-*/;
   
   public native final String getProjectFilePath() /*-{
      return this.project_file_path;
   }-*/;

}
