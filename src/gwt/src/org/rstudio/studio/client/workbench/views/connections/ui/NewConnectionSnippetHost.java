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
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext.NewConnectionInfo;
import org.rstudio.studio.client.workbench.views.connections.res.NewConnectionSnippetHostResources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.PushButton;
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
      newConnectionSnippetHostResources_ = GWT.create(NewConnectionSnippetHostResources.class);
      
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
      final ArrayList<NewConnectionSnippetParts> snippetParts = parseSnippet(info.getSnippet());
      int visibleRows = snippetParts.size();
      int visibleParams = snippetParts.size();
      
      // If we have a field that shares the first row, usually port:
      boolean hasSecondaryHeaderField = false;
      if (visibleParams >= 2 && snippetParts.get(0).getOrder() == snippetParts.get(1).getOrder()) {
         visibleRows --;
         hasSecondaryHeaderField = true;
      }

      visibleRows = Math.min(visibleRows, 4);

      final Grid connGrid = new Grid(visibleRows, 5);
      connGrid.addStyleName(RES.styles().grid());

      connGrid.getCellFormatter().setWidth(0, 0, "150px");
      connGrid.getCellFormatter().setWidth(0, 1, "180px");
      connGrid.getCellFormatter().setWidth(0, 2, "50px");
      connGrid.getCellFormatter().setWidth(0, 3, "54px");
      connGrid.getCellFormatter().setWidth(0, 4, "30px");

      for (int idxParams = 0, idxRow = 0; idxRow < visibleRows; idxParams++, idxRow++) {
         String key = snippetParts.get(idxParams).getKey();
         Label label = new Label(key + ":");
         label.addStyleName(RES.styles().label());
         connGrid.setWidget(idxRow, 0, label);
         connGrid.getRowFormatter().setVerticalAlign(idxRow, HasVerticalAlignment.ALIGN_TOP);
         
         String textboxStyle = RES.styles().textbox();
         
         if (idxRow == 0 && hasSecondaryHeaderField) {
            textboxStyle = RES.styles().firstTextbox();
         } else if (idxRow < visibleRows - 1) {
            connGrid.getCellFormatter().getElement(idxRow, 1).setAttribute("colspan", "4");
         } else if (idxRow == visibleRows - 1) {
            connGrid.getCellFormatter().getElement(idxRow, 1).setAttribute("colspan", "3");
            textboxStyle = RES.styles().buttonTextbox();
         }

         if (key.toLowerCase() == "password") {
            PasswordTextBox password = new PasswordTextBox();
            password.setText(snippetParts.get(idxParams).getValue());
            password.addStyleName(textboxStyle);
            connGrid.setWidget(idxRow, 1, password);
         }
         else if (key.toLowerCase() == "parameters") {
            TextArea textarea = new TextArea();
            textarea.setVisibleLines(6);
            textarea.addStyleName(RES.styles().textarea());
            connGrid.setWidget(idxRow, 1, textarea);
         }
         else {
            TextBox textbox = new TextBox();
            textbox.setText(snippetParts.get(idxParams).getValue());
            textbox.addStyleName(textboxStyle);
            connGrid.setWidget(idxRow, 1, textbox);
         }
         
         if (idxRow == 0 && hasSecondaryHeaderField) {
            idxParams++;
            
            String secondKey = snippetParts.get(idxParams).getKey();
            Label secondLabel = new Label(secondKey + ":");
            secondLabel.addStyleName(RES.styles().secondLabel());
            connGrid.setWidget(idxRow, 2, secondLabel);
            connGrid.getRowFormatter().setVerticalAlign(idxRow, HasVerticalAlignment.ALIGN_TOP);
            
            TextBox secondTextbox = new TextBox();
            secondTextbox.setText(snippetParts.get(idxParams).getValue());
            secondTextbox.addStyleName(RES.styles().secondTextbox());
            connGrid.setWidget(idxRow, 3, secondTextbox);
            connGrid.getCellFormatter().getElement(idxRow, 3).setAttribute("colspan", "2");
         }

         if (idxRow == visibleRows - 1) {
            PushButton pushButton = new PushButton(
               new Image(newConnectionSnippetHostResources_.configImage()),
               new ClickHandler()
               {
                  @Override
                  public void onClick(ClickEvent arg0)
                  {
                     new NewConnectionSnippetDialog(
                        new OperationWithInput<ArrayList<NewConnectionSnippetParts>>() {
                           @Override
                           public void execute(final ArrayList<NewConnectionSnippetParts> result)
                           {
                           }
                        },
                        snippetParts,
                        info
                     ).showModal();
                  }
               });
            
            pushButton.addStyleName(RES.styles().settingsButton());
            pushButton.setWidth("20px");
            connGrid.setWidget(idxRow, 2, pushButton);
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
      
      String firstTextbox();
      String secondLabel();
      String secondTextbox();
      String buttonTextbox();

      String settingsButton();
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
   private NewConnectionSnippetHostResources newConnectionSnippetHostResources_;
}
