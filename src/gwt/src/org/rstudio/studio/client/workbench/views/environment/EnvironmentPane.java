/*
 * EnvironmentPane.java
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

package org.rstudio.studio.client.workbench.views.environment;

import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.environment.model.RObject;
import org.rstudio.studio.client.workbench.views.environment.view.EnvironmentObjects;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class EnvironmentPane extends WorkbenchPane 
                             implements EnvironmentPresenter.Display
{
   @Inject
   public EnvironmentPane(Commands commands)
   {
      super("Environment");
      
      commands_ = commands;
      
      ensureWidget();
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      
      toolbar.addRightWidget(commands_.refreshEnvironment().createToolbarButton());
      
      return toolbar;
   }
   
   @Override
   protected Widget createMainWidget()
   {
      objects_ = new EnvironmentObjects();
      return objects_;
   }

   @Override
   public void addObject(RObject object)
   {
      String itemText = object.getName() + " - " +
                        object.getType() + 
                        " [" + object.getLength() + "]";
      objects_.addObject(itemText);
   }
   
   @Override
   public void setBrowseMode(boolean browseMode)
   {
      objects_.setBrowseMode(browseMode);
   }

   @Override
   public void clearObjects()
   {
      objects_.clearObjects();
   }
   
   private EnvironmentObjects objects_;
   private Commands commands_;
}
