/*
 * ConnectionObjectSpecifier.java
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

import java.util.ArrayList;

import org.rstudio.core.client.JsArrayUtil;

import com.google.gwt.core.client.JsArray;

public class ConnectionObjectSpecifier
{
   public ConnectionObjectSpecifier()
   {
      containers_ = new ArrayList<ConnectionPathEntry>();
   }
   
   public ConnectionObjectSpecifier(String name, String type)
   {
      this();
      addPathEntry(name, type);
   }
   
   public void addPathEntry(ConnectionPathEntry entry)
   {
      containers_.add(entry);
   }
   
   public void addPathEntry(String name, String type)
   {
      containers_.add(ConnectionPathEntry.create(name, type));
   }
   
   public JsArray<ConnectionPathEntry> asJsArray()
   {
      return JsArrayUtil.toJsArray(containers_);
   }
   
   private final ArrayList<ConnectionPathEntry> containers_;
}
