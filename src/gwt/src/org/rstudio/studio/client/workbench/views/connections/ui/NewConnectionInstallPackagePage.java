/*
 * NewConnectionInstallPackagePage.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.connections.ui;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.resources.ImageResourceUrl;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.workbench.views.connections.ConnectionsPresenter;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext.NewConnectionInfo;

import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class NewConnectionInstallPackagePage
      extends WizardPage<NewConnectionContext, ConnectionOptions>
{
   public NewConnectionInstallPackagePage(final NewConnectionInfo info)
   {
      super(info.getName(), "", info.getName() + " Connection",
            StringUtil.isNullOrEmpty(info.iconData()) ? null
                  : new ImageResourceUrl(new SafeUri()
                  {
                     @Override
                     public String asString()
                     {
                        return info.iconData();
                     }
                  }, 16, 16),
            null);

      info_ = info;

      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   @Inject
   private void initialize(DependencyManager dependencyManager,
                           ConnectionsPresenter connectionsPresenter)
   {
      dependencyManager_ = dependencyManager;
      connectionsPresenter_ = connectionsPresenter;
   }

   @Override
   public void onBeforeActivate(Operation operation, final ModalDialogBase wizard)
   {
      dependencyManager_.withConnectionPackage(
            info_.getName(), 
            info_.getPackage(), 
            info_.getVersion(), 
            new Operation()
            {
               @Override
               public void execute()
               {
                  wizard.closeDialog();
                  connectionsPresenter_.onNewConnection();
               }
            }
      );
   }

   @Override
   public void onActivate(ProgressIndicator indicator)
   {
   }

   @Override
   protected void initialize(NewConnectionContext initData)
   {
   }

   @Override
   protected boolean acceptNavigation()
   {
      return true;
   }

   @Override
   public void focus()
   {
   }

   @Override
   protected Widget createWidget()
   {
      Widget widget = new VerticalPanel(); 
      return widget;
   }

   @Override
   protected ConnectionOptions collectInput()
   {
      return null;
   }

   private NewConnectionInfo info_;
   private DependencyManager dependencyManager_;
   private ConnectionsPresenter connectionsPresenter_;
}
