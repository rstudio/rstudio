/*
 * RPubsPresenter.java
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
package org.rstudio.studio.client.common.rpubs;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.rpubs.model.RPubsServerOperations;
import org.rstudio.studio.client.common.rpubs.ui.RPubsUploadDialog;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.inject.Inject;

public class RPubsPresenter
{
   public interface Binder extends CommandBinder<Commands, RPubsPresenter>
   {}
   
   public interface Context
   {
      String getContextId();
      String getTitle();
      String getHtmlFile();
      boolean isPublished();
   }
   
   @Inject
   public RPubsPresenter(Binder binder,
                         Commands commands,
                         final GlobalDisplay globalDisplay,
                         EventBus eventBus,
                         RPubsServerOperations server)
   {
      binder.bind(commands, this);  
   }
   
   public void setContext(Context context)
   {
      context_ = context;
   }
   
   @Handler
   public void onPublishHTML()
   {
      RPubsUploadDialog dlg = new RPubsUploadDialog(context_.getTitle(),
                                                    context_.getHtmlFile(),
                                                    context_.isPublished(),
                                                    context_.getContextId());
      dlg.showModal();
   }

   private Context context_;
}
