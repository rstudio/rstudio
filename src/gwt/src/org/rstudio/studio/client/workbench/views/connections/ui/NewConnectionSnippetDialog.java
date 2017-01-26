/*
 * NewConnectionSnippetDialog.java
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

import java.util.ArrayList;

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.HelpLink;

import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class NewConnectionSnippetDialog extends ModalDialog<ArrayList<NewConnectionSnippetParts>>
{
   public interface NewConnectionSnippetDialogStyle extends CssResource
   {
   }

   @Inject
   private void initialize()
   {
   }
   
   public NewConnectionSnippetDialog(
      OperationWithInput<ArrayList<NewConnectionSnippetParts>> operation,
      ArrayList<NewConnectionSnippetParts> config)
   {
      super("Configure Connection", operation);
      initialConfig_ = config;

      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Override
   protected void onDialogShown()
   {
      super.onDialogShown();

      setOkButtonCaption("Configure");

      HelpLink helpLink = new HelpLink(
         "TBD",
         "TBD",
         false);
      addLeftWidget(helpLink);   
   }
   
   @Override
   protected Widget createMainWidget()
   {
      widget_ = new VerticalPanel();
      
      return widget_;
   }
   
   @Override
   protected ArrayList<NewConnectionSnippetParts> collectInput()
   {
      return initialConfig_;
   }
   
   private Widget widget_;
   private ArrayList<NewConnectionSnippetParts> initialConfig_;
}
