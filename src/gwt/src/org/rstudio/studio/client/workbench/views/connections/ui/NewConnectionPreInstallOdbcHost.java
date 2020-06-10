/*
 * NewConnectionPreInstallOdbcHost.java
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

package org.rstudio.studio.client.workbench.views.connections.ui;


import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.rstudioapi.DialogHtmlSanitizer;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionsServerOperations;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class NewConnectionPreInstallOdbcHost extends Composite
{
   interface Binder extends UiBinder<Widget, NewConnectionPreInstallOdbcHost>
   {}

   @Inject
   private void initialize(ConnectionsServerOperations server)
   {
      server_ = server;
   }

   public void onDeactivate(Operation operation)
   {
      operation.execute();
   }
   
   public NewConnectionPreInstallOdbcHost()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);

      dirChooser_ = new DirectoryChooserTextBox("", "", ElementIds.TextBoxButtonId.ODBC_PATH, null);
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
 
      initWidget(createWidget());
   }
   
   private Widget createWidget()
   {
      return mainWidget_;
   }

   public void initializeInfo(NewConnectionInfo info)
   {
      dirChooser_.setText(info.getOdbcInstallPath());
      license_.setText(info.getOdbcLicense());
      driverLabel_.setText("The " + info.getName() + " driver is currently not installed. ");

      if (!StringUtil.isNullOrEmpty(info.getOdbcWarning()))
      {
         SafeHtml safeMsg = DialogHtmlSanitizer.sanitizeHtml(info.getOdbcWarning());
         HTMLPanel formattedPanel = new HTMLPanel(safeMsg);
         warningLabel_.add(formattedPanel);
      }
      else
      {
         warningPanel_.setVisible(false);
      }
   }

   public ConnectionOptions collectInput()
   {
      options_.setIntermediateInstallPath(dirChooser_.getText());
      return options_;
   }

   public void setIntermediateResult(ConnectionOptions result) 
   {
      options_ = result;
   }
   
   public interface Styles extends CssResource
   {
   }

   @SuppressWarnings("unused")
   private ConnectionsServerOperations server_;

   private Widget mainWidget_;

   @UiField
   TextArea license_;

   @UiField
   Label driverLabel_;

   @UiField(provided = true)
   DirectoryChooserTextBox dirChooser_;

   @UiField
   HTMLPanel warningLabel_;

   @UiField
   HTMLPanel warningPanel_;

   @SuppressWarnings("unused")
   private OperationWithInput<Boolean> nextPageEnabledOperation_;

   private ConnectionOptions options_;
}
