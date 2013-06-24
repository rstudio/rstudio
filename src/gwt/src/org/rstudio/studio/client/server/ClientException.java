/*
 * ClientException.java
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
package org.rstudio.studio.client.server;

import org.rstudio.core.client.StringUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class ClientException extends JavaScriptObject
{   
   public static final ClientException create(Throwable e) 
   {
      JsArray<StackItem> stack = JsArray.createArray().cast();
      for (StackTraceElement element : e.getStackTrace())
         stack.push(StackItem.create(element));
      
      return create(e.toString(),
                    GWT.getPermutationStrongName(),
                    stack);
   }
   
   public static native final ClientException create(
                                               String description,
                                               String permutation,
                                               JsArray<StackItem> stack) /*-{
      var ex = new Object();
      ex.description = description;
      ex.permutation = permutation;
      ex.stack = stack;
      return ex;
   }-*/;
   
   protected ClientException()
   {  
   }
   
   public native final String getDescription() /*-{
      return this.description;
   }-*/;
   
   public native final String getPermutation() /*-{
      return this.permutation;
   }-*/;
   
   public native final JsArray<StackItem> getStack() /*-{
      return this.stack;
   }-*/;
   
   public static class StackItem extends JavaScriptObject
   {
      public static final StackItem create(StackTraceElement element) 
      {
         return create(StringUtil.notNull(element.getFileName()),
                       StringUtil.notNull(element.getClassName()),
                       StringUtil.notNull(element.getMethodName()),
                       element.getLineNumber());
      }
      
      public static native final StackItem create(String fileName,
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
      
      protected StackItem()
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
