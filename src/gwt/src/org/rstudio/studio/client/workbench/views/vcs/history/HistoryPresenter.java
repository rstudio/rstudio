/*
 * HistoryPresenter.java
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
package org.rstudio.studio.client.workbench.views.vcs.history;

import com.google.inject.Inject;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.vcs.VCSServerOperations;

import java.util.ArrayList;

public class HistoryPresenter
{
   public interface Display
   {
      void setData(ArrayList<CommitInfo> commits);
      void showModal();
   }

   @Inject
   public HistoryPresenter(VCSServerOperations server,
                           final Display view)
   {
      server_ = server;
      view_ = view;

      server_.vcsHistory("",
                         -1,
                         new SimpleRequestCallback<RpcObjectList<CommitInfo>>()
      {
         @Override
         public void onResponseReceived(RpcObjectList<CommitInfo> response)
         {
            view.setData(response.toArrayList());
         }
      });
   }

   public void showModal()
   {
      view_.showModal();
   }

   private final VCSServerOperations server_;
   private final Display view_;
}
