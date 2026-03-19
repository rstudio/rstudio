/*
 * CheckForUpdatesDialog.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.packages.ui;

import java.util.ArrayList;

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerDataSource;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.packages.PackagesConstants;
import org.rstudio.studio.client.workbench.views.packages.model.PackageUpdate;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.inject.Inject;

public class CheckForUpdatesDialog extends PackageActionConfirmationDialog<PackageUpdate>
{
   public CheckForUpdatesDialog(ServerDataSource<JsArray<PackageUpdate>> updatesDS,
                                OperationWithInput<ArrayList<PackageUpdate>> checkOperation,
                                Operation cancelOperation)
   {
      super(constants_.updatePackagesCaption(), constants_.installUpdatesCaption(), Roles.getDialogRole(), updatesDS, checkOperation, cancelOperation);
      setThemeAware(true);
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      indicator_ = addProgressIndicator();
   }

   @Inject
   private void initialize(GlobalDisplay globalDisplay,
                           PackagesServerOperations server)
   {
      globalDisplay_ = globalDisplay;
      server_ = server;
   }

   @Override
   protected void showNoActionsRequired()
   {
      globalDisplay_.showMessage(
            MessageDialog.INFO, 
            constants_.checkForUpdatesCaption(),
            constants_.checkForUpdatesMessage());
   }

   @Override
   protected void addTableColumns(CellTable<PendingAction> table)
   {
      TextColumn<PendingAction> nameColumn = new TextColumn<PendingAction>() {
         public String getValue(PendingAction action)
         {
            return action.getActionInfo().getPackageName();
         } 
      };  
      table.addColumn(nameColumn, constants_.packageHeader());
      table.setColumnWidth(nameColumn, 28, Unit.PCT);

      TextColumn<PendingAction> installedColumn = new TextColumn<PendingAction>() {
         public String getValue(PendingAction action)
         {
            return action.getActionInfo().getInstalled();
         } 
      };  
      table.addColumn(installedColumn, constants_.installedHeader());
      table.setColumnWidth(installedColumn, 28, Unit.PCT);

      TextColumn<PendingAction> availableColumn = new TextColumn<PendingAction>() {
         public String getValue(PendingAction action)
         {
            return action.getActionInfo().getAvailable();
         } 
      };  
      table.addColumn(availableColumn, constants_.availableHeader());
      table.setColumnWidth(availableColumn, 28, Unit.PCT);

      Column<PendingAction, PendingAction> newsColumn =
            new Column<PendingAction, PendingAction>(
                  new AbstractCell<PendingAction>(BrowserEvents.CLICK, BrowserEvents.KEYDOWN)
                  {
                     @Override
                     public void render(Context context, PendingAction value, SafeHtmlBuilder sb)
                     {
                        if (value != null)
                        {
                           sb.appendHtmlConstant(
                              "<span title=\"" + constants_.showPackageNewsTitle() + "\" " +
                              "style=\"cursor: pointer; display: inline-block; vertical-align: middle;\">" +
                              "<svg xmlns=\"http://www.w3.org/2000/svg\" height=\"18\" viewBox=\"0 -960 960 960\" " +
                              "width=\"18\" fill=\"currentColor\">" +
                              "<path d=\"M200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h440l200 200v440" +
                              "q0 33-23.5 56.5T760-120H200Zm0-80h560v-400H600v-160H200v560Zm80-80h400v-80H280v80Zm0-320" +
                              "h200v-80H280v80Zm0 160h400v-80H280v80Zm-80-320v160-160 560-560Z\"/>" +
                              "</svg></span>");
                        }
                     }

                     @Override
                     protected void onEnterKeyDown(Context context, Element parent,
                           PendingAction value, NativeEvent event, ValueUpdater<PendingAction> updater)
                     {
                        if (updater != null)
                           updater.update(value);
                     }

                     @Override
                     public void onBrowserEvent(Context context, Element parent,
                           PendingAction value, NativeEvent event, ValueUpdater<PendingAction> updater)
                     {
                        super.onBrowserEvent(context, parent, value, event, updater);
                        if (BrowserEvents.CLICK.equals(event.getType()) && updater != null)
                           updater.update(value);
                     }
                  })
            {
               @Override
               public PendingAction getValue(PendingAction object)
               {
                  return object;
               }
            };

      newsColumn.setFieldUpdater((index, object, value) -> {
         indicator_.onProgress(constants_.openingNewsProgressMessage());
         server_.getPackageNewsUrl(
               object.getActionInfo().getPackageName(),
               object.getActionInfo().getLibPath(),
               new ServerRequestCallback<String>()
               {
                  @Override
                  public void onResponseReceived(String response)
                  {
                     indicator_.clearProgress();
                     navigateToUrl(response);
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     indicator_.clearProgress();
                     Debug.logError(error);
                  }
               });
      });

      table.addColumn(newsColumn, constants_.newsHeader());
      table.setColumnWidth(newsColumn, 16, Unit.PCT);
   }

   @Override
   protected String getActionName(PackageUpdate action)
   {
      return action.getPackageName();
   }

   private void navigateToUrl(String url)
   {
      if (url == null || url.length() == 0)
      {
         globalDisplay_.showErrorMessage(
               constants_.errorOpeningNewsCaption(),
               constants_.errorOpeningNewsMessage());
         return;
      }
      
      GlobalDisplay.NewWindowOptions options = new GlobalDisplay.NewWindowOptions();
      options.setName("_rstudio_package_news");
      globalDisplay_.openWindow(url, options);
   }
   
   private final ProgressIndicator indicator_;

   // Injected ----
   private GlobalDisplay globalDisplay_;
   private PackagesServerOperations server_;
   private static final PackagesConstants constants_ = com.google.gwt.core.client.GWT.create(PackagesConstants.class);
}
