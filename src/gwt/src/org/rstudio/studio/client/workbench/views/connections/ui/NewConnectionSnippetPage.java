/*
 * NewConnectionSnippetPage.java
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
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext.NewConnectionInfo;

import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.user.client.ui.Widget;

public class NewConnectionSnippetPage 
   extends WizardPage<NewConnectionContext, ConnectionOptions>
{
   public NewConnectionSnippetPage(final NewConnectionInfo info)
   {
      super(
         info.getName(),
         "",
         info.getName() + " Connection",
         StringUtil.isNullOrEmpty(info.iconData()) ? null : new ImageResourceUrl(
            new SafeUri()
            {
               @Override
               public String asString()
               {
                  return info.iconData();
               }
            },
            16,
            16
         ),
         null);
      
      info_ = info;
   }

   @Override
   public void focus()
   {
   }

   @Override
   public void onBeforeActivate(Operation operation, ModalDialogBase wizard)
   {
      contents_.onBeforeActivate(operation, info_);
   }
   
   @Override
   public void onActivate(ProgressIndicator indicator)
   {
   }

   @Override
   public void onDeactivate(Operation operation)
   {
      contents_.onDeactivate(operation);
   }
   
   @Override
   protected Widget createWidget()
   {
      contents_ = new NewConnectionSnippetHost();

      return contents_;
   }

   @Override
   protected void initialize(NewConnectionContext initData)
   {
   }

   @Override
   public HelpLink getHelpLink()
   {
      if (StringUtil.isNullOrEmpty(info_.getHelp()))
         return null;

      return new HelpLink(
         "Using " + info_.getName(),
         info_.getHelp(),
         false,
         false);
   }

   @Override
   protected ConnectionOptions collectInput()
   {
      return contents_.collectInput();
   }

   @Override
   protected String getWizardPageBackgroundStyle()
   {
      return NewConnectionWizard.RES.styles().newConnectionWizardBackground();
   }
   
   private NewConnectionSnippetHost contents_;
   private NewConnectionInfo info_;
}
