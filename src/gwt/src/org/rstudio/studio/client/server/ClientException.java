/*
 * ClientException.java
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
package org.rstudio.studio.client.server;

import org.rstudio.core.client.StringUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class ClientException extends JavaScriptObject
{   
   public static final ClientException create(Throwable e) 
   {
      JsArray<StackElement> stack = JsArray.createArray().cast();
      for (StackTraceElement element : e.getStackTrace())
         stack.push(StackElement.create(element));
      
      return create(StringUtil.notNull(e.getMessage()),
                    GWT.getPermutationStrongName(),
                    stack);
   }
   
   public static native final ClientException create(
                                               String message,
                                               String strongName,
                                               JsArray<StackElement> stack) /*-{
      var ex = new Object();
      ex.message = message;
      ex.strong_name = strongName;
      ex.stack = stack;
      return ex;
   }-*/;
   
   protected ClientException()
   {  
   }
   
   public native final String getMessage() /*-{
      return this.message;
   }-*/;
   
   public native final String getStrongName() /*-{
      return this.strong_name;
   }-*/;
   
   public native final JsArray<StackElement> getStack() /*-{
      return this.stack;
   }-*/;
   
   public static class StackElement extends JavaScriptObject
   {
      public static final StackElement create(StackTraceElement element) 
      {
         return create(StringUtil.notNull(element.getFileName()),
                       StringUtil.notNull(element.getClassName()),
                       StringUtil.notNull(element.getMethodName()),
                       element.getLineNumber());
      }
      
      public static native final StackElement create(String fileName,
                                                  String className,
                                                  String methodName,
                                                  int lineNumber) /*-{
         var item = new Object();
         item.file_name = fileName;
         item.class_name = className;
         item.method_name = methodName;
         item.line_number = lineNumber; 
         return item;       
      }-*/;
      
      protected StackElement()
      {
      }
      
      public native final String getFileName() /*-{
         return this.file_name;
      }-*/;
      
      public native final String getClassName() /*-{
         return this.class_name;
      }-*/;
      
      public native final String getMethodName() /*-{
         return this.method_name;
      }-*/;
      
      public native final String getLineNumber() /*-{
         return this.line_number;
      }-*/; 
   }
}
