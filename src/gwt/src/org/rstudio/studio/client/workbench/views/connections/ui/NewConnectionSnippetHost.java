/*
 * NewConnectionSnippetHost.java
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
import java.util.Collections;
import java.util.Comparator;

import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext.NewConnectionInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class NewConnectionSnippetHost extends Composite
{
   @Inject
   private void initialize()
   {
   }

   public void onBeforeActivate(Operation operation, NewConnectionInfo info)
   {
      initialize(operation, info);
   }
   
   public void onActivate(ProgressIndicator indicator)
   {
   }

   public void onDeactivate(Operation operation)
   {
      operation.execute();
   }
   
   public NewConnectionSnippetHost()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      initWidget(createWidget());
   }
   
   private void initialize(final Operation operation, final NewConnectionInfo info)
   {
      ConnectionCodePanel superCodePanel = codePanel_;
      superCodePanel.setCode(info.getSnippet(), "");
      
      parametersPanel_.clear();
      parametersPanel_.add(createParameterizedUI(info));
      
      operation.execute();
   }

   private ArrayList<NewConnectionSnippetParts> parseSnippet(String input) {
      ArrayList<NewConnectionSnippetParts> parts = new ArrayList<NewConnectionSnippetParts>();
      String pattern = "\\$\\{([0-9]+):([^=}]+)(=([^}]+))?\\}";

      RegExp regExp = RegExp.compile(pattern, "g");

      for (MatchResult matcher = regExp.exec(input); matcher != null; matcher = regExp.exec(input)) {
         if (matcher.getGroupCount() >= 2) {
            int order = 0;
            try {
               order = Integer.parseInt(matcher.getGroup(1));
            } catch (NumberFormatException e) {
            }

            String key = matcher.getGroup(2);
            String value = matcher.getGroupCount() >= 4 ? matcher.getGroup(4) : "";

            parts.add(new NewConnectionSnippetParts(order, key, value));
         }
      }

      Collections.sort(parts, new Comparator<NewConnectionSnippetParts>()
      {
         @Override
         public int compare(NewConnectionSnippetParts p1, NewConnectionSnippetParts p2)
         {
            return p1.getOrder() == p2.getOrder() ? 0 : p1.getOrder() < p2.getOrder() ? -1 : 1;
         }
      });

      return parts;
   }
   
   private Grid createParameterizedUI(final NewConnectionInfo info)
   {
      ArrayList<NewConnectionSnippetParts> snippetParts = parseSnippet(info.getSnippet());

      final Grid connGrid = new Grid(snippetParts.size(), 2);
      connGrid.addStyleName(RES.styles().grid());


      for (int i = 0; i < snippetParts.size(); i++) {
         String key = snippetParts.get(i).getKey();
         Label label = new Label(key + ":");
         label.addStyleName(RES.styles().label());
         connGrid.setWidget(i, 0, label);
         connGrid.getRowFormatter().setVerticalAlign(i, HasVerticalAlignment.ALIGN_TOP);

         if (key.toLowerCase() == "password") {
            PasswordTextBox password = new PasswordTextBox();
            password.setText(snippetParts.get(i).getValue());
            password.addStyleName(RES.styles().textbox());
            connGrid.setWidget(i, 1, password);
         }
         else {
            TextBox textbox = new TextBox();
            textbox.setText(snippetParts.get(i).getValue());
            textbox.addStyleName(RES.styles().textbox());
            connGrid.setWidget(i, 1, textbox);
         }
      }

      return connGrid;
   }
   
   private Widget createWidget()
   {
      VerticalPanel container = new VerticalPanel();
      
      parametersPanel_ = new VerticalPanel();
      parametersPanel_.addStyleName(RES.styles().parametersPanel());
      container.add(parametersPanel_);        
      
      // add the code panel     
      codePanel_ = new ConnectionCodePanel();
      codePanel_.addStyleName(RES.styles().dialogCodePanel());

      Grid codeGrid = new Grid(1, 1);
      codeGrid.addStyleName(RES.styles().codeGrid());
      codeGrid.setCellPadding(0);
      codeGrid.setCellSpacing(0);
      codeGrid.setWidget(0, 0, codePanel_);
      container.add(codeGrid);
     
      return container;
   }

   public ConnectionOptions collectInput()
   {
      // collect the result
      ConnectionOptions result = ConnectionOptions.create(
         codePanel_.getCode(),
         codePanel_.getConnectVia());
      
      // return result
      return result;
   }
   
   public interface Styles extends CssResource
   {
      String helpLink();
      String codeGrid();
      String dialogCodePanel();
      
      String label();
      String grid();
      String textbox();
      String textarea();

      String parametersPanel();
   }

   public interface Resources extends ClientBundle
   {
      @Source("NewConnectionSnippetHost.css")
      Styles styles();
   }
   
   public static Resources RES = GWT.create(Resources.class);
   public static void ensureStylesInjected() 
   {
      RES.styles().ensureInjected();
   }
   
   private ConnectionCodePanel codePanel_;
   private VerticalPanel parametersPanel_;
}
