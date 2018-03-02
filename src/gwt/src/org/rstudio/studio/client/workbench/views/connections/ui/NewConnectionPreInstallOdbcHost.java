/*
 * NewConnectionPreInstallOdbcHost.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.DelayedProgressRequestCallback;
import org.rstudio.studio.client.common.rstudioapi.DialogHtmlSanitizer;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionsServerOperations;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.VerticalPanel;
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

   private ConnectionsServerOperations server_;

   private Widget mainWidget_;

   @UiField
   TextArea license_;

   @UiField
   Label driverLabel_;

   @UiField
   DirectoryChooserTextBox dirChooser_;

   @UiField
   HTMLPanel warningLabel_;

   @UiField
   HTMLPanel warningPanel_;

   private OperationWithInput<Boolean> nextPageEnabledOperation_;

   private ConnectionOptions options_;
}
