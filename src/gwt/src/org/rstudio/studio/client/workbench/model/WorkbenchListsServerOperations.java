/*
 * WorkbenchListsServerOperations.java
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

import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.JsArrayString;



public interface WorkbenchListsServerOperations 
{   
   void listGet(String listName, 
                ServerRequestCallback<JsArrayString> requestCallback);
   
   void listSetContents(String listName,
                        ArrayList<String> list,
                        ServerRequestCallback<Void> requestCallback);
   
   void listPrependItem(String listName,
                        String value,
                        ServerRequestCallback<Void> requestCallback);
   
   void listAppendItem(String listName,
                       String value,
                       ServerRequestCallback<Void> requestCallback);
   
   void listRemoveItem(String listName,
                       String value,
                       ServerRequestCallback<Void> requestCallback);
   
   void listClear(String listName,
                  ServerRequestCallback<Void> requestCallback);
}
