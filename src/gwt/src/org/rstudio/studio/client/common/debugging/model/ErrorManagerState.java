/*
 * ErrorManagerState.java
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

package org.rstudio.studio.client.common.debugging.model;

import com.google.gwt.core.client.JavaScriptObject;

public class ErrorManagerState extends JavaScriptObject
{
   protected ErrorManagerState() {}
   
   public final native int getErrorHandlerType() /*-{
      return this.error_handler_type;
   }-*/;
   
   public final native void setErrorHandlerType(int type) /*-{
      this.error_handler_type = type;
   }-*/;

   public final native boolean getUserCodeOnly() /*-{
      return this.user_code_only;
   }-*/;
   
   public final native void setUserCodeOnly(boolean userCode) /*-{
     this.user_code_only = userCode;
   }-*/;   
} 
