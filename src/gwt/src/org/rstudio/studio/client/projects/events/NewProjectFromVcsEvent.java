/*
 * NewProjectFromVcsEvent.java
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

public class NewProjectFromVcsEvent extends GwtEvent<NewProjectFromVcsEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onNewProjectFromVcsEvent(NewProjectFromVcsEvent event);
   }

   public NewProjectFromVcsEvent(String vcsId,
                                 String repoUrl,
                                 String dirName,
                                 String destDir,
                                 String username,
                                 TutorialApiCallContext callContext)
   {
      vcsId_ = vcsId;
      repoUrl_ = repoUrl;
      dirName_ = dirName;
      destDir_ = destDir;
      username_ = username;
      callContext_ = callContext;
   }

   public String getVcsId()
   {
      return vcsId_;
   }

   public String getRepoUrl()
   {
      return repoUrl_;
   }

   public String getDirName()
   {
      return dirName_;
   }

   public String getDestDir()
   {
      return destDir_;
   }

   public String getUsername()
   {
      return username_;
   }

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
      handler.onNewProjectFromVcsEvent(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

   private final String vcsId_;
   private final String repoUrl_;
   private final String dirName_;
   private final String destDir_;
   private final String username_;
   private final TutorialApiCallContext callContext_;
}
