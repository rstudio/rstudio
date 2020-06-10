/*
 * NewConnectionSnippetHost.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.LayoutGrid;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.images.MessageDialogImages;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.DelayedProgressRequestCallback;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.connections.events.NewConnectionWizardRequestCloseEvent;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionUninstallResult;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionsServerOperations;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionInfo;
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
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class NewConnectionSnippetHost extends Composite
{
   @Inject
   private void initialize(ConnectionsServerOperations server,
                           GlobalDisplay globalDisplay,
                           EventBus eventBus)
   {
      server_ = server;
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
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
      
      int maxRows = 4;
      
      if (!StringUtil.isNullOrEmpty(info.getWarning())) {
         maxRows--;
         
         HorizontalPanel warningPanel = new HorizontalPanel();
         
         warningPanel.addStyleName(RES.styles().warningPanel());
         Image warningImage = new Image(new ImageResource2x(ThemeResources.INSTANCE.warningSmall2x()));
         warningImage.addStyleName(RES.styles().warningImage());
         warningImage.setAltText(MessageDialogImages.DIALOG_WARNING_TEXT);
         warningPanel.add(warningImage);
         
         Label label = new Label();
         label.setText(info.getWarning());
         label.addStyleName(RES.styles().warningLabel());
         warningPanel.add(label);
         warningPanel.setCellWidth(label, "100%");

         parametersPanel_.add(warningPanel);
         parametersPanel_.setCellHeight(warningPanel,"25px");
         parametersPanel_.setCellWidth(warningPanel,"100%");
      }
      
      parametersPanel_.add(createParameterizedUI(info, maxRows));

      snippetParts_ = parseSnippet(info.getSnippet());
      updateCodePanel();
      
      if (operation != null)
         operation.execute();
   }

   private ArrayList<NewConnectionSnippetParts> parseSnippet(String input) {
      ArrayList<NewConnectionSnippetParts> parts = new ArrayList<NewConnectionSnippetParts>();

      RegExp regExp = RegExp.compile(pattern_, "g");

      for (MatchResult matcher = regExp.exec(input); matcher != null; matcher = regExp.exec(input)) {
         if (matcher.getGroupCount() >= 2) {
            int order = 0;
            try {
               order = Integer.parseInt(matcher.getGroup(1));
            } catch (NumberFormatException e) {
            }

            String key = matcher.getGroup(2);
            String value = matcher.getGroupCount() >= 4 ? matcher.getGroup(4) : null;
            String connStringField = matcher.getGroupCount() >= 6 ? matcher.getGroup(6) : null;
            
            if (value != null) {
               value = value.replaceAll("\\$colon\\$", ":");
               value = value.replaceAll("\\$equal\\$", "=");
            }

            parts.add(new NewConnectionSnippetParts(order, key, value, connStringField));
         }
      }

      Collections.sort(parts, new Comparator<NewConnectionSnippetParts>()
      {
         @Override
         public int compare(NewConnectionSnippetParts p1, NewConnectionSnippetParts p2)
         {
            return p1.getOrder() - p2.getOrder();
         }
      });

      return parts;
   }

   private void showSuccess()
   {
      VerticalPanel verticalPanel = new VerticalPanel();
      verticalPanel.addStyleName(RES.styles().dialogMessagePanel());
      HTML msg = new HTML("<b>Success!</b> The given parameters " +
            "can be used to connect and disconnect correctly.");
      
      verticalPanel.add(msg);
      MessageDialog dlg = new MessageDialog(MessageDialog.INFO,
            "Test Results",
            verticalPanel
            );

      dlg.addButton("OK", ElementIds.DIALOG_OK_BUTTON, new Operation() {
         @Override
         public void execute()
         {
         }
      }, true, false);

      dlg.showModal();
   }
   
   private void showFailure(String error)
   {
      VerticalPanel verticalPanel = new VerticalPanel();
      verticalPanel.addStyleName(RES.styles().dialogMessagePanel());
      
      SafeHtmlBuilder safeHtmlBuilder = new SafeHtmlBuilder();
      safeHtmlBuilder.appendHtmlConstant("<b>Failure.</b> ");
      safeHtmlBuilder.appendEscaped(error);
      
      verticalPanel.add(new HTML(safeHtmlBuilder.toSafeHtml()));
      MessageDialog dlg = new MessageDialog(MessageDialog.ERROR,
            "Test Results",
            verticalPanel
            );

      dlg.addButton("OK", ElementIds.DIALOG_OK_BUTTON, new Operation() {
         @Override
         public void execute()
         {
         }
      }, true, false);

      dlg.showModal();
   }
   
   private LayoutGrid createParameterizedUI(final NewConnectionInfo info, int maxRows)
   {
      final ArrayList<NewConnectionSnippetParts> snippetParts = parseSnippet(info.getSnippet());
      int visibleRows = snippetParts.size();
      int visibleParams = Math.min(visibleRows, maxRows);
      
      // If we have a field that shares the first row, usually port:
      boolean hasSecondaryHeaderField = false;
      if (visibleParams >= 2 && snippetParts.get(0).getOrder() == snippetParts.get(1).getOrder()) {
         visibleRows --;
         visibleParams ++;
         hasSecondaryHeaderField = true;
      }

      boolean showAdvancedButton = visibleRows > maxRows;
      visibleRows = Math.min(visibleRows, maxRows);

      visibleParams = Math.min(visibleParams, snippetParts.size());
      final ArrayList<NewConnectionSnippetParts> secondarySnippetParts = 
            new ArrayList<NewConnectionSnippetParts>(snippetParts.subList(visibleParams, snippetParts.size()));

      final LayoutGrid connGrid = new LayoutGrid(visibleRows + 1, 4);
      connGrid.addStyleName(RES.styles().grid());

      if (visibleRows > 0) {
         connGrid.getCellFormatter().setWidth(0, 0, "150px");
         connGrid.getCellFormatter().setWidth(0, 1, "180px");
         connGrid.getCellFormatter().setWidth(0, 2, "60px");
         connGrid.getCellFormatter().setWidth(0, 3, "74px");
      }
      else {
         connGrid.getCellFormatter().setWidth(0, 0, "495px");
      }

      for (int idxParams = 0, idxRow = 0; idxRow < visibleRows; idxParams++, idxRow++) {
         connGrid.getRowFormatter().setStyleName(idxRow, RES.styles().gridRow());
         
         final String key = snippetParts.get(idxParams).getKey();
         FormLabel label = new FormLabel(key + ":");
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

         if (visibleRows == 1) {
            TextArea textarea = new TextArea();
            textarea.setVisibleLines(maxRows + 2);
            textarea.addStyleName(RES.styles().textarea());
            textarea.setText(snippetParts.get(idxParams).getValue());
            connGrid.setWidget(idxRow, 1, textarea);
            textboxBase = textarea;
         }
         else {
            TextBox textbox = new TextBox();
            textbox.setText(snippetParts.get(idxParams).getValue());
            textbox.addStyleName(textboxStyle);
            textboxBase = textbox;
         }
         label.setFor(textboxBase);
         
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

      HorizontalPanel buttonsPanel = new HorizontalPanel();
      buttonsPanel.addStyleName(RES.styles().buttonsPanel());

      final ThemedButton testButton = new ThemedButton("Test");
      testButton.addClickHandler(new ClickHandler() {
         public void onClick(ClickEvent event) {
            testButton.setEnabled(false);
            server_.connectionTest(
               codePanel_.getCode(),
               new DelayedProgressRequestCallback<String>("Testing Connection...") {
                  @Override
                  protected void onSuccess(String error)
                  {
                     testButton.setEnabled(true);
                     if (StringUtil.isNullOrEmpty(error)) {
                        showSuccess();
                     }
                     else {
                        showFailure(error);
                     }
                  }
                  
                  @Override
                  public void onError(ServerError error) {
                     testButton.setEnabled(true);
                  }
               });
         }
      });

      uninstallButton_ = makeUninstallButton();

      if (info.getHasInstaller()) {
         buttonsPanel.add(uninstallButton_);
         buttonsPanel.setCellHorizontalAlignment(uninstallButton_, HasAlignment.ALIGN_RIGHT);
         buttonsPanel.setCellWidth(uninstallButton_, "100%");
      }

      if (showAdvancedButton) {
         ThemedButton optionsButton = new ThemedButton("Options...", new ClickHandler() {
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

         buttonsPanel.add(optionsButton);
      }
      
      buttonsPanel.add(testButton);
      buttonsPanel.setCellHorizontalAlignment(testButton, HasAlignment.ALIGN_RIGHT);

      connGrid.getRowFormatter().setStyleName(visibleRows, RES.styles().lastRow());


      connGrid.getCellFormatter().getElement(visibleRows, 1).setAttribute("colspan", "3");
      connGrid.getCellFormatter().getElement(visibleRows, 2).removeFromParent();
      connGrid.getCellFormatter().getElement(visibleRows, 2).removeFromParent();
      connGrid.setWidget(visibleRows, 1, buttonsPanel);

      return connGrid;
   }

   private void updateCodePanel()
   {
      String input = info_.getSnippet();

      // replace random fields
      String previousInput = "";
      Random random = new Random();
      while (previousInput != input) {
         previousInput = input;
         input = input.replaceFirst(patternRandNumber_, Integer.toString(random.nextInt(10000)));
      }

      RegExp regExp = RegExp.compile(pattern_, "g");
      
      StringBuilder builder = new StringBuilder();     
      int inputIndex = 0;

      for (MatchResult matcher = regExp.exec(input); matcher != null; matcher = regExp.exec(input)) {
         if (matcher.getGroupCount() >= 2) {
            String key = matcher.getGroup(2);
            String value = matcher.getGroupCount() >= 4 ? matcher.getGroup(4) : null;
            String connStringField = matcher.getGroupCount() >= 6 ? matcher.getGroup(6) : null;
            
            if (value != null) {
               value = value.replaceAll("\\$colon\\$", ":");
               value = value.replaceAll("\\$equal\\$", "=");
            }

            builder.append(input.substring(inputIndex, matcher.getIndex()));
            
            if (partsKeyValues_.containsKey(key)) {
               value = partsKeyValues_.get(key);
            }
            
            if (value != null) {
               if (connStringField != null) {
                  builder.append(connStringField);
                  builder.append("=");
               }
               
               builder.append(value);
               
               if (connStringField != null) {
                  builder.append(";");
               }
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

      LayoutGrid codeGrid = new LayoutGrid(1, 1);
      codeGrid.addStyleName(RES.styles().codeGrid());
      codeGrid.setCellPadding(0);
      codeGrid.setCellSpacing(0);
      codeGrid.setWidget(0, 0, codePanel_);
      container.add(codeGrid);
     
      return container;
   }

   private ThemedButton makeUninstallButton()
   {
      // newConnectionSnippetHostResources_.trashImage()
      ThemedButton button = new ThemedButton("Uninstall...", new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent arg0)
         {  
            globalDisplay_.showYesNoMessage(
               MessageDialog.QUESTION,
               "Uninstall " + info_.getName() + " Driver", 
               "Uninstall the " + info_.getName() + " driver by removing files and registration entries?",
                  false,
                  new Operation() 
                  {
                     @Override
                     public void execute()
                     {
                        server_.uninstallOdbcDriver(
                        info_.getName(), 
                        new ServerRequestCallback<ConnectionUninstallResult>() {

                           @Override
                           public void onResponseReceived(ConnectionUninstallResult result)
                           {
                              Operation dismissOperation = new Operation()
                              {
                                 @Override
                                 public void execute()
                                 {
                                    eventBus_.fireEvent(new NewConnectionWizardRequestCloseEvent());
                                 }
                              };
                              
                              if (!StringUtil.isNullOrEmpty(result.getError())) {
                                 globalDisplay_.showErrorMessage(
                                    "Uninstallation failed",
                                    result.getError()
                                 );
                              }
                              else if (!StringUtil.isNullOrEmpty(result.getMessage())) {
                                 globalDisplay_.showMessage(
                                    MessageDialog.INFO,
                                    "Uninstallation complete",
                                    result.getMessage(),
                                    dismissOperation
                                 );
                              }
                              else
                              {
                                 globalDisplay_.showMessage(
                                    MessageDialog.INFO,
                                    "Uninstallation complete",
                                    "Driver " + info_.getName() + " was successfully uninstalled.",
                                    dismissOperation
                                 );
                              }
                           } 

                           @Override
                           public void onError(ServerError error)
                           {
                              Debug.logError(error);
                              globalDisplay_.showErrorMessage(
                                 "Uninstallation failed",
                                 error.getUserMessage());
                           }
                        });
                     }
                  },
                  new Operation() 
                  {
                     @Override
                     public void execute()
                     {
                     }
                  },
                  true);
         }
      });
      
      button.addStyleName(RES.styles().uninstallButton());

      return button;
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
      String buttonsPanel();
      
      String dialogMessagePanel();

      String uninstallButton();
      
      String warningPanel();
      String warningImage();
      String warningLabel();
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

   public void setIntermediateResult(ConnectionOptions result) 
   {
      if (result != null) {
         String intermediate = result.getIntermediateSnippet();
         if (!StringUtil.isNullOrEmpty(intermediate)) {
            info_.setSnippet(intermediate);
            initialize(null, info_);
         }
      }
   }
   
   private ConnectionCodePanel codePanel_;
   private VerticalPanel parametersPanel_;
   
   @SuppressWarnings("unused")
   private NewConnectionSnippetHostResources newConnectionSnippetHostResources_;

   NewConnectionInfo info_;
   ArrayList<NewConnectionSnippetParts> snippetParts_;
   HashMap<String, String> partsKeyValues_ = new HashMap<String, String>();

   static final String pattern_ = "\\$\\{([0-9]+):([^:=}]+)(=([^:}]*))?(:([^}]+))?\\}";
   static final String patternRandNumber_ = "\\$\\{#\\}";
   
   private ConnectionsServerOperations server_;
   private GlobalDisplay globalDisplay_;
   private ThemedButton uninstallButton_;
   private EventBus eventBus_;
}
