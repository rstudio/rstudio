/*
 * PackagesPane.java
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
package org.rstudio.studio.client.workbench.views.packages;

import java.util.ArrayList;
import java.util.List;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.cellview.AriaLabeledCheckboxCell;
import org.rstudio.core.client.cellview.ImageButtonColumn;
import org.rstudio.core.client.cellview.ImageButtonColumn.TitleProvider;
import org.rstudio.core.client.cellview.LabeledBoolean;
import org.rstudio.core.client.cellview.LinkColumn;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.RStudioDataGrid;
import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SuperDevMode;
import org.rstudio.studio.client.packrat.model.PackratContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.projects.ProjectContext;
import org.rstudio.studio.client.workbench.projects.RenvContext;
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
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.DefaultCellTableBuilder;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.inject.Inject;

public class PackagesPane extends WorkbenchPane implements Packages.Display
{
   @Inject
   public PackagesPane(Commands commands, 
                       Session session,
                       GlobalDisplay display,
                       EventBus events)
   {
      super("Packages", events);
      commands_ = commands;
      session_ = session;
      display_ = display;
      
      dataGridRes_ = (PackagesDataGridResources) 
            GWT.create(PackagesDataGridResources.class);
      ensureWidget();
   }
   
   @Override
   public void setObserver(PackagesDisplayObserver observer)
   {
      observer_ = observer;
   }
   
   @Override
   public void setPackageState(ProjectContext projectContext, 
                               List<PackageInfo> packages)
   {
      projectContext_ = projectContext;
      packagesDataProvider_.setList(packages);
      createPackagesTable();

      // manage visibility of Packrat / renv menu buttons
      PackratContext packratContext = projectContext_.getPackratContext();
      RenvContext renvContext = projectContext_.getRenvContext();
      
      packratMenuButton_.setVisible(false);
      renvMenuButton_.setVisible(false);
      if (packratContext.isModeOn())
         packratMenuButton_.setVisible(true);
      else if (renvContext.active)
         renvMenuButton_.setVisible(true);
      
      // always show the separator before the packrat commands
      projectButtonSeparator_.setVisible(true);
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
      int row = packageRow(status.getName(), status.getLib());
      
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
         if (packages.get(i).getName() == status.getName() &&
             i != row)
         {
            packages.set(i, packages.get(i).asUnloaded());
         }
      }
   }
   
   private int packageRow(String packageName, String packageLib)
   {
      // if we haven't retrieved packages yet then return not found
      if (packagesDataProvider_ == null)
         return -1;
      
      List<PackageInfo> packages = packagesDataProvider_.getList();
      
      // figure out which row of the table includes this package
      int row = -1;
      for (int i=0; i<packages.size(); i++)
      {
         PackageInfo packageInfo = packages.get(i);
         if (packageInfo.getName() == packageName &&
             packageInfo.getLibrary() == packageLib)
         {
            row = i;
            break;
         }
      }
      return row;
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar("Packages Tab");
     
      // install packages
      toolbar.addLeftWidget(commands_.installPackage().createToolbarButton());
      toolbar.addLeftSeparator();
      
      // update packages
      toolbar.addLeftWidget(commands_.updatePackages().createToolbarButton());
      projectButtonSeparator_ = toolbar.addLeftSeparator();
      
      // packrat (all packrat UI starts out hidden and then appears
      // in response to changes in the packages state)

      // create packrat menu + button
      ToolbarPopupMenu packratMenu = new ToolbarPopupMenu();
      packratMenu.addItem(commands_.packratHelp().createMenuItem(false));
      packratMenu.addSeparator();
      packratMenu.addItem(commands_.packratCheckStatus().createMenuItem(false));
      packratMenu.addItem(commands_.packratClean().createMenuItem(false));
      packratMenu.addItem(commands_.packratBundle().createMenuItem(false));
      packratMenu.addSeparator();
      packratMenu.addItem(commands_.packratOptions().createMenuItem(false));
      packratMenuButton_ = new ToolbarMenuButton(
            "Packrat", 
            ToolbarButton.NoTitle,
            commands_.packratBootstrap().getImageResource(),
            packratMenu
       );
      toolbar.addLeftWidget(packratMenuButton_);
      packratMenuButton_.setVisible(false);
      
      // create renv menu + button
      ToolbarPopupMenu renvMenu = new ToolbarPopupMenu();
      renvMenu.addItem(commands_.renvHelp().createMenuItem(false));
      renvMenu.addSeparator();
      renvMenu.addItem(commands_.renvSnapshot().createMenuItem(false));
      renvMenu.addItem(commands_.renvRestore().createMenuItem(false));
      
      renvMenuButton_ = new ToolbarMenuButton(
            "renv",
            ToolbarButton.NoTitle,
            commands_.packratBootstrap().getImageResource(), // TODO
            renvMenu);
      toolbar.addLeftWidget(renvMenuButton_);
      renvMenuButton_.setVisible(false);
            
      searchWidget_ = new SearchWidget("Filter by package name", new SuggestOracle() {
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
      
      ElementIds.assignElementId(searchWidget_, ElementIds.SW_PACKAGES);
      toolbar.addRightWidget(searchWidget_);
      
      toolbar.addRightSeparator();

      ToolbarButton refreshButton = commands_.refreshPackages().createToolbarButton();
      refreshButton.addStyleName(ThemeStyles.INSTANCE.refreshToolbarButton());
      toolbar.addRightWidget(refreshButton);
      
      return toolbar;
   }
   
   private class VersionCell extends AbstractCell<PackageInfo>
   {
      public VersionCell (boolean packratVersion)
      {
         packratVersion_ = packratVersion;
      }

      @Override
      public void render(Context context, PackageInfo value, SafeHtmlBuilder sb)
      {
         sb.appendHtmlConstant("<div title=\"");
         if (!packratVersion_)
            sb.appendEscaped(value.getLibrary());
         sb.appendHtmlConstant("\"");
         sb.appendHtmlConstant(">");
         if (packratVersion_)
         {
            sb.appendEscaped(value.getPackratVersion());
         }
         else
         {
            sb.appendEscaped(value.getVersion());
         }
         sb.appendHtmlConstant("</div>"); 
      }
      
      private boolean packratVersion_;
   }
   
   @Override
   protected Widget createMainWidget()
   {
      packagesDataProvider_ = new ListDataProvider<PackageInfo>();
      packagesTableContainer_ = new LayoutPanel();
      packagesTableContainer_.addStyleName("ace_editor_theme");
      return packagesTableContainer_;
   }
   
   
   @Override
   public void onResize()
   {
      super.onResize();
      
      if (packagesTable_ != null)
         packagesTable_.onResize();
   }
   
   @Override
   public void onSelected()
   {
      super.onSelected();
      
      // If the packages table is created while the tab isn't visible, it will
      // have a cached height of 0. Refresh this height when the tab is 
      // selected.
      if (packagesTable_ != null)
         packagesTable_.onResize();
   }
   
   private void createPackagesTable()
   {
      try
      {
         packagesTableContainer_.clear();
         packagesTable_ = new RStudioDataGrid<PackageInfo>(
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
   
   private boolean showButtonImpl(PackageInfo object)
   {
      if (object.getPackageSource() == null)
         return false;
      
      PackageInfo.Source source = PackageInfo.Source.valueOf(object.getPackageSource());
      return source != PackageInfo.Source.Base;
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
         new Column<PackageInfo, PackageInfo>(new VersionCell(false)) {

            @Override
            public PackageInfo getValue(PackageInfo object)
            {
               return object;
            }
      };

      ImageButtonColumn<PackageInfo> browseColumn = new ImageButtonColumn<PackageInfo>(
            new ImageResource2x(ThemeResources.INSTANCE.browsePackage2x()),
            new OperationWithInput<PackageInfo>() {
               @Override
               public void execute(PackageInfo packageInfo)
               {
                  RStudioGinjector.INSTANCE.getGlobalDisplay().openWindow(packageInfo.getBrowseUrl());
               }
            },
            new TitleProvider<PackageInfo>()
            {
               @Override
               public String get(PackageInfo object)
               {
                  if (object.getPackageSource() == null)
                     return "Browse package on CRAN";
                  
                  PackageInfo.Source source = PackageInfo.Source.valueOf(object.getPackageSource());
                  switch (source)
                  {
                  case Base         : return "";
                  case Bioconductor : return "Browse package on Bioconductor";
                  case CRAN         : return "Browse package on CRAN";
                  case Custom       : return "Browse package [" + object.getBrowseUrl() + "]";
                  case GitHub       : return "Browse package on GitHub";
                  case Unknown      : return "Browse package on CRAN";
                  default           : return "Browse package on CRAN";
                  }
               }
            })
      {
         @Override
         public boolean showButton(PackageInfo object)
         {
            return showButtonImpl(object);
         }
      };
      
      ImageButtonColumn<PackageInfo> removeColumn = new ImageButtonColumn<PackageInfo>(
            new ImageResource2x(ThemeResources.INSTANCE.removePackage2x()),
            new OperationWithInput<PackageInfo>() {
               @Override
               public void execute(PackageInfo packageInfo)
               {
                  observer_.removePackage(packageInfo);
               }
            },
            "Remove package")
      {
         @Override
         public boolean showButton(PackageInfo object)
         {
            return showButtonImpl(object);
         }
      };

      // add common columns
      packagesTable_.addColumn(loadedColumn, new TextHeader(""));
      packagesTable_.addColumn(nameColumn, new TextHeader("Name"));
      packagesTable_.addColumn(descColumn, new TextHeader("Description"));
      packagesTable_.addColumn(versionColumn, new TextHeader("Version"));
      packagesTable_.setColumnWidth(loadedColumn, 30, Unit.PX);

      // add columns when using project-local library
      if (projectContext_.isActive())
      {
         Column<PackageInfo, PackageInfo> lockfileVersionColumn = 
            new Column<PackageInfo, PackageInfo>(new VersionCell(true)) {

               @Override
               public PackageInfo getValue(PackageInfo object)
               {
                  return object;
               }
         };
      
         TextColumn<PackageInfo> packageSourceColumn = 
               new TextColumn<PackageInfo>() {
                  @Override
                  public String getValue(PackageInfo pkgInfo)
                  {
                     if (pkgInfo.isInProjectLibrary())
                     {
                        String source = pkgInfo.getPackratSource();
                        if (source == "github")
                           return "GitHub";
                        else if (source == "Bioconductor")
                           return "BioC";
                        else if (source == "source")
                           return "Source";
                        else
                           return source;
                     }
                     else
                        return "";
                  }
         };

         packagesTable_.addColumn(lockfileVersionColumn, new TextHeader("Lockfile"));
         packagesTable_.addColumn(packageSourceColumn, new TextHeader("Source"));

         // distribute columns for extended package information
         packagesTable_.setColumnWidth(nameColumn, 20, Unit.PCT);
         packagesTable_.setColumnWidth(descColumn, 40, Unit.PCT);
         packagesTable_.setColumnWidth(versionColumn, 15, Unit.PCT);
         packagesTable_.setColumnWidth(lockfileVersionColumn, 15, Unit.PCT);
         packagesTable_.setColumnWidth(packageSourceColumn, 10, Unit.PCT);
      }
      else
      {
         // distribute columns for non-extended package information
         packagesTable_.setColumnWidth(nameColumn, 25, Unit.PCT);
         packagesTable_.setColumnWidth(descColumn, 60, Unit.PCT);
         packagesTable_.setColumnWidth(versionColumn, 15, Unit.PCT);
      }
     
      // browse column is common
      packagesTable_.addColumn(browseColumn, new TextHeader(""));
      packagesTable_.setColumnWidth(browseColumn, 20, Unit.PX);

      // remove column is common (note that we allocate extra column
      // width to provide space for a scrollbar if needed)
      int scrollWidth = DomUtils.getScrollbarWidth();
      if (scrollWidth > 0)
         scrollWidth += 3;
      
      packagesTable_.addColumn(removeColumn, new TextHeader(""));
      packagesTable_.setColumnWidth(removeColumn, 20 + scrollWidth, Unit.PX);

      packagesTable_.setTableBuilder(new 
            PackageTableBuilder(packagesTable_));
      packagesTable_.setSkipRowHoverCheck(true);
      
      packagesTableContainer_.add(packagesTable_);
      layoutPackagesTable();
      
      // unbind old table from data provider in case we've re-generated the pane
      for (HasData<PackageInfo> display : packagesDataProvider_.getDataDisplays())
         packagesDataProvider_.removeDataDisplay(display);
      
      packagesDataProvider_.addDataDisplay(packagesTable_);
   }

   private void layoutPackagesTable()
   {
      layoutPackagesTable(0);
   }
   
   private void layoutPackagesTable(double top)
   {
      packagesTableContainer_.setWidgetLeftRight(
            packagesTable_, 0, Unit.PX, 0, Unit.PX);
      packagesTableContainer_.setWidgetTopBottom(
            packagesTable_, top, Unit.PX, 0, Unit.PX);
   }
   
   class LoadedColumn extends Column<PackageInfo, LabeledBoolean>
   {
      public LoadedColumn()
      {
         super(new AriaLabeledCheckboxCell(false, false));
         
         setFieldUpdater(new FieldUpdater<PackageInfo, LabeledBoolean>() {
            @Override
            public void update(int index, PackageInfo packageInfo, LabeledBoolean value)
            {
               if (packageInfo.getLibrary() == null ||
                   packageInfo.getLibrary().length() == 0)
               {
                  display_.showMessage(GlobalDisplay.MSG_INFO, 
                        "Package Not Loaded",
                        "The package '" + packageInfo.getName() + "' cannot " +
                        "be loaded because it is not installed. Install the " +
                        "package to make it available for loading.");
               }
               else
               {
                  if (value.getBool())
                     observer_.loadPackage(packageInfo);
                  else
                     observer_.unloadPackage(packageInfo);
               }
            }    
         });
      }
      
      @Override
      public LabeledBoolean getValue(PackageInfo packageInfo)
      {
         return new LabeledBoolean(packageInfo.getName(), packageInfo.isLoaded());
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
                     if (packageInfo.getHelpUrl() == null || 
                         packageInfo.getHelpUrl().length() == 0)
                     {
                        display_.showMessage(GlobalDisplay.MSG_INFO, 
                              "Help Not Available", 
                              "The package '" + packageInfo.getName() + "' " + 
                              "is not installed. Install the package to make " +
                              "its help content available.");
                     }
                     else
                     {
                        observer_.showHelp(packageInfo);
                     }
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
   
   class PackageTableBuilder extends DefaultCellTableBuilder<PackageInfo>
   {
      public PackageTableBuilder(AbstractCellTable<PackageInfo> cellTable)
      {
         super(cellTable);
      }

      @Override
      public void buildRowImpl(PackageInfo pkg, int idx)
      {
         String library = pkg.isInProjectLibrary() ? 
               pkg.getSourceLibrary() : pkg.getLibrary();
         if (pkg.isFirstInLibrary())
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
         }
         super.buildRowImpl(pkg, idx);
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
   
   private DataGrid<PackageInfo> packagesTable_;
   private ListDataProvider<PackageInfo> packagesDataProvider_;
   private SearchWidget searchWidget_;
   private PackagesDisplayObserver observer_;
   
   private ToolbarMenuButton packratMenuButton_;
   private ToolbarMenuButton renvMenuButton_;
   private Widget projectButtonSeparator_;
   
   private LayoutPanel packagesTableContainer_;
   private int gridRenderRetryCount_;
   private ProjectContext projectContext_;

   private final Commands commands_;
   private final Session session_;
   private final GlobalDisplay display_;
   private final PackagesDataGridResources dataGridRes_;
}
