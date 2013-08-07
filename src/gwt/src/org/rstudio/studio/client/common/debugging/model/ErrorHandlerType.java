/*
 * ErrorHandlerType.java
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

public class ErrorHandlerType extends JavaScriptObject
{
   public static final int ERRORS_AUTOMATIC = 0;
   public static final int ERRORS_BREAK_ALWAYS = 1;
   public static final int ERRORS_BREAK_USER = 2;
   public static final int ERRORS_IGNORE = 3;
   public static final int ERRORS_CUSTOM = 4;
   
   protected ErrorHandlerType() {}

   public final native int getType() /*-{
      return this.type;
   }-*/;   
   
   public static String getNameOfType(int type)
   {
      switch(type)
      {
      case ERRORS_AUTOMATIC:
         return "Automatic";
      case ERRORS_BREAK_ALWAYS:
         return "Break Always";
      case ERRORS_BREAK_USER: 
         return "Break in My Code";
      case ERRORS_IGNORE:
         return "Ignore";
      case ERRORS_CUSTOM:
         return "Custom";
      }
      return "Unknown";
   }
}
