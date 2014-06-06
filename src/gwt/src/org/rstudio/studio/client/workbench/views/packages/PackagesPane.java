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
import org.rstudio.studio.client.packrat.model.PackratContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInfo;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallContext;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallOptions;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallRequest;
import org.rstudio.studio.client.workbench.views.packages.model.PackageLibraryUtils;
import org.rstudio.studio.client.workbench.views.packages.model.PackageLibraryUtils.PackageLibraryType;
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
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.inject.Inject;

public class PackagesPane extends WorkbenchPane implements Packages.Display
{
   @Inject
   public PackagesPane(Commands commands, 
                       Session session)
   {
      super("Packages");
      commands_ = commands;
      session_ = session;
      dataGridRes_ = (PackagesDataGridResources) 
            GWT.create(PackagesDataGridResources.class);
      ensureWidget();
   }
   
   @Override
   public void setObserver(PackagesDisplayObserver observer)
   {
      observer_ = observer ;  
   }
   
   @Override
   public void setPackageState(PackratContext packratContext, 
                               List<PackageInfo> packages)
   {
      packratContext_ = packratContext;
      packagesDataProvider_.setList(packages);
      createPackagesTable();

      // show the bootstrap button if this state is eligible for Packrat but the
      // project isn't currently under Packrat control
      packratBootstrapButton_.setVisible(
         packratContext_.isApplicable() && 
         !packratContext_.isPackified());
      
      // show the toolbar button if Packrat mode is on
      packratMenuButton_.setVisible(packratContext_.isModeOn());
      
      // show the separator if either of the above are visible
      packratSeparator_.setVisible(packratBootstrapButton_.isVisible() ||
                                   packratMenuButton_.isVisible());
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
      toolbar.addLeftSeparator();
      
      // packrat (all packrat UI starts out hidden and then appears
      // in response to changes in the packages state)

      // create packrat bootstrap button
      packratBootstrapButton_ = commands_.packratBootstrap().createToolbarButton(false);
      toolbar.addLeftWidget(packratBootstrapButton_);
      packratBootstrapButton_.setVisible(false);
      
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
      packratMenuButton_.setVisible(false);
      packratSeparator_ = toolbar.addLeftSeparator();
      packratSeparator_.setVisible(false);
          
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
      return packagesTableContainer_;
   }
   
   private void createPackagesTable()
   {
      try
      {
         packagesTableContainer_.clear();
         packagesTable_ = new DataGrid<PackageInfo>(
            packagesDataProvider_.getList().size(), dataGridRes_);
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
    
      Column<PackageInfo, PackageInfo> descColumn = 
         new Column<PackageInfo, PackageInfo>(new DescriptionCell()) {

            @Override
            public PackageInfo getValue(PackageInfo object)
            {
               return object;
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

      // add common columns
      packagesTable_.addColumn(loadedColumn, new TextHeader(""));
      packagesTable_.addColumn(nameColumn, new TextHeader("Name"));
      packagesTable_.addColumn(descColumn, new TextHeader("Description"));
      packagesTable_.addColumn(versionColumn, new TextHeader("Version"));
      packagesTable_.setColumnWidth(loadedColumn, 30, Unit.PX);

      // set up Packrat-specific columns
      if (packratContext_ != null &&
          packratContext_.isModeOn())
      {
         TextColumn<PackageInfo> packratVersionColumn = 
               new TextColumn<PackageInfo>() {
                  @Override
                  public String getValue(PackageInfo pkgInfo)
                  {
                     if (pkgInfo.getInPackratLibary())
                        return pkgInfo.getPackratVersion();
                     else
                        return "";
                  }
         };
         TextColumn<PackageInfo> packratSourceColumn = 
               new TextColumn<PackageInfo>() {
                  @Override
                  public String getValue(PackageInfo pkgInfo)
                  {
                     if (pkgInfo.getInPackratLibary())
                     {
                        String source = pkgInfo.getPackratSource();
                        if (source.equals("github"))
                           return "GitHub";
                        else if (source.equals("Bioconductor"))
                           return "BioC";
                        else
                           return source;
                     }
                     else
                        return "";
                  }
         };

         packagesTable_.addColumn(packratVersionColumn, new TextHeader("Packrat"));
         packagesTable_.addColumn(packratSourceColumn, new TextHeader("Source"));

         // distribute columns for extended package information
         packagesTable_.setColumnWidth(nameColumn, 20, Unit.PCT);
         packagesTable_.setColumnWidth(descColumn, 40, Unit.PCT);
         packagesTable_.setColumnWidth(versionColumn, 15, Unit.PCT);
         packagesTable_.setColumnWidth(packratVersionColumn, 15, Unit.PCT);
         packagesTable_.setColumnWidth(packratSourceColumn, 10, Unit.PCT);
         
         // highlight rows that are out of sync in packrat
         packagesTable_.setRowStyles(new PackageRowStyles());
      }
      else
      {
         // distribute columns for non-extended package information
         packagesTable_.setColumnWidth(nameColumn, 30, Unit.PCT);
         packagesTable_.setColumnWidth(descColumn, 55, Unit.PCT);
         packagesTable_.setColumnWidth(versionColumn, 15, Unit.PCT);
      }
     
      // remove column is common
      packagesTable_.addColumn(removeColumn, new TextHeader(""));
      packagesTable_.setColumnWidth(removeColumn, 30, Unit.PX);

      packagesTable_.setHeaderBuilder(new 
            PackageHeaderBuilder(packagesTable_, false));
      packagesTable_.setTableBuilder(new 
            PackageTableBuilder(packagesTable_));
      packagesTable_.setSkipRowHoverCheck(true);
      packagesTableContainer_.add(packagesTable_);
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
      public String getStyleNames(PackageInfo row, int rowIndex)
      {
         PackageInfo pkgInfo = row.cast();
         if (pkgInfo.getInPackratLibary() &&
             pkgInfo.getPackratVersion() != pkgInfo.getVersion())
         {
            return dataGridRes_.dataGridStyle().packageOutOfSyncRow();
         }
         return "";
      }
   }
   
   private class DescriptionCell extends AbstractCell<PackageInfo>
   {
      @Override
      public void render(Context context, PackageInfo pkgInfo, 
                         SafeHtmlBuilder sb)
      {
         if (pkgInfo.getDesc().length() > 0)
            sb.appendEscaped(pkgInfo.getDesc());
         else
         {
            sb.appendHtmlConstant("<span class=\"" + 
                    dataGridRes_.dataGridStyle().packageNotApplicableColumn() +
                    "\">Not installed</span>");
         }
      }
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
   private final PackagesDataGridResources dataGridRes_;
}
