/*
 * BuildPane.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.views.buildtools;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.CheckableMenuItem;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.compile.CompileOutput;
import org.rstudio.studio.client.common.compile.CompileOutputBufferWithHighlight;
import org.rstudio.studio.client.common.compile.CompilePanel;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarker;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.quarto.QuartoHelper;
import org.rstudio.studio.client.quarto.model.QuartoConfig;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildRenderSubTypeEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildServeSubTypeEvent;
import org.rstudio.studio.client.workbench.views.buildtools.model.BookdownFormats;
import org.rstudio.studio.client.workbench.views.buildtools.model.BuildServerOperations;

public class BuildPane extends WorkbenchPane
      implements BuildPresenter.Display
{
   static interface Binder extends CommandBinder<Commands, BuildPane>
   {
   }

   @Inject
   public BuildPane(Commands commands,
                    EventBus events,
                    Session session,
                    FileTypeRegistry fileTypeRegistry,
                    BuildServerOperations server, 
                    ProjectsServerOperations projServer)
   {
      super(constants_.buildText(), events);
      ((BuildPane.Binder) GWT.create(BuildPane.Binder.class)).bind(commands, this);

      commands_ = commands;
      session_ = session;
      fileTypeRegistry_ = fileTypeRegistry;
      server_ = server;
      compilePanel_ = new CompilePanel(new CompileOutputBufferWithHighlight());
      projServer_ = projServer;
      ensureWidget();
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar(constants_.buildTabLabel());

      SessionInfo sessionInfo =  session_.getSessionInfo();
      String type = sessionInfo.getBuildToolsType();
      boolean pkg = type == SessionInfo.BUILD_TOOLS_PACKAGE;
      boolean makefile = type == SessionInfo.BUILD_TOOLS_MAKEFILE;
      boolean website = type == SessionInfo.BUILD_TOOLS_WEBSITE;
      boolean quarto = type == SessionInfo.BUILD_TOOLS_QUARTO;

      // quarto books get special treatment (build button is a pure menu)
      QuartoConfig quartoConfig = session_.getSessionInfo().getQuartoConfig();
      if (quarto && QuartoHelper.isQuartoBookConfig(quartoConfig))
      {
         ToolbarPopupMenu bookBuildMenu = new QuartoBookBuildPopupMenu();
         buildAllButton_ = new ToolbarMenuButton(
               constants_.buildBookText(),
               constants_.buildBookText(),
               commands_.quartoRenderDocument().getImageResource(),
               bookBuildMenu);
         toolbar.addLeftWidget(buildAllButton_);
      }
      else
      {
         // always include Install button
         buildAllButton_ = commands_.buildAll().createToolbarButton();
         if (website)
         {
            if (sessionInfo.getBuildToolsBookdownWebsite())
            {
               buildAllButton_.setText(constants_.buildBookText());
            }
            else
            {
               buildAllButton_.setText(constants_.buildWebsiteText());
            }
         }
         toolbar.addLeftWidget(buildAllButton_);

         if (pkg) 
         {
            ToolbarPopupMenu installMoreMenu = new ToolbarPopupMenu();
            toolbar.addLeftWidget(new ToolbarMenuButton(ToolbarButton.NoText, "", installMoreMenu, false));
            
            projServer_.readProjectOptions(new SimpleRequestCallback<RProjectOptions>() {
               @Override
               public void onResponseReceived(RProjectOptions response) 
               {
                  RProjectConfig config = response.getConfig();
                  
                  boolean preclean = config.getPackageCleanBeforeInstall();
                  String installArgs = config.getPackageInstallArgs();

                  buildAllButton_.setTitle(
                     commands_.buildAll().getTooltip() + "\n\nR CMD INSTALL " + (preclean ? "--preclean " : " ") + installArgs + " <pkg>"
                  );
                  
                  AppCommand cmdBuildFull = commands_.buildFull();
                  cmdBuildFull.setDesc(
                     cmdBuildFull.getTooltip() + "\n\nR CMD INSTALL --preclean " + installArgs + " <pkg>"
                  );
                  buildFullMenuItem_ = cmdBuildFull.createMenuItem(false);

                  AppCommand cmdBuildIncremental = commands_.buildIncremental();
                  cmdBuildIncremental.setDesc(
                     cmdBuildIncremental.getTooltip() + "\n\nR CMD INSTALL " + installArgs + " <pkg>"
                  );
                  buildIncrementalMenuItem_ = cmdBuildIncremental.createMenuItem(false);

                  installMoreMenu.addItem(buildFullMenuItem_);
                  installMoreMenu.addItem(buildIncrementalMenuItem_);
                  
                  installMoreMenu.addSeparator();
                  installMoreMenu.addItem(commands_.buildToolsProjectSetup().createMenuItem(false));
               }
            });
         }
         
         // book build menu
         if (sessionInfo.getBuildToolsBookdownWebsite())
         {
            ToolbarPopupMenu bookBuildMenu = new BookdownBuildPopupMenu();
            ToolbarMenuButton buildMenuButton = new ToolbarMenuButton(ToolbarButton.NoText,
                  constants_.buildBookOptionsText(), bookBuildMenu, true);
            ElementIds.assignElementId(buildMenuButton, ElementIds.BUILD_BOOKDOWN_MENUBUTTON);
            toolbar.addLeftWidget(buildMenuButton);  
         }
      }
      
      // sync build all button caption
      syncBuildAllUI();
      toolbar.addLeftSeparator();
     
      if (quarto)
      {
         // quarto book gets a menu
         if (QuartoHelper.isQuartoBookConfig(quartoConfig) && quartoConfig.project_formats.length > 1)
         {
            ToolbarPopupMenu bookServeMenu = new QuartoBookServePopupMenu();
            ToolbarMenuButton menuButton =  new ToolbarMenuButton(
                  constants_.serveBookText(),
                  constants_.serveBookText(),
                  commands_.serveQuartoSite().getImageResource(), 
                  bookServeMenu
            );
            toolbar.addLeftWidget(menuButton);
            toolbar.addLeftSeparator();
         }
         // quarto website gets generic serve site
         else if (QuartoHelper.isQuartoWebsiteConfig(quartoConfig))
         {
            toolbar.addLeftWidget(commands_.serveQuartoSite().createToolbarButton());
            toolbar.addLeftSeparator();
         }
      }
      
      // packages get check package
      if (pkg)
      {
         toolbar.addLeftWidget(commands_.testPackage().createToolbarButton());
         toolbar.addLeftSeparator();

         toolbar.addLeftWidget(commands_.checkPackage().createToolbarButton());
         toolbar.addLeftSeparator();
      }

      // create more menu
      if (makefile || website || pkg)
      {
         ToolbarPopupMenu moreMenu = new ToolbarPopupMenu();

         if (website)
         {
            moreMenu.addItem(commands_.cleanAll().createMenuItem(false));
            moreMenu.addSeparator();
         }

         // packages get additional commands
         else if (pkg)
         {
            moreMenu.addItem(commands_.devtoolsLoadAll().createMenuItem(false));
            moreMenu.addItem(commands_.buildFull().createMenuItem(false));
            moreMenu.addSeparator();
            moreMenu.addItem(commands_.buildSourcePackage().createMenuItem(false));
            moreMenu.addItem(commands_.buildBinaryPackage().createMenuItem(false));
            moreMenu.addSeparator();
            moreMenu.addItem(commands_.roxygenizePackage().createMenuItem(false));
            moreMenu.addSeparator();
         }

         else if (makefile)
         {
            moreMenu.addItem(commands_.cleanAll().createMenuItem(false));
            moreMenu.addSeparator();
         }
         moreMenu.addItem(commands_.buildToolsProjectSetup().createMenuItem(false));

         // add more menu
         ToolbarMenuButton moreButton = new ToolbarMenuButton(
               constants_.moreText(),
               ToolbarButton.NoTitle,
               new ImageResource2x(StandardIcons.INSTANCE.more_actions2x()),
               moreMenu);
         ElementIds.assignElementId(moreButton, ElementIds.BUILD_MORE_MENUBUTTON);
         toolbar.addLeftWidget(moreButton);
      }

      // build clear button
      clearBuildButton_ = commands_.clearBuild().createToolbarButton();
      clearBuildButton_.addStyleName(ThemeStyles.INSTANCE.clearBuildButton());
      clearBuildButton_.setVisible(true);

      // connect compile panel
      compilePanel_.connectToolbar(toolbar);
      toolbar.addRightWidget(clearBuildButton_);

      return toolbar;
   }

   @Handler
   void onClearBuild()
   {
       compilePanel_.clearAll();
   }
   
   class BookdownBuildPopupMenu extends ToolbarPopupMenu
   {
      @Override
      public void getDynamicPopupMenu(final ToolbarPopupMenu.DynamicPopupMenuCallback callback)
      {
         clearItems();
         
         server_.getBookdownFormats(new SimpleRequestCallback<BookdownFormats>()
         {
            @Override
            public void onResponseReceived(BookdownFormats formats)
            {
               String defaultFormat = formats.getOutputFormat();
               JsArrayString allFormats = formats.getAllOututFormats();
               MenuItem allMenu = new FormatMenuItem(
                  "all", constants_.allFormatsLabel(), defaultFormat == "all");
               addItem(allMenu);
               addSeparator();
               for (int i = 0; i < allFormats.length(); i++)
               {
                  String format = allFormats.get(i);
                  addItem(new FormatMenuItem(format, defaultFormat == format));
               }
               callback.onPopupMenu(BookdownBuildPopupMenu.this);
            }
         });
      }
      
      class FormatMenuItem extends CheckableMenuItem
      {
         public FormatMenuItem(String format, boolean isChecked)
         {
            this(format, format, isChecked);
         }

         public FormatMenuItem(String format, String label, boolean isChecked)
         {
            super(label);
            format_ = format;
            label_ = label;
            isChecked_ = isChecked;
            onStateChanged();
         }

         public String getFormat()
         {
            return format_;
         }
         
         @Override
         public String getLabel()
         {
            return label_;
         }

         @Override
         public boolean isChecked()
         {
            return isChecked_;
         }
         
         @Override
         public void onInvoked()
         {
            BuildPane.this.fireEvent(new BuildRenderSubTypeEvent(format_));
         }

         protected final String format_;
         protected final String label_;
         protected final boolean isChecked_;
      }
      
   }
   

   class QuartoBookBuildPopupMenu extends ToolbarPopupMenu
   {
      public QuartoBookBuildPopupMenu()
      {
         String allFormats = AppCommand.formatMenuLabel(
               commands_.quartoRenderDocument().getImageResource(), 
               constants_.allFormatsLabel(),
               commands_.buildAll().getShortcut().toString(true)
            );
         MenuItem allMenu = new MenuItem(allFormats, true, new Command() {
            @Override
            public void execute()
            {
               BuildPane.this.fireEvent(new BuildRenderSubTypeEvent("all"));
            }  
         });
         addItem(allMenu);
         addSeparator();
         String[] formats = session_.getSessionInfo().getQuartoConfig().project_formats;
         for (int i=0; i<formats.length; i++) 
         {
            String format = formats[i];
            ImageResource img = fileTypeRegistry_.getIconForFilename("output." + format)
                  .getImageResource();
            String menuLabel = AppCommand.formatMenuLabel(
                  img, constants_.formatMenuLabel(formatName(format)), null);
            addItem(new MenuItem(menuLabel, true, new Command() {
               @Override
               public void execute()
               {
                  BuildPane.this.fireEvent(new BuildRenderSubTypeEvent(format));
               }
               
            }));
         }
      } 
   }
   
   class QuartoBookServePopupMenu extends ToolbarPopupMenu
   {
      public QuartoBookServePopupMenu()
      {
         String[] formats = session_.getSessionInfo().getQuartoConfig().project_formats;
         for (int i=0; i<formats.length; i++) 
         {
            String format = formats[i];
            if (format.startsWith("html") || format.startsWith("pdf"))
            {
               ImageResource img = fileTypeRegistry_.getIconForFilename("output." + format)
                     .getImageResource();
               String menuLabel = AppCommand.formatMenuLabel(
                     img, constants_.formatMenuLabel(formatName(format)), null);
               addItem(new MenuItem(menuLabel, true, new Command() {
                  @Override
                  public void execute()
                  {
                     BuildPane.this.fireEvent(new BuildServeSubTypeEvent(format));
                  }
               }));
            }
         }
      } 
   }
   



   @Override
   protected Widget createMainWidget()
   {
      return compilePanel_;
   }

   @Override
   public void buildStarted()
   {
      compilePanel_.compileStarted(null);
   }

   @Override
   public void showOutput(CompileOutput output, boolean scrollToBottom)
   {
      compilePanel_.showOutput(output, scrollToBottom);
   }

   @Override
   public void showErrors(String basePath,
                          JsArray<SourceMarker> errors,
                          boolean ensureVisible,
                          int autoSelect,
                          boolean openErrors,
                          String buildType)
   {
      errorsBuildType_ = buildType;
      compilePanel_.showErrors(basePath, errors, autoSelect, openErrors);

      if (ensureVisible && SourceMarker.showErrorList(errors))
         ensureVisible();
   }

   @Override
   public void buildCompleted()
   {
      compilePanel_.compileCompleted();
   }

   @Override
   public HasClickHandlers stopButton()
   {
      return compilePanel_.stopButton();
   }

   @Override
   public HasSelectionCommitHandlers<CodeNavigationTarget> errorList()
   {
      return compilePanel_.errorList();
   }
   
   @Override
   public HandlerRegistration addBuildRenderSubTypeHandler(BuildRenderSubTypeEvent.Handler handler)
   {
      return handlers_.addHandler(BuildRenderSubTypeEvent.TYPE, handler);
   }

   @Override
   public HandlerRegistration addBuildServeSubTypeHandler(BuildServeSubTypeEvent.Handler handler)
   {
      return handlers_.addHandler(BuildServeSubTypeEvent.TYPE, handler);
   }
   
   @Override
   public void fireEvent(GwtEvent<?> event)
   {
      handlers_.fireEvent(event);
   }
   
   @Override
   public String errorsBuildType()
   {
      return errorsBuildType_;
   }

   @Override
   public void scrollToBottom()
   {
      compilePanel_.scrollToBottom();
   }
   
   private void syncBuildAllUI()
   {
      SessionInfo sessionInfo =  session_.getSessionInfo();
      String type = sessionInfo.getBuildToolsType();
      if (type == SessionInfo.BUILD_TOOLS_WEBSITE)
      {
         if (sessionInfo.getBuildToolsBookdownWebsite())
         {
            buildAllButton_.setText(constants_.buildBookText());
         }
         else 
         {
            buildAllButton_.setText(constants_.buildWebsiteText());
         }
      }
      else if (type == SessionInfo.BUILD_TOOLS_QUARTO)
      {
         QuartoConfig config = sessionInfo.getQuartoConfig();
         if (config.project_type == SessionInfo.QUARTO_PROJECT_TYPE_WEBSITE)
         {
            buildAllButton_.setText(constants_.renderWebsiteText());
         }
         else if (config.project_type == SessionInfo.QUARTO_PROJECT_TYPE_BOOK)
         {
            buildAllButton_.setText(constants_.renderBookText());
         }
         else
         {
            buildAllButton_.setText(constants_.renderProjectText());
         }
      }
   }

   
   private String formatName(String format)
   {
      if (format == "html")
         return "HTML";
      else if (format == "pdf")
         return "PDF";
      else if (format == "docx")
         return "MS Word";
      else if (format == "epub")
         return "EPUB";
      else
         return format;
   }


   private ToolbarButton clearBuildButton_;
   private final Commands commands_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final Session session_;
   private final BuildServerOperations server_;
   ProjectsServerOperations projServer_;
   private String errorsBuildType_;
   private ToolbarButton buildAllButton_;
   private MenuItem buildFullMenuItem_;
   private MenuItem buildIncrementalMenuItem_;

   private final CompilePanel compilePanel_;

   private final HandlerManager handlers_ = new HandlerManager(this);

   private static final ViewBuildtoolsConstants constants_ = GWT.create(ViewBuildtoolsConstants.class);

}
