/*
 * FindOutputPresenter.java
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
package org.rstudio.studio.client.workbench.views.find;

import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.inject.Inject;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.events.FindInFilesResultEvent;
import org.rstudio.studio.client.workbench.views.BasePresenter;

public class FindOutputPresenter extends BasePresenter
   implements FindInFilesResultEvent.Handler
{
   public interface Display extends WorkbenchView,
                                    HasSelectionHandlers<CodeNavigationTarget>
   {
      void addMatch(String path, int line, int column, String value);
      void ensureVisible();
   }

   @Inject
   public FindOutputPresenter(Display view,
                              GlobalDisplay globalDisplay,
                              final FileTypeRegistry ftr)
   {
      super(view);
      view_ = view;
      globalDisplay_ = globalDisplay;

      view_.addSelectionHandler(new SelectionHandler<CodeNavigationTarget>()
      {
         @Override
         public void onSelection(SelectionEvent<CodeNavigationTarget> event)
         {
            CodeNavigationTarget target = event.getSelectedItem();
            if (target == null)
               return;

            ftr.editFile(FileSystemItem.createFile(target.getFile()),
                         target.getPosition());
         }
      });
   }

   @Override
   public void onFindInFilesResult(FindInFilesResultEvent event)
   {
      view_.bringToFront();
   }

   @Handler
   public void onFindInFiles()
   {
      globalDisplay_.promptForText("Find", "Find:", "", new OperationWithInput<String>()
      {
         @Override
         public void execute(String input)
         {
            // TODO: Actually perform search
            // TODO: Show indication that search is in progress
            // TODO: Provide way to cancel a running search

            view_.ensureVisible();

            view_.addMatch("~/rstudio/COPYING", 10, 12, "Hello");
            view_.addMatch("~/rstudio/COPYING", 20, 12, "Hello2");
            view_.addMatch("~/rstudio/INSTALL", 6, 12, "Hello3");
            view_.addMatch("~/rstudio/INSTALL", 102, 12, "Hello4");
            view_.addMatch("~/rstudio/COPYING", 40, 12, "Hello5");
            view_.addMatch("~/rstudio/COPYING", 60, 12, "Hello6");
         }
      });
   }

   private final Display view_;
   private final GlobalDisplay globalDisplay_;
}
