/*
 * Data.java
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
package org.rstudio.studio.client.workbench.views.data;

import com.google.inject.Inject;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.data.model.DataServerOperations;

public class Data extends BasePresenter 
{
   public interface Display extends WorkbenchView
   {
     
   }
   
   @Inject
   public Data(Display display, 
               EventBus eventBus,
               DataServerOperations server)
   {
      super(display);
      display_ = display;
   }
   
  
   
   @SuppressWarnings("unused")
   private final Display display_ ;

   
}
