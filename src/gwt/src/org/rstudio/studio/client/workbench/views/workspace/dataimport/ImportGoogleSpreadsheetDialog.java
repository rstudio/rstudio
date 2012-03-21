/*
 * ImportGoogleSpreadsheetDialog.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.workspace.dataimport;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Invalidation.Token;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.core.client.widget.SimplePanelWithProgress;
import org.rstudio.core.client.widget.events.SelectionChangedEvent;
import org.rstudio.core.client.widget.events.SelectionChangedHandler;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.workspace.model.GoogleSpreadsheetImportSpec;
import org.rstudio.studio.client.workbench.views.workspace.model.GoogleSpreadsheetInfo;
import org.rstudio.studio.client.workbench.views.workspace.model.WorkspaceServerOperations;

import java.util.ArrayList;

public class ImportGoogleSpreadsheetDialog extends ModalDialog<GoogleSpreadsheetImportSpec>
{
   interface Resources extends ClientBundle
   {
      @Source("ImportGoogleSpreadsheetDialog.css")
      Styles styles();

      ImageResource spreadsheet();
   }

   interface Styles extends CssResource
   {
      String progressPanel();
      String scrollPanel();
      String table();
      String selected();
      String date();
      String googleSpreadsheetSearch();
      String overflowWarning();

      String varNameLabel();
      String varName();
      String spreadsheetIcon();
      String iconCell();
   }

   public ImportGoogleSpreadsheetDialog(
            WorkspaceServerOperations server,
            GlobalDisplay globalDisplay,
            ProgressOperationWithInput<GoogleSpreadsheetImportSpec> operation)
   {
      super("Import Google Spreadsheet", operation);
      server_ = server;
      globalDisplay_ = globalDisplay;
      
           
      setOkButtonCaption("Import");
   }

   @Override
   protected GoogleSpreadsheetImportSpec collectInput()
   {
      ArrayList<String> resourceIds = table_.getSelectedValues();

      if (resourceIds.size() == 1)
      {
         // strip whitespace from spreadsheet name
         String objectName = varname_.getText().trim();
         
         return GoogleSpreadsheetImportSpec.create(
               resourceIds.get(0),
               objectName);
      }
      else
      {
         return null;
      }
   }

   private String objectNameFromTitle(String title)
   {
      return title.replace(" ", ".");
   }

   @Override
   protected boolean validate(GoogleSpreadsheetImportSpec input)
   {
      if (input == null)
      {
         globalDisplay_.showErrorMessage(
                              "No Item Selected",
                               "You must select a spreadsheet to import.");
         return false;
      }

      if (input.getObjectName().length() == 0)
      {
         varname_.setFocus(true);
         globalDisplay_.showErrorMessage("Variable Name Is Required",
                                         "Please provide a variable name.");
         return false;
      }

      return true;
   }

   @Override
   protected Widget createMainWidget()
   {
      Resources res = GWT.create(Resources.class);
      Styles styles = res.styles();

      // initialize list box
      table_ = new GoogleSpreadsheetTable(styles.selected(),
                                          styles.date(),
                                          styles.spreadsheetIcon(),
                                          styles.iconCell());
      table_.setStyleName(styles.table());
      table_.setWidth("100%");
      table_.addSelectionChangedHandler(new SelectionChangedHandler()
      {
         public void onSelectionChanged(SelectionChangedEvent e)
         {
            maybeUpdateVarName();
         }
      });
      
      // disable controls until we get the list
      enableControls(false);

      // return list box

      scrollPanel_ = new ScrollPanel(table_);
      scrollPanel_.setStyleName(styles.scrollPanel());
      scrollPanel_.setSize("100%", "100%");

      progressPanel_ = new SimplePanelWithProgress();
      progressPanel_.setStyleName(styles.progressPanel());
      progressPanel_.showProgress(1);
      progressPanel_.setSize("500px", "300px");

      final SearchWidget search = new SearchWidget(new SuggestOracle()
      {
         @Override
         public void requestSuggestions(Request request, Callback callback)
         {
            callback.onSuggestionsReady(
                  request,
                  new Response(new ArrayList<Suggestion>()));
         }
      });
      search.addStyleName(styles.googleSpreadsheetSearch());
      search.addValueChangeHandler(new ValueChangeHandler<String>()
      {
         public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent)
         {
            progressPanel_.showProgress(1);

            searchInvalidator_.invalidate();
            final Token token = searchInvalidator_.getInvalidationToken();
            new Timer() {
               @Override
               public void run()
               {
                  if (token.isInvalid())
                     return;
                  listSpreadsheets(search.getText(), token);
               }
            }.schedule(500);
         }
      });

      VerticalPanel vpanel = new VerticalPanel();
      vpanel.add(search);
      vpanel.setCellHorizontalAlignment(search, VerticalPanel.ALIGN_RIGHT);
      vpanel.add(progressPanel_);
      overflow_ = new Label("Showing the first " + MAX_SHOWN + " spreadsheets");
      overflow_.setStyleName(styles.overflowWarning());
      showOverflow(false);
      vpanel.add(overflow_);

      FlowPanel varNamePanel = new FlowPanel();
      Label varNameLabel = new Label("Name:");
      varNameLabel.setStyleName(styles.varNameLabel());
      varNamePanel.add(varNameLabel);
      varname_ = new TextBox();
      varname_.setStyleName(styles.varName());
      varNamePanel.add(varname_);
      addLeftWidget(varNamePanel);

      listSpreadsheets(null, null);

      return vpanel;
   }

   private void maybeUpdateVarName()
   {
      if (varname_.getText().equals(varNameDefault_))
      {
         ArrayList<String> list = table_.getSelectedValues2();
         String title = list.size() == 0 ? "" : list.get(0);
         String name = objectNameFromTitle(title);
         varname_.setText(name);
         varNameDefault_ = name;
      }
   }

   private void listSpreadsheets(String searchTerm,
                                 final Token invalidationToken)
   {
      showOverflow(false);

      // run query
      server_.listGoogleSpreadsheets(
         searchTerm, 
         MAX_SHOWN + 1,
         new ServerRequestCallback<JsArray<GoogleSpreadsheetInfo>>() {

            @Override
            public void onResponseReceived(
                              JsArray<GoogleSpreadsheetInfo> spreadsheets)
            {
               if (invalidationToken != null && invalidationToken.isInvalid())
                  return;

               progressPanel_.setWidget(scrollPanel_);

               // populate list box
               table_.clear();
               ArrayList<GoogleSpreadsheetInfo> list =
                     new ArrayList<GoogleSpreadsheetInfo>();
               for (int i=0; i<Math.min(MAX_SHOWN, spreadsheets.length()); i++)
               {
                  GoogleSpreadsheetInfo ssInfo = spreadsheets.get(i);
                  list.add(ssInfo);
               }
               table_.addItems(list, false);

               showOverflow(spreadsheets.length() > MAX_SHOWN);

               enableControls(true);
            }

            @Override
            public void onError(ServerError error)
            {
               if (invalidationToken != null && invalidationToken.isInvalid())
                  return;

               progressPanel_.setWidget(null);

               // errors are silently ignored -- this is the preferred
               // approach until we upgrade the protocol to return
               // structured OAuth responses which include authentication
               // challenges

               closeDialog();
            }

         });
   }

   private void showOverflow(boolean show)
   {
      if (!show)
         overflow_.getElement().getStyle().setProperty("textIndent", "-1000px");
      else
         overflow_.getElement().getStyle().setProperty("textIndent", "0");
   }

   public static void ensureStylesInjected()
   {
      Resources res = GWT.create(Resources.class);
      res.styles().ensureInjected();
   }
   
   private SimplePanelWithProgress progressPanel_;
   private final WorkspaceServerOperations server_;
   private final GlobalDisplay globalDisplay_;
   private GoogleSpreadsheetTable table_;
   private final Invalidation searchInvalidator_ = new Invalidation();

   private ScrollPanel scrollPanel_;
   private static final int MAX_SHOWN = 100;
   private Label overflow_;
   private TextBox varname_;
   private String varNameDefault_ = "";
}
