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

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.vcs.ChangelistTablePresenter;
import org.rstudio.studio.client.workbench.views.vcs.svn.SVNPresenter.Display;

public class SVNPane extends WorkbenchPane implements Display
{
   @Inject
   public SVNPane(ChangelistTablePresenter changelistTablePresenter,
                  Session session,
                  Commands commands)
   {
      super(session.getSessionInfo().getVcsName());

      changelistTablePresenter_ = changelistTablePresenter;
      commands_ = commands;
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

      toolbar.addLeftWidget(commands_.vcsRevert().createToolbarButton());

      toolbar.addRightWidget(commands_.vcsRefresh().createToolbarButton());

      return toolbar;
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

   private final ChangelistTablePresenter changelistTablePresenter_;
   private final Commands commands_;
}
