/*
 * DatabaseObject.java
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
         parent: null,
         matches: true
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
   
   public final native void setMatches(boolean matches) /*-{
      this.matches = matches;
   }-*/;
   
   public final native boolean matches() /*-{
      // matches by default unless we have data showing otherwise
      return typeof this.matches === "undefined" ? true : !!this.matches;
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
