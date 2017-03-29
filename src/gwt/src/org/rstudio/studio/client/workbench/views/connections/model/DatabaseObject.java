/*
 * DatabaseObject.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.connections.model;

import com.google.gwt.core.client.JavaScriptObject;

public class DatabaseObject extends JavaScriptObject
{ 
   protected DatabaseObject()
   {
   }
  
   public static native final DatabaseObject create(String name, String type) /*-{ 
      return {
         name: name,
         type: type,
         parent: null
      }; 
   }-*/;
   
   public final native String getName() /*-{
      return this.name;
   }-*/;
   
   public final native String getType() /*-{
      return this.type;
   }-*/;
   
   public final native DatabaseObject getParent() /*-{
      return this.parent;
   }-*/;
   
   public final native void setParent(DatabaseObject parent) /*-{
      this.parent = parent;
   }-*/;
   
   public final boolean isEqualTo(DatabaseObject other) 
   {
      return getName()   == other.getName() &&
             getType()   == other.getType() &&
             getParent() == other.getParent();
   }
   
   public final ConnectionObjectSpecifier createSpecifier()
   {
      final ConnectionObjectSpecifier parent = getParent() == null ?
            new ConnectionObjectSpecifier() : getParent().createSpecifier();
      parent.addPathEntry(getName(), getType());
      return parent;
   }
}