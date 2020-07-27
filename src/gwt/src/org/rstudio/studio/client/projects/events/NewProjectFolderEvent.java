/*
 * NewProjectFolderEvent.java
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

package org.rstudio.studio.client.projects.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.application.model.TutorialApiCallContext;

public class NewProjectFolderEvent extends GwtEvent<NewProjectFolderEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onNewProjectFolderEvent(NewProjectFolderEvent event);
   }

   public NewProjectFolderEvent(String dirName,
                                String destDir,
                                boolean createRepo,
                                TutorialApiCallContext callContext)
   {
      dirName_ = dirName;
      destDir_ = destDir;
      createRepo_ = createRepo;
      callContext_ = callContext;
   }

   public String getDirName()
   {
      return dirName_;
   }

   public String getDestDir()
   {
      return destDir_;
   }

   public boolean getCreateRepo()
   {
      return createRepo_;
   }

   /**
    * @return info about api call that triggered this event, or null if not triggered by api
    */
   public TutorialApiCallContext getCallContext()
   {
      return callContext_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onNewProjectFolderEvent(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

   private final String dirName_;
   private final String destDir_;
   private final boolean createRepo_;
   private final TutorialApiCallContext callContext_;
}
