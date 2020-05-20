/* PackratResolveConflictDialog.java
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

package org.rstudio.studio.client.packrat.ui;

import java.util.ArrayList;

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.RStudioDataGrid;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.StyleUtils;
import org.rstudio.studio.client.packrat.model.PackratConflictActions;
import org.rstudio.studio.client.packrat.model.PackratConflictResolution;
import org.rstudio.studio.client.workbench.views.packages.ui.PackagesDataGridCommon;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class PackratResolveConflictDialog 
                           extends ModalDialog<PackratConflictResolution>
{
   public PackratResolveConflictDialog(
               ArrayList<PackratConflictActions> conflictActions,
               OperationWithInput<PackratConflictResolution> onResolved)
   {
      super("Resolve Conflict", Roles.getDialogRole(), onResolved);
      
      setOkButtonCaption("Resolve");
         
      // main widget
      mainWidget_ = new VerticalPanel();
      
      // layout constants
      final int kTableWidth = 540;
      final int kPackageColWidth = 120;
      final int kActionCol1Width = 190;
      final int kActionCol2Width = 200;
      
      // create label
      Label label = new Label(
        "Packrat's packages are out of sync with the packages currently " +
        "installed in your library. To resolve the conflict you need to " +
        "either update Packrat to match your library or update your library " +
        "to match Packrat.");
      label.addStyleName(RESOURCES.styles().conflictLabel());
      label.setWidth(kTableWidth + "px");
      mainWidget_.add(label);
            
      // table
      table_ = new RStudioDataGrid<PackratConflictActions>(conflictActions.size(),
            (PackagesDataGridCommon)GWT.create(PackagesDataGridCommon.class));
      StyleUtils.forceMacScrollbars(table_);
      table_.addStyleName(RESOURCES.styles().conflictsTable());
      table_.setWidth(kTableWidth + "px");
      table_.setHeight("225px");
      table_.setRowData(conflictActions);
      table_.addColumn(
         new TextColumn<PackratConflictActions>() {
            public String getValue(PackratConflictActions item)
            {
               return item.getPackage();
            } 
         },
         "Package"
      );
      
      table_.addColumn(
         new TextColumn<PackratConflictActions>() {
            public String getValue(PackratConflictActions item)
            {
               return StringUtil.notNull(item.getSnapshotAction());
            } 
         },
         "Packrat"
      );
        
      table_.addColumn(
         new TextColumn<PackratConflictActions>() {
            public String getValue(PackratConflictActions item)
            {
               return StringUtil.notNull(item.getLibraryAction());
            } 
         },
         "Library"
      );
      
      table_.setColumnWidth(0, kPackageColWidth + "px");
      table_.setColumnWidth(1, kActionCol1Width + "px");
      table_.setColumnWidth(2, kActionCol2Width + "px");    
      mainWidget_.add(table_);
      
      // create radio buttons
      Grid choiceGrid = new Grid(1, 3);
      choiceGrid.setWidth(kTableWidth + "px");
      choiceGrid.getColumnFormatter().setWidth(0, (kPackageColWidth-3) + "px");
      choiceGrid.getColumnFormatter().setWidth(1, (kActionCol1Width+3) + "px");
      choiceGrid.getColumnFormatter().setWidth(2, kActionCol2Width + "px");
      choiceGrid.addStyleName(RESOURCES.styles().choicesGrid());
      Label resolutionLabel =new Label("Resolution:");
      resolutionLabel.addStyleName(RESOURCES.styles().resolutionLabel());
      choiceGrid.setWidget(0, 0, resolutionLabel);
      snapshotChoice_ = new RadioButton("snapshot", "Update Packrat (Snapshot)");
      snapshotChoice_.addStyleName(RESOURCES.styles().choiceButton());
      choiceGrid.setWidget(0, 1, snapshotChoice_);
      snapshotChoice_.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            libraryChoice_.setValue(!event.getValue(), false);
         }
      });
      libraryChoice_ = new RadioButton("library", "Update Library (Restore)");
      libraryChoice_.addStyleName(RESOURCES.styles().choiceButton());
      choiceGrid.setWidget(0, 2, libraryChoice_);
      libraryChoice_.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            snapshotChoice_.setValue(!event.getValue(), false);
         }
      });
      mainWidget_.add(choiceGrid);
      
      
   }
   
   @Override
   protected PackratConflictResolution collectInput()
   {
      if (!snapshotChoice_.getValue() && !libraryChoice_.getValue())
      {
         return null;
      }
      else if (snapshotChoice_.getValue())
      {
         return PackratConflictResolution.Snapshot;
      }
      else if (libraryChoice_.getValue())
      {
         return PackratConflictResolution.Library;
      }
      else
      {
         return null;
      }
   }

   @Override
   protected boolean validate(PackratConflictResolution input)
   {
      if (input == null)
      {
         RStudioGinjector.INSTANCE.getGlobalDisplay().showMessage(
               MessageDialog.ERROR, 
               "No Selection Made", 
               "You must choose to either update Packrat (snapshot) or " +
               "update the project's private library (restore).");
         return false;
      }
      else
      {
         return true;
      }
   }

   @Override
   protected Widget createMainWidget()
   {
      
      return mainWidget_;
   }  
   
   static interface Styles extends CssResource
   {
      String conflictLabel();
      String choicesGrid();
      String choiceButton();
      String resolutionLabel();
      String conflictsTable();
   }
  
   static interface Resources extends ClientBundle
   {
      @Source("PackratResolveConflictDialog.css")
      Styles styles();
   }
   
   static Resources RESOURCES = (Resources)GWT.create(Resources.class);
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }
   
   private VerticalPanel mainWidget_;
   private DataGrid<PackratConflictActions> table_;
   private RadioButton snapshotChoice_;
   private RadioButton libraryChoice_;
}
