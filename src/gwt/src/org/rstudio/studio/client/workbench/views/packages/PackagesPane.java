/*
 * PackagesPane.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.packages;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.cellview.ImageButtonColumn;
import org.rstudio.core.client.cellview.LinkColumn;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SuperDevMode;
import org.rstudio.studio.client.packrat.Packrat;
import org.rstudio.studio.client.packrat.model.PackratContext;
import org.rstudio.studio.client.packrat.model.PackratPackageInfo;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInfo;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallContext;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallOptions;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallRequest;
import org.rstudio.studio.client.workbench.views.packages.model.PackageLibraryUtils;
import org.rstudio.studio.client.workbench.views.packages.model.PackageStatus;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;
import org.rstudio.studio.client.workbench.views.packages.ui.InstallPackageDialog;
import org.rstudio.studio.client.workbench.views.packages.ui.PackagesCellTableResources;
import org.rstudio.studio.client.workbench.views.packages.ui.PackagesDataGridResources;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.AbstractHeaderOrFooterBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.DefaultCellTableBuilder;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.inject.Inject;

public class PackagesPane extends WorkbenchPane implements Packages.Display,
                                                           Packrat.Display
{
   @Inject
   public PackagesPane(Commands commands, 
                       Session session)
   {
      super("Packages");
      commands_ = commands;
      session_ = session;
     
      ensureWidget();
   }
   
   @Override
   public void setObserver(PackagesDisplayObserver observer)
   {
      observer_ = observer ;  
   }

   @Override
   public void setPackratContext(PackratContext context)
   {
      packratContext_ = context;
      packratMenuButton_.setVisible(context.isPackified());
      packratBootstrapButton_.setVisible(!context.isPackified());
      packratSeparator_.setVisible(context.isApplicable());
   }
   
   @Override
   public void listPackages(List<PackageInfo> packages)
   {
      packagesTable_.setPageSize(packages.size());
      packagesDataProvider_.setList(packages);
   }
   
   @Override
   public void installPackage(PackageInstallContext installContext,
                              PackageInstallOptions defaultInstallOptions,
                              PackagesServerOperations server,
                              GlobalDisplay globalDisplay,
                              OperationWithInput<PackageInstallRequest> operation)
   {
      new InstallPackageDialog(installContext,
                               defaultInstallOptions,
                               server, 
                               globalDisplay, 
                               operation).showModal();
   }
   
   @Override
   public void setPackageStatus(PackageStatus status)
   {
      int row = packageRow(status.getName(), status.getLib()) ;
      
      if (row != -1)
      {
         List<PackageInfo> packages = packagesDataProvider_.getList();
        
         packages.set(row, status.isLoaded() ? packages.get(row).asLoaded() :
                                               packages.get(row).asUnloaded());
      }
      
      // go through any duplicates to reconcile their status
      List<PackageInfo> packages = packagesDataProvider_.getList();
      for (int i=0; i<packages.size(); i++)
      {
         if (packages.get(i).getName().equals(status.getName()) &&
             i != row)
         {
            packages.set(i, packages.get(i).asUnloaded());
         }
      }
   }
   
   private int packageRow(String packageName, String packageLib)
   {
      // if we haven't retreived packages yet then return not found
      if (packagesDataProvider_ == null)
         return -1;
      
      List<PackageInfo> packages = packagesDataProvider_.getList();
      
      // figure out which row of the table includes this package
      int row = -1;
      for (int i=0; i<packages.size(); i++)
      {
         PackageInfo packageInfo = packages.get(i);
         if (packageInfo.getName().equals(packageName) &&
             packageInfo.getLibrary().equals(packageLib))
         {
            row = i ;
            break;
         }
      }
      return row ;
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
     
      // install packages
      toolbar.addLeftWidget(commands_.installPackage().createToolbarButton());
      toolbar.addLeftSeparator();
      
      // update packages
      toolbar.addLeftWidget(commands_.updatePackages().createToolbarButton());
      toolbar.addLeftSeparator();
      
      // packrat

      // create packrat bootstrap button
      packratBootstrapButton_ = commands_.packratBootstrap().createToolbarButton(false);
      toolbar.addLeftWidget(packratBootstrapButton_);
      
      // create packrat menu + button
      ToolbarPopupMenu packratMenu = new ToolbarPopupMenu();
      packratMenu.addItem(commands_.packratSnapshot().createMenuItem(false));
      packratMenu.addItem(commands_.packratRestore().createMenuItem(false));
      packratMenu.addItem(commands_.packratClean().createMenuItem(false));
      packratMenu.addSeparator();
      packratMenu.addItem(commands_.packratStatus().createMenuItem(false));
      packratMenu.addItem(commands_.packratBundle().createMenuItem(false));
      packratMenu.addSeparator();
      packratMenu.addItem(commands_.packratHelp().createMenuItem(false));
      packratMenuButton_ = new ToolbarButton(
            "Packrat", commands_.packratBootstrap().getImageResource(), 
            packratMenu
       );
      toolbar.addLeftWidget(packratMenuButton_);
      packratSeparator_ = toolbar.addLeftSeparator();
      
      setPackratContext(session_.getSessionInfo().getPackratContext());
      
      toolbar.addLeftWidget(commands_.refreshPackages().createToolbarButton());
      
      searchWidget_ = new SearchWidget(new SuggestOracle() {
         @Override
         public void requestSuggestions(Request request, Callback callback)
         {
            // no suggestions
            callback.onSuggestionsReady(
                  request,
                  new Response(new ArrayList<Suggestion>()));
         }
      });
      searchWidget_.addValueChangeHandler(new ValueChangeHandler<String>() {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            observer_.onPackageFilterChanged(event.getValue().trim());   
         }
      });
      toolbar.addRightWidget(searchWidget_);
      
      return toolbar;
   }
   
   private class VersionCell extends AbstractCell<PackageInfo>
   {
      @Override
      public void render(Context context, PackageInfo value, SafeHtmlBuilder sb)
      {
         sb.appendHtmlConstant("<div title=\"");
         sb.appendEscaped(PackageLibraryUtils.getLibraryDescription(
               session_, value.getLibrary()) + " (" +
               value.getLibrary() + ")");
         sb.appendHtmlConstant("\"");
         sb.appendHtmlConstant(" class=\"");
         sb.appendEscaped(ThemeStyles.INSTANCE.adornedText());
         sb.appendHtmlConstant("\"");
         sb.appendHtmlConstant(">");
         sb.appendEscaped(value.getVersion());
         sb.appendHtmlConstant("</div>"); 
      }
   }
   
   @Override
   protected Widget createMainWidget()
   {
      packagesDataProvider_ = new ListDataProvider<PackageInfo>();
      packagesTableContainer_ = new ResizeLayoutPanel();
      createPackagesTable();
      return packagesTableContainer_;
   }
   
   private void createPackagesTable()
   {
      try
      {
         if (packratContext_ != null && 
             packratContext_.isModeOn())
         {
            packagesTable_ = new DataGrid<PackageInfo>(-1, 
               (PackagesDataGridResources) GWT.create(
                     PackagesDataGridResources.class));
         }
         else
         {
            packagesTable_ = new CellTable<PackageInfo>(15, 
                  PackagesCellTableResources.INSTANCE);
         }
      }
      catch (Exception e)
      {
         // constructing the data grid can fail in superdevmode; so if we're
         // in superdevmode, try a few times 
         if (SuperDevMode.isActive())
         {
            if (gridRenderRetryCount_ >= 5)
            {
               Debug.log("WARNING: Failed to render packages pane data grid");
            }
            gridRenderRetryCount_++;
            Debug.log("WARNING: Retrying packages data grid render (" + 
                      gridRenderRetryCount_ + ")");
            Timer t = new Timer() {
               @Override
               public void run()
               {
                  createPackagesTable();
               }
            };
            t.schedule(5);
         }
      }
      
      if (packagesTable_ != null)
      {
         initPackagesTable();
      }
   }
   
   private void initPackagesTable()
   {
      packagesTable_.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
      packagesTable_.setSelectionModel(new NoSelectionModel<PackageInfo>());
        
      LoadedColumn loadedColumn = new LoadedColumn();
      
      NameColumn nameColumn = new NameColumn();
    
      TextColumn<PackageInfo> descColumn = new TextColumn<PackageInfo>() {
         @Override
         public String getValue(PackageInfo packageInfo)
         {
            return packageInfo.getDesc();
         } 
      };  
      
      Column<PackageInfo, PackageInfo> versionColumn = 
         new Column<PackageInfo, PackageInfo>(new VersionCell()) {

            @Override
            public PackageInfo getValue(PackageInfo object)
            {
               return object;
            }
      };
      
      ImageButtonColumn<PackageInfo> removeColumn = 
        new ImageButtonColumn<PackageInfo>(
          AbstractImagePrototype.create(ThemeResources.INSTANCE.removePackage()),
          new OperationWithInput<PackageInfo>() {
            @Override
            public void execute(PackageInfo packageInfo)
            {
               observer_.removePackage(packageInfo);          
            }  
          },
          "Remove package");

      if (packratContext_ != null &&
          packratContext_.isModeOn())
      {
         TextColumn<PackageInfo> packratVersionColumn = 
               new TextColumn<PackageInfo>() {
                  @Override
                  public String getValue(PackageInfo pkgInfo)
                  {
                     PackratPackageInfo packratInfo = pkgInfo.cast();
                     if (packratInfo.getInPackratLibary())
                        return packratInfo.getPackratVersion();
                     else
                        return "";
                  }
         };
         TextColumn<PackageInfo> packratSourceColumn = 
               new TextColumn<PackageInfo>() {
                  @Override
                  public String getValue(PackageInfo pkgInfo)
                  {
                     PackratPackageInfo packratInfo = pkgInfo.cast();
                     if (packratInfo.getInPackratLibary())
                        return packratInfo.getPackratSource();
                     else
                        return "";
                  }
         };
         packagesTable_.addColumn(loadedColumn, new TextHeader(""));
         packagesTable_.addColumn(nameColumn, new TextHeader("Name"));
         packagesTable_.addColumn(descColumn, new TextHeader("Description"));
         packagesTable_.addColumn(versionColumn, new TextHeader("Version"));
         packagesTable_.addColumn(packratVersionColumn, new TextHeader("Packrat"));
         packagesTable_.addColumn(packratSourceColumn, new TextHeader("Source"));
         packagesTable_.addColumn(removeColumn, new TextHeader(""));

         packagesTable_.setColumnWidth(loadedColumn, 30, Unit.PX);
         packagesTable_.setColumnWidth(nameColumn, 20, Unit.PCT);
         packagesTable_.setColumnWidth(descColumn, 50, Unit.PCT);
         packagesTable_.setColumnWidth(versionColumn, 10, Unit.PCT);
         packagesTable_.setColumnWidth(packratVersionColumn, 10, Unit.PCT);
         packagesTable_.setColumnWidth(packratSourceColumn, 10, Unit.PCT);
         packagesTable_.setColumnWidth(removeColumn, 30, Unit.PX);
         
         packagesTable_.setHeaderBuilder(new 
               PackageHeaderBuilder(packagesTable_, false));
         packagesTable_.setTableBuilder(new 
               PackageTableBuilder(packagesTable_));
         packagesTable_.setSkipRowHoverCheck(true);
         packagesTable_.setRowStyles(new PackageRowStyles(
               (PackagesDataGridResources) GWT.create(PackagesDataGridResources.class)));
         packagesTableContainer_.add(packagesTable_);
      }
      else
      {
         packagesTable_.addColumn(loadedColumn);
         packagesTable_.addColumn(nameColumn);
         packagesTable_.addColumn(descColumn);
         packagesTable_.addColumn(versionColumn);
         packagesTable_.addColumn(removeColumn);
         packagesTableContainer_.add(new ScrollPanel(packagesTable_));
      }
     
      packagesDataProvider_.addDataDisplay(packagesTable_);
   }
   
   class LoadedColumn extends Column<PackageInfo, Boolean>
   {
      public LoadedColumn()
      {
         super(new CheckboxCell(false, false));
         
         setFieldUpdater(new FieldUpdater<PackageInfo,Boolean>() {
            @Override
            public void update(int index, PackageInfo packageInfo, Boolean value)
            {
               if (value.booleanValue())
                  observer_.loadPackage(packageInfo.getName(),
                                        packageInfo.getLibrary()) ;
               else
                  observer_.unloadPackage(packageInfo.getName(),
                                          packageInfo.getLibrary()) ;
               
            }    
         });
      }
      
      @Override
      public Boolean getValue(PackageInfo packageInfo)
      {
         return packageInfo.isLoaded();
      }
      
   }
   
   // package name column which includes a hyperlink to package docs
   class NameColumn extends LinkColumn<PackageInfo>
   {
      public NameColumn()
      {
         super(packagesDataProvider_,
               new OperationWithInput<PackageInfo>() 
               {
                  @Override
                  public void execute(PackageInfo packageInfo)
                  {
                     observer_.showHelp(packageInfo);
                  }
               },
               false);
      }
      
      @Override
      public String getValue(PackageInfo packageInfo)
      {
         return packageInfo.getName();
      }
   }
   
   private class PackageHeaderBuilder 
           extends AbstractHeaderOrFooterBuilder<PackageInfo>
   {
      public PackageHeaderBuilder(AbstractCellTable<PackageInfo> table,
                                  boolean isFooter)
      {
         super(table, isFooter);
      }

      @Override
      protected boolean buildHeaderOrFooterImpl()
      {
         TableRowBuilder row = startRow();
         for (int i = 0; i < packagesTable_.getColumnCount(); i++)
         {
            TableCellBuilder cell = row.startTH();
            cell.className(PackagesCellTableResources.INSTANCE
                           .cellTableStyle().packageTableHeader());
            TextHeader header = (TextHeader)packagesTable_.getHeader(i);
            cell.text(header.getValue());
            cell.endTH();
         }
         row.end();
         return true;
      }
   }
   
   class PackageTableBuilder extends DefaultCellTableBuilder<PackageInfo>
   {
      public PackageTableBuilder(AbstractCellTable<PackageInfo> cellTable)
      {
         super(cellTable);
      }

      @Override
      public void buildRowImpl(PackageInfo pkg, int idx)
      {
         String library = pkg.getLibrary();
         PackageLibraryType libraryType = 
               PackageLibraryUtils.typeOfLibrary(session_, library);
         if ((idx == lastIdx_ + 1 && !lastLibrary_.equals(libraryType)) || 
             idx == 0)
         {
           TableRowBuilder row = startRow();
           TableCellBuilder cell = row.startTD();
           cell.colSpan(5).className(
                 PackagesCellTableResources.INSTANCE.cellTableStyle()
                 .libraryHeader());
           cell.title(library);
           cell.startH1().text(
                 PackageLibraryUtils.getLibraryDescription(session_, library))
                 .endH1();
           row.endTD();
           
           row.endTR();
           lastLibrary_ = libraryType;
         }
         super.buildRowImpl(pkg, idx);
         lastIdx_ = idx;
      }
      
      private PackageLibraryType lastLibrary_;
      private int lastIdx_ = 0;
   }   
   
   private class PackageRowStyles implements RowStyles<PackageInfo>
   {
      public PackageRowStyles(PackagesDataGridResources res)
      {
         res_ = res;
      }

      public String getStyleNames(PackageInfo row, int rowIndex)
      {
         PackratPackageInfo pkgInfo = row.cast();
         if (pkgInfo.getInPackratLibary() &&
             pkgInfo.getPackratVersion() != pkgInfo.getVersion())
         {
            return res_.dataGridStyle().packageOutOfSyncRow();
         }
         return "";
      }

      private final PackagesDataGridResources res_;
   }
  
   private AbstractCellTable<PackageInfo> packagesTable_;
   private ListDataProvider<PackageInfo> packagesDataProvider_;
   private SearchWidget searchWidget_;
   private PackagesDisplayObserver observer_ ;
   
   private ToolbarButton packratBootstrapButton_;
   private ToolbarButton packratMenuButton_;
   private Widget packratSeparator_;
   private ResizeLayoutPanel packagesTableContainer_;
   private int gridRenderRetryCount_;
   private PackratContext packratContext_;

   private final Commands commands_;
   private final Session session_;
}
