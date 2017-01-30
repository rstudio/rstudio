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
import java.util.HashMap;

import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext.NewConnectionInfo;
import org.rstudio.studio.client.workbench.views.connections.res.NewConnectionSnippetHostResources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;
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
      info_ = info;
      
      parametersPanel_.clear();
      parametersPanel_.add(createParameterizedUI(info));

      snippetParts_ = parseSnippet(info.getSnippet());
      updateCodePanel();
      
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
   
   private static int maxRows_ = 4;
   
   private Grid createParameterizedUI(final NewConnectionInfo info)
   {
      final ArrayList<NewConnectionSnippetParts> snippetParts = parseSnippet(info.getSnippet());
      int visibleRows = snippetParts.size();
      int visibleParams = Math.min(visibleRows, maxRows_);
      
      // If we have a field that shares the first row, usually port:
      boolean hasSecondaryHeaderField = false;
      if (visibleParams >= 2 && snippetParts.get(0).getOrder() == snippetParts.get(1).getOrder()) {
         visibleRows --;
         visibleParams ++;
         hasSecondaryHeaderField = true;
      }

      visibleRows = Math.min(visibleRows, maxRows_);

      final ArrayList<NewConnectionSnippetParts> secondarySnippetParts = 
            new ArrayList<NewConnectionSnippetParts>(snippetParts.subList(visibleParams, snippetParts.size()));

      final Grid connGrid = new Grid(visibleRows + 1, 4);
      connGrid.addStyleName(RES.styles().grid());

      connGrid.getCellFormatter().setWidth(0, 0, "150px");
      connGrid.getCellFormatter().setWidth(0, 1, "180px");
      connGrid.getCellFormatter().setWidth(0, 2, "60px");
      connGrid.getCellFormatter().setWidth(0, 3, "74px");

      for (int idxParams = 0, idxRow = 0; idxRow < visibleRows; idxParams++, idxRow++) {
         connGrid.getRowFormatter().setStyleName(idxRow, RES.styles().gridRow());
         
         final String key = snippetParts.get(idxParams).getKey();
         Label label = new Label(key + ":");
         label.addStyleName(RES.styles().label());
         connGrid.setWidget(idxRow, 0, label);
         connGrid.getRowFormatter().setVerticalAlign(idxRow, HasVerticalAlignment.ALIGN_TOP);
         
         String textboxStyle = RES.styles().textbox();

         if (idxRow == 0 && hasSecondaryHeaderField) {
            textboxStyle = RES.styles().firstTextbox();
         } else {
            connGrid.getCellFormatter().getElement(idxRow, 1).setAttribute("colspan", "4");
         }
         
         final TextBoxBase textboxBase;

         if (key.toLowerCase() == "password") {
            PasswordTextBox password = new PasswordTextBox();
            password.setText(snippetParts.get(idxParams).getValue());
            password.addStyleName(textboxStyle);
            connGrid.setWidget(idxRow, 1, password);
            textboxBase = password;
         }
         else if (key.toLowerCase() == "parameters") {
            TextArea textarea = new TextArea();
            textarea.setVisibleLines(7);
            textarea.addStyleName(RES.styles().textarea());
            connGrid.setWidget(idxRow, 1, textarea);
            textboxBase = textarea;
         }
         else {
            TextBox textbox = new TextBox();
            textbox.setText(snippetParts.get(idxParams).getValue());
            textbox.addStyleName(textboxStyle);
            textboxBase = textbox;
         }
         
         connGrid.setWidget(idxRow, 1, textboxBase);
         
         textboxBase.addChangeHandler(new ChangeHandler()
         {
            @Override
            public void onChange(ChangeEvent arg0)
            {
               partsKeyValues_.put(key, textboxBase.getValue());
               updateCodePanel();
            }
         });
         
         if (idxRow == 0 && hasSecondaryHeaderField) {
            idxParams++;
            
            final String secondKey = snippetParts.get(idxParams).getKey();
            Label secondLabel = new Label(secondKey + ":");
            secondLabel.addStyleName(RES.styles().secondLabel());
            connGrid.setWidget(idxRow, 2, secondLabel);
            connGrid.getRowFormatter().setVerticalAlign(idxRow, HasVerticalAlignment.ALIGN_TOP);
            
            final TextBox secondTextbox = new TextBox();
            secondTextbox.setText(snippetParts.get(idxParams).getValue());
            secondTextbox.addStyleName(RES.styles().secondTextbox());
            connGrid.setWidget(idxRow, 3, secondTextbox);
            connGrid.getCellFormatter().getElement(idxRow, 3).setAttribute("colspan", "2");
            
            secondTextbox.addChangeHandler(new ChangeHandler()
            {
               @Override
               public void onChange(ChangeEvent arg0)
               {
                  partsKeyValues_.put(secondKey, secondTextbox.getValue());
                  updateCodePanel();
               }
            });
         }
      }

      if (true) {
         ThemedButton pushButton = new ThemedButton("Advanced...", new ClickHandler() {
            public void onClick(ClickEvent event) {
               new NewConnectionSnippetDialog(
                  new OperationWithInput<HashMap<String, String>>() {
                     @Override
                     public void execute(final HashMap<String, String> result)
                     {
                        for(String key : result.keySet()) {
                           partsKeyValues_.put(key, result.get(key));
                        }
                        updateCodePanel();
                     }
                  },
                  secondarySnippetParts,
                  info
               ).showModal();
            }
         });

         connGrid.getRowFormatter().setStyleName(visibleRows, RES.styles().lastRow());
         connGrid.getCellFormatter().getElement(visibleRows, 1).setAttribute("colspan", "4");
         connGrid.setWidget(visibleRows, 1, pushButton);
      }

      return connGrid;
   }

   private void updateCodePanel()
   {
      String input = info_.getSnippet();
      
      String pattern = "\\$\\{([0-9]+):([^=}]+)(=([^}]+))?\\}";
      RegExp regExp = RegExp.compile(pattern, "g");
      
      StringBuilder builder = new StringBuilder();     
      int inputIndex = 0;

      for (MatchResult matcher = regExp.exec(input); matcher != null; matcher = regExp.exec(input)) {
         if (matcher.getGroupCount() >= 2) {
            String key = matcher.getGroup(2);
            String value = matcher.getGroupCount() >= 4 ? matcher.getGroup(4) : "";

            builder.append(input.substring(inputIndex, matcher.getIndex()));
            
            if (partsKeyValues_.containsKey(key)) {
               value = partsKeyValues_.get(key);
            }
            
            if (value != null) {
               builder.append(value);
            }
            
            inputIndex = matcher.getIndex() + matcher.getGroup(0).length();
         }
      }
      
      builder.append(input.substring(inputIndex, input.length()));
      
      codePanel_.setCode(builder.toString(), "");
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
      
      String grid();
      String gridRow();
      
      String label();
      String textbox();
      String textarea();

      String parametersPanel();
      
      String firstTextbox();
      String secondLabel();
      String secondTextbox();
      String buttonTextbox();

      String settingsButton();
      
      String lastRow();
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

   NewConnectionInfo info_;
   ArrayList<NewConnectionSnippetParts> snippetParts_;
   HashMap<String, String> partsKeyValues_ = new HashMap<String, String>();
}
