/*
 * Workspace.java
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
package org.rstudio.studio.client.workbench.views.workspace;


import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.BasePresenter;

public class Workspace
      extends BasePresenter
   implements IsWidget
{
   public interface Binder extends CommandBinder<Commands, Workspace> {}

   public interface Display extends WorkbenchView
   {
   }

   @Inject
   public Workspace(Workspace.Display view)
   {
      super(view);
   }
   
   @Override
   public void onBeforeSelected()
   {
   }

}
