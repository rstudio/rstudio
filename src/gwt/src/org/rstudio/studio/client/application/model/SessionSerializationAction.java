/*
 * SessionSerializationAction.java
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
package org.rstudio.studio.client.application.model;

import com.google.gwt.core.client.JavaScriptObject;

public class SessionSerializationAction extends JavaScriptObject
{
   public final static int SAVE_DEFAULT_WORKSPACE = 1;
   public final static int LOAD_DEFAULT_WORKSPACE = 2;
   public final static int SUSPEND_SESSION = 3;
   public final static int RESUME_SESSION = 4;
   public final static int COMPLETED = 5;
   
   protected SessionSerializationAction()
   {
      
   }
   
   public native final int getType() /*-{
      return this.type;
   }-*/;
   
   public native final String getTargetPath() /*-{
      return this.targetPath;
   }-*/;
}
