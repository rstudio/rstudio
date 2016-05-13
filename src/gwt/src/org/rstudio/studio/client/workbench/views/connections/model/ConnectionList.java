/*
 * Connection.java
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


package org.rstudio.studio.client.workbench.views.connections.model;

import java.util.ArrayList;

import org.rstudio.studio.client.workbench.WorkbenchList;
import org.rstudio.studio.client.workbench.events.ListChangedEvent;
import org.rstudio.studio.client.workbench.events.ListChangedHandler;

public class ConnectionList
{
   public ConnectionList(WorkbenchList workbenchList)
   {
      workbenchList_ = workbenchList;
      workbenchList_.addListChangedHandler(new ListChangedHandler() {
         @Override
         public void onListChanged(ListChangedEvent event)
         {
            connections_ = event.getList();
         }
      });
   }

   private WorkbenchList workbenchList_;
   @SuppressWarnings("unused")
   private ArrayList<String> connections_;
}
