/*
 * WorkbenchLists.java
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

import java.util.ArrayList;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class WorkbenchLists extends JavaScriptObject
{   
   protected WorkbenchLists()
   {
      
   }
  
   public final ArrayList<String> getList(String name)
   {
      JsArrayString jsList = getListNative(name);
      return convertList(jsList);
   }
   
  
   private native final JsArrayString getListNative(String name) /*-{
      return this[name];
   }-*/;
   
   private ArrayList<String> convertList(JsArrayString jsList)
   {
      ArrayList<String> list = new ArrayList<String>();
      for (int i=0; i<jsList.length(); i++)
         list.add(jsList.get(i));
      return list;
   }
   
}
