/*
 * SVNPane.java
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

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.vcs.common.ChangelistTable;
import org.rstudio.studio.client.workbench.views.vcs.svn.SVNPresenter.Display;

import java.util.ArrayList;

public class SVNPane extends WorkbenchPane implements Display
{
   @Inject
   public SVNPane(SVNChangelistTablePresenter changelistTablePresenter,
                  Session session,
                  Commands commands)
   {
      super(session.getSessionInfo().getVcsName());

      changelistTablePresenter_ = changelistTablePresenter;
      commands_ = commands;

      diffButton_ = new ToolbarButton(
            "Diff",
            commands_.vcsDiff().getImageResource(),
            (ClickHandler)null);
      addFilesButton_ = new ToolbarButton(
            "Add",
            commands_.vcsAddFiles().getImageResource(),
            (ClickHandler)null);
      deleteFilesButton_ = new ToolbarButton(
            "Delete",
            commands_.vcsRemoveFiles().getImageResource(),
            (ClickHandler)null);
      revertFilesButton_ = new ToolbarButton(
            "Revert",
            commands_.vcsRevert().getImageResource(),
            (ClickHandler)null);
      updateButton_ = new ToolbarButton(
            "Update",
            commands_.vcsPull().getImageResource(),
            (ClickHandler)null);
      commitButton_ = new ToolbarButton(
            "Commit",
            commands_.vcsCommit().getImageResource(),
            (ClickHandler)null);
   }

   @Override
   public void bringToFront()
   {
   }

   @Override
   public void setProgress(boolean showProgress)
   {
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();

      toolbar.addLeftWidget(diffButton_);
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(addFilesButton_);
      toolbar.addLeftWidget(deleteFilesButton_);
      toolbar.addLeftWidget(revertFilesButton_);
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(updateButton_);
//      toolbar.addLeftWidget(commitButton_);

      toolbar.addRightWidget(commands_.vcsRefresh().createToolbarButton());

      return toolbar;
   }

   @Override
   public ToolbarButton getDiffButton()
   {
      return diffButton_;
   }

   @Override
   public ToolbarButton getAddFilesButton()
   {
      return addFilesButton_;
   }

   @Override
   public ToolbarButton getDeleteFilesButton()
   {
      return deleteFilesButton_;
   }

   @Override
   public ToolbarButton getRevertFilesButton()
   {
      return revertFilesButton_;
   }

   @Override
   public ToolbarButton getUpdateButton()
   {
      return updateButton_;
   }

   @Override
   public ToolbarButton getCommitButton()
   {
      return commitButton_;
   }

   @Override
   public ArrayList<StatusAndPath> getSelectedItems()
   {
      return changelistTablePresenter_.getSelectedItems();
   }
   
   @Override
   public ChangelistTable getChangelistTable()
   {
      return changelistTablePresenter_.getView();
   }

   @Override
   protected Widget createMainWidget()
   {
      return changelistTablePresenter_.getView();
   }

   @Override
   public void onBeforeSelected()
   {
   }

   @Override
   public void onSelected()
   {
   }

   private final SVNChangelistTablePresenter changelistTablePresenter_;
   private final Commands commands_;
   private final ToolbarButton diffButton_;
   private final ToolbarButton addFilesButton_;
   private final ToolbarButton deleteFilesButton_;
   private final ToolbarButton revertFilesButton_;
   private final ToolbarButton updateButton_;
   private final ToolbarButton commitButton_;
}
