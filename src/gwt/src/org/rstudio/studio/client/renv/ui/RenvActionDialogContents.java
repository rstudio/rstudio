/*
 * RenvActionDialogContents.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.renv.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.theme.RStudioDataGridResources;
import org.rstudio.core.client.theme.RStudioDataGridStyle;
import org.rstudio.core.client.widget.RStudioDataGrid;
import org.rstudio.studio.client.renv.RenvConstants;
import org.rstudio.studio.client.workbench.projects.RenvAction;

import java.util.ArrayList;
import java.util.List;

public class RenvActionDialogContents extends Composite
{
   
   interface RenvRestoreDialogContentsUiBinder 
         extends UiBinder<Widget, RenvActionDialogContents> {}
   
   public RenvActionDialogContents(String action, JsArray<RenvAction> actions)
   {
      action_ = action;
      actions_ = new ArrayList<>();
      JsArrayUtil.fillList(actions, actions_);
      
      table_ = new RStudioDataGrid<>(actions.length(), RES);
      
      table_.setHeight("500px");
      table_.setWidth("600px");
      table_.setRowData(actions_);
      
      textColumn(constants_.packageColumnText(), "15%", (RenvAction entry) -> getPackageName(entry));
      textColumn(constants_.libraryVersionColumnText(), "20%", (RenvAction entry) -> getLibraryVersion(entry));
      textColumn(constants_.lockfileVersionColumnText(), "20%", (RenvAction entry) -> getLockfileVersion(entry));
      textColumn(constants_.actionVersionColumnText(),"45%", (RenvAction entry) -> getAction(entry));
      
      
      if (action == "Snapshot")
      {
         headerLabel_ = new Label(constants_.snapshotHeaderLabel());
      }
      else if (action == "Restore")
      {
         headerLabel_ = new Label(constants_.restoreHeaderLabel());
      }
      
      initWidget(uiBinder.createAndBindUi(this));
   }
   
   // Helper Functions ----
   
   private String getPackageName(RenvAction entry)
   {
      return entry.getPackageName();
   }
   
   private String getLibraryVersion(RenvAction entry)
   {
      String version = entry.getLibraryVersion();
      return StringUtil.isNullOrEmpty(version) ? constants_.libraryVersionNotInstalled() : version;
   }
   
   private String getLockfileVersion(RenvAction entry)
   {
      String version = entry.getLockfileVersion();
      return StringUtil.isNullOrEmpty(version) ? constants_.lockfileVersionNotRecorded() : version;
   }
   
   private String getAction(RenvAction entry)
   {
      return (action_ == "Snapshot") ? snapshotAction(entry) : restoreAction(entry);
   }
   
   private interface ValueGetter<T>
   {
      T getValue(RenvAction action);
   }
   
   private void textColumn(String name, String width, ValueGetter<String> getter)
   {
      TextColumn<RenvAction> column = new TextColumn<RenvAction>()
      {
         @Override
         public String getValue(RenvAction action)
         {
            return getter.getValue(action);
         }
      };
      
      table_.addColumn(column, name);
      table_.setColumnWidth(column, width);
   }
   
   private String snapshotAction(RenvAction entry)
   {
      if (entry.getAction() == "install")
      {
         return constants_.installAction(entry.getPackageName(), entry.getLibraryVersion());
      }
      else if (entry.getAction() == "remove")
      {
         return constants_.removeAction(entry.getPackageName(), entry.getLockfileVersion());
      }
      else
      {
         return constants_.updateAction(entry.getPackageName(), entry.getLockfileVersion(), entry.getLibraryVersion());
      }
   }
   
   private String restoreAction(RenvAction entry)
   {
      if (entry.getAction() == "install")
      {
         return constants_.restoreInstallAction(entry.getPackageName(), entry.getLockfileVersion()
         );
      }
      else if (entry.getAction() == "remove")
      {
         return constants_.restoreRemoveAction(entry.getPackageName(), entry.getLibraryVersion());
      }
      else
      {
         return StringUtil.format(
               "{action} '{package}' [{oldVersion} -> {newVersion}]",
               "action",     StringUtil.capitalize(entry.getAction()),
               "package",    entry.getPackageName(),
               "oldVersion", entry.getLibraryVersion(),
               "newVersion", entry.getLockfileVersion());
      }
   }
   
   // UI Fields ----
   
   @UiField(provided = true)
   DataGrid<RenvAction> table_;
   
   @UiField(provided = true)
   Label headerLabel_;
   
   // Private Members ----
   private final String action_;
   private final List<RenvAction> actions_;
   
   private static RenvRestoreDialogContentsUiBinder uiBinder =
         GWT.create(RenvRestoreDialogContentsUiBinder.class);
   
   // Resources, etc ----
   public interface Resources extends RStudioDataGridResources
   {
      @Source({RStudioDataGridStyle.RSTUDIO_DEFAULT_CSS, "RenvActionDialogContents.css"})
      Styles dataGridStyle();
   }
   
   public interface Styles extends RStudioDataGridStyle
   {
   }
   
   private static final Resources RES = GWT.create(Resources.class);
   private static final RenvConstants constants_ = GWT.create(RenvConstants.class);
   
   static { RES.dataGridStyle().ensureInjected(); }
   
}
