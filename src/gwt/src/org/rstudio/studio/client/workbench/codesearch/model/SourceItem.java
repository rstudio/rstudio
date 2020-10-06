/*
 * SourceItem.java
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
package org.rstudio.studio.client.workbench.codesearch.model;

import com.google.gwt.core.client.JavaScriptObject;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.XRef;
import org.rstudio.core.client.js.JsObject;


public class SourceItem extends JavaScriptObject
{
   protected SourceItem()
   {
   }
   
   // NOTE: synchronize with class in SessionCodeSearch.cpp
   public static final int NONE       =  0;
   public static final int FUNCTION   =  1;
   public static final int METHOD     =  2;
   public static final int CLASS      =  3;
   public static final int ENUM       =  4;
   public static final int ENUM_VALUE =  5;
   public static final int NAMESPACE  =  6; 
   public static final int SECTION    =  7;
   public static final int FIGURE     =  8;
   public static final int TABLE      =  9;
   public static final int MATH       = 10;

   public final native int getType() /*-{
      return this.type;
   }-*/;
   
   public final native String getName() /*-{
      return this.name;
   }-*/;
   
   public final native String getParentName() /*-{
      return this.parent_name;
   }-*/;

   public final native String getExtraInfo() /*-{
      return this.extra_info;
   }-*/;

   // aliased path
   public final native String getContext() /*-{
      return this.context;
   }-*/;
   
   public final native int getLine() /*-{
      return this.line;
   }-*/;
   
   public final native int getColumn() /*-{
      return this.column;
   }-*/;
   
   public final native JsObject getMetadata() /*-{
      return this.metadata || {};
   }-*/;
   
   public final boolean hasXRef()
   {
      return getMetadata().hasKey("xref");
   }
   
   public final XRef getXRef()
   {
      return getMetadata().getObject("xref").cast();
   }

   public final CodeNavigationTarget toCodeNavigationTarget()
   {
      if (hasXRef())
      {
         XRef xref = getXRef();
         return new CodeNavigationTarget(
               getContext(),
               FilePosition.create(getLine(), getColumn()),
               xref);
      }
      else
      {
         return new CodeNavigationTarget(
               getContext(),
               FilePosition.create(getLine(), getColumn()));
      }
   }
}
