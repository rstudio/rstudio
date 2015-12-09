/*
 * VcsFilesChangedCallback.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.vcs.common;

import java.util.ArrayList;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsFileContentsChangedEvent;

public class VcsFilesChangedCallback<T> extends SimpleRequestCallback<T>
{
   public VcsFilesChangedCallback(String caption, ArrayList<String> paths)
   {
      super(caption);
      paths_ = paths;
   }
   
   @Override
   public void onResponseReceived(T result)
   {
      RStudioGinjector.INSTANCE.getEventBus().fireEvent(
            new VcsFileContentsChangedEvent(paths_));
   }
   
   private final ArrayList<String> paths_;
}
