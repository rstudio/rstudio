/*
 * ClientState.java
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
package org.rstudio.studio.client.workbench.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public final class ClientState extends JavaScriptObject
{
   public static final int TEMPORARY = 0;
   public static final int PERSISTENT = 1;
   public static final int PROJECT_PERSISTENT = 2;
   
   protected ClientState()
   {
   }

   public static native ClientState create() /*-{
      return {
         temporary: {},
         persistent: {},
         project_persistent: {},
         set: function(group, name, value, persist) {
            var base = this.temporary;
            if (persist == 1)
               base = this.persistent;
            else if (persist == 2)
               base = this.project_persistent;
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
   
   public native final JavaScriptObject getProjectPersistentData() /*-{
      return this.project_persistent;
   }-*/;

   public native final boolean isEmpty() /*-{
      return this.isEmpty;
   }-*/;

   public native final void putObject(String group,
                                      String name,
                                      JavaScriptObject value,
                                      int persist) /*-{
      this.set(group, name, value, persist);
   }-*/;

   public native final void putString(String group,
                                      String name,
                                      String value,
                                      int persist) /*-{
      this.set(group, name, value, persist);
   }-*/;

   public native final void putInt(String group,
                                   String name,
                                   int value,
                                   int persist) /*-{
      this.set(group, name, value, persist);
   }-*/;

   public native final void putBoolean(String group,
                                       String name,
                                       boolean value,
                                       int persist) /*-{
      this.set(group, name, value, persist);
   }-*/;

   public void putStrings(String group,
                          String name,
                          String[] value,
                          int persist)
   {
      JsArrayString array = JsArrayString.createArray().cast();
      for (String v : value)
         array.push(v);
      this.putObject(group, name, array, persist);
   }
}
