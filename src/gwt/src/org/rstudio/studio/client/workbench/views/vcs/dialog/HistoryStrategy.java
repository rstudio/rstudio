/*
 * HistoryStrategy.java
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
package org.rstudio.studio.client.workbench.views.vcs.dialog;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.AbstractPager;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.view.client.HasData;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.DiffParser;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshHandler;

public interface HistoryStrategy
{
   void setRev(String rev);

   String idColumnName();
   boolean isBranchingSupported();
   boolean isShowFileSupported();
   boolean isSearchSupported();

   void setSearchText(HasValue<String> searchText);

   void setFileFilter(HasValue<FileSystemItem> fileFilter);

   void showFile(String revision,
                 String filename,
                 ServerRequestCallback<String> requestCallback);
   
   void saveFileAs(String revision,
                   String source,
                   String destination,
                   ProgressIndicator indicator);

   HandlerRegistration addVcsRefreshHandler(VcsRefreshHandler refreshHandler);

   void showCommit(String commitId,
                   boolean noSizeWarning,
                   ServerRequestCallback<String> requestCallback);

   void addDataDisplay(HasData<CommitInfo> display);
   void onRangeChanged(HasData<CommitInfo> display);

   void refreshCount();

   void initializeHistory(HasData<CommitInfo> dataDisplay);

   AbstractPager getPager();

   boolean getAutoSelectFirstRow();

   DiffParser createParserForCommit(String commitDiff);
   
   boolean getShowHistoryErrors();
}
