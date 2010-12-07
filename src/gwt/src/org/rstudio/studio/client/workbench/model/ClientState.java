/*
 * ClientState.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public final class ClientState extends JavaScriptObject
{
   protected ClientState()
   {
   }

   public static native ClientState create() /*-{
      return {
         temporary: {},
         persistent: {},
         set: function(group, name, value, persist) {
            var base = persist ? this.persistent : this.temporary;
            var grp = base[group];
            if (!grp)
               grp = base[group] = {};
            grp[name] = value;
            this.isEmpty = false;
         },
         isEmpty: true
      };
   }-*/;

   public native final JavaScriptObject getTemporaryData() /*-{
      return this.temporary;
   }-*/;

   public native final JavaScriptObject getPersistentData() /*-{
      return this.persistent;
   }-*/;

   public native final boolean isEmpty() /*-{
      return this.isEmpty;
   }-*/;

   public native final void putObject(String group,
                                      String name,
                                      JavaScriptObject value,
                                      boolean persist) /*-{
      this.set(group, name, value, persist);
   }-*/;

   public native final void putString(String group,
                                      String name,
                                      String value,
                                      boolean persist) /*-{
      this.set(group, name, value, persist);
   }-*/;

   public native final void putInt(String group,
                                   String name,
                                   int value,
                                   boolean persist) /*-{
      this.set(group, name, value, persist);
   }-*/;

   public native final void putBoolean(String group,
                                       String name,
                                       boolean value,
                                       boolean persist) /*-{
      this.set(group, name, value, persist);
   }-*/;

   public void putStrings(String group,
                          String name,
                          String[] value,
                          boolean persist)
   {
      JsArrayString array = JsArrayString.createArray().cast();
      for (String v : value)
         array.push(v);
      this.putObject(group, name, array, persist);
   }
}
