/*
 * SVNChangelistTablePresenter.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.svn;

import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.vcs.SVNServerOperations;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.common.vcs.StatusAndPathInfo;

import java.util.ArrayList;

public class SVNChangelistTablePresenter
{
   @Inject
   public SVNChangelistTablePresenter(final SVNChangelistTable view,
                                      SVNServerOperations server)
   {
      view_ = view;
      server.svnStatus(new SimpleRequestCallback<JsArray<StatusAndPathInfo>>()
      {
         @Override
         public void onResponseReceived(JsArray<StatusAndPathInfo> response)
         {
            view.setItems(StatusAndPath.fromInfos(response));
         }
      });
   }

   public SVNChangelistTable getView()
   {
      return view_;
   }

   public ArrayList<StatusAndPath> getSelectedItems()
   {
      return view_.getSelectedItems();
   }

   private final SVNChangelistTable view_;
}
