/*
 * SourceNavigation.java
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
package org.rstudio.studio.client.workbench.views.source.model;


import com.google.gwt.core.client.JavaScriptObject;

public class SourceNavigation extends JavaScriptObject
{
   protected SourceNavigation()
   {
   }

   public static final native SourceNavigation create(
                                                 String document_id,
                                                 String path,
                                                 SourcePosition position) /*-{
      var sourceNavPosition = new Object();
      sourceNavPosition.document_id = document_id;
      sourceNavPosition.path = path;
      sourceNavPosition.position = position;
      return sourceNavPosition;
   }-*/;
   
   public native final String getDocumentId() /*-{
      return this.document_id;
   }-*/;
   
   public native final String getPath() /*-{
     return this.path;
   }-*/;  
   
   public native final SourcePosition getPosition() /*-{
      return this.position;
   }-*/;
   
   public final boolean isEqualTo(SourceNavigation other)
   {
      return isAtSameRowAs(other) &&
             (getPosition().getColumn() == other.getPosition().getColumn());
   }
   
   public final boolean isAtSameRowAs(SourceNavigation other)
   {
      if (other == null)
      {
         return false;
      }
      else
      {
         return getDocumentId() == other.getDocumentId() &&
                getPosition().isSameRowAs(other.getPosition());
      }
   }
   
   public final String toDebugString()
   {
      return (getPath() != null ? getPath() : getDocumentId()) + " (" +
             (getPosition().getContext() != null ? getPosition().getContext() + " = " : "") + 
             getPosition().getRow() + ", " + getPosition().getColumn() + ")";
   }
}
