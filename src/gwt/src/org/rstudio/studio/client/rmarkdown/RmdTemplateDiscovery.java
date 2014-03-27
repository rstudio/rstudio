/*
 * RmdTemplateDiscovery.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.rmarkdown;


import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;

import org.rstudio.studio.client.rmarkdown.events.RmdTemplateDiscoveredEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdTemplateDiscoveryCompletedEvent;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;

import com.google.inject.Inject;

public class RmdTemplateDiscovery implements 
   RmdTemplateDiscoveredEvent.Handler,
   RmdTemplateDiscoveryCompletedEvent.Handler
{
   @Inject
   public RmdTemplateDiscovery(EventBus eventBus, 
                               RMarkdownServerOperations server)
   {
      eventBus.addHandler(RmdTemplateDiscoveredEvent.TYPE, this);
      eventBus.addHandler(RmdTemplateDiscoveryCompletedEvent.TYPE, this);
   }
   
   @Override
   public void onRmdTemplateDiscovered(RmdTemplateDiscoveredEvent event)
   {

   }
   
   @Override
   public void onRmdTemplateDiscoveryCompleted(
         RmdTemplateDiscoveryCompletedEvent event)
   {

   }
   
   public void discoverTemplates(
         OperationWithInput<String> onTemplateDiscovered,
         Operation onCompleted)
   {
      
   }
}
