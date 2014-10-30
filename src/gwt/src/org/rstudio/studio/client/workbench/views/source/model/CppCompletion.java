/*
 * CppCompletion.java
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
package org.rstudio.studio.client.workbench.views.source.model;

import com.google.gwt.core.client.JavaScriptObject;

public class CppCompletion extends JavaScriptObject
{
   protected CppCompletion()
   {
   }
   
   public static native final CppCompletion create(String typedText) /*-{
      return {
         kind: 1,
         typed_text: typedText,
         text: null
      };
   }-*/;
   
   //
   // Declaration kinds (note there are additional cursor kinds
   // but they don't apply to declarations)
   //
   
   public static final int UNEXPOSED_DECL = 1;
   public static final int STRUCT_DECL = 2;
   public static final int UNION_DECL = 3;
   public static final int CLASS_DECL = 4;
   public static final int ENUM_DECL = 5;
   public static final int FIELD_DECL = 6;
   public static final int ENUM_CONSTANT_DECL = 7;
   public static final int FUNCTION_DECL = 8;
   public static final int VAR_DECL = 9;
   public static final int PARM_DECL = 10;
   /* 11-19 are for Objective C */
   public static final int TYPEDEF_DECL = 20;
   public static final int CXX_METHOD = 21;
   public static final int NAMESPACE = 22;
   public static final int LINKAGE_SPEC = 23;
   public static final int CONSTRUCTOR = 24;
   public static final int DESTRUCTOR = 25;
   public static final int CONVERSION_FUNCTION = 26;
   public static final int TEMPLATE_TYPE_PARAM = 27;
   public static final int NON_TEMPLATE_TYPE_PARAM = 28;
   public static final int TEMPLATE_TEMPLATE_PARAM = 28;
   public static final int FUNCTION_TEMPLATE = 30;
   public static final int CLASS_TEMPLATE = 31;
   public static final int CLASS_TEMPLATE_PARTIAL_SPECIALIZATION = 32;
   public static final int NAMESPACE_ALIAS = 33;
   public static final int USING_DIRECTIVE = 34;
   public static final int USING_DECLARATION = 35;
   public static final int TYPE_ALIAS_DECL = 36;
   /* 37-38 are for Objective C */
   public static final int CXX_ACCESS_SPECIFIER = 39;
   
   public native final int getKind() /*-{
       return this.kind;
   }-*/;    
   
   public final boolean isFunction()
   {
      int kind = getKind();
      return kind == FUNCTION_DECL ||
             kind == CXX_METHOD ||
             kind == FUNCTION_TEMPLATE ||
             kind == CONSTRUCTOR ||
             kind == DESTRUCTOR;
   }
   
   public final boolean isVariable()
   {
      int kind = getKind();
      return kind == FIELD_DECL ||
             kind == VAR_DECL;
   }
   
   public native final String getTypedText() /*-{
      return this.typed_text;
   }-*/;
   
   public native final String getText() /*-{
      return this.text;
   }-*/;
}
