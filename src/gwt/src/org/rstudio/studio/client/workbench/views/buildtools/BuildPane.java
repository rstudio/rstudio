/*
 * BuildPane.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.buildtools;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
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
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarker;
import org.rstudio.studio.client.quarto.QuartoHelper;
import org.rstudio.studio.client.quarto.model.QuartoConfig;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
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
                    BuildServerOperations server)
   {
      super("Build", events);
      ((BuildPane.Binder) GWT.create(BuildPane.Binder.class)).bind(commands, this);

      commands_ = commands;
      session_ = session;
      server_ = server;
      compilePanel_ = new CompilePanel(new CompileOutputBufferWithHighlight());
      ensureWidget();
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar("Build Tab");

      SessionInfo sessionInfo =  session_.getSessionInfo();
      String type = sessionInfo.getBuildToolsType();
      boolean pkg = type == SessionInfo.BUILD_TOOLS_PACKAGE;
      boolean makefile = type == SessionInfo.BUILD_TOOLS_MAKEFILE;
      boolean website = type == SessionInfo.BUILD_TOOLS_WEBSITE;
      boolean quarto = type == SessionInfo.BUILD_TOOLS_QUARTO;

      // always include build all
      buildAllButton_ = commands_.buildAll().createToolbarButton();
      if (website)
      {
         if (sessionInfo.getBuildToolsBookdownWebsite())
         {
            buildAllButton_.setText("Build Book");
         }
         else
         {
            buildAllButton_.setText("Build Website");
         }
      }
      toolbar.addLeftWidget(buildAllButton_);


      // book build menu
      ToolbarPopupMenu bookBuildMenu = null;
      QuartoConfig quartoConfig = session_.getSessionInfo().getQuartoConfig();
      quartoBookBuildPopupMenu_ = new QuartoBookBuildPopupMenu();
      if (sessionInfo.getBuildToolsBookdownWebsite())
      {
         bookBuildMenu = new BookdownBuildPopupMenu();
      }
      else if (quartoConfig.project_type == "book")
      {
         bookBuildMenu = quartoBookBuildPopupMenu_;
      }
      if (bookBuildMenu != null)
      {
         buildMenuButton_ = new ToolbarMenuButton(ToolbarButton.NoText,
               "Build book options", bookBuildMenu, true);
         ElementIds.assignElementId(buildMenuButton_, ElementIds.BUILD_BOOKDOWN_MENUBUTTON);
         toolbar.addLeftWidget(buildMenuButton_);
      }
      
      // sync build all button caption
      syncBuildAllUI();
      

      toolbar.addLeftSeparator();
      
      // quarto gets serve site
      if (quarto && QuartoHelper.isQuartoWebsiteConfig(quartoConfig))
      {
         toolbar.addLeftWidget(commands_.serveQuartoSite().createToolbarButton());
         toolbar.addLeftSeparator();
      }

      // packages get check package
      if (pkg)
      {
         toolbar.addLeftWidget(commands_.checkPackage().createToolbarButton());
         toolbar.addLeftSeparator();
      }

      // create more menu
      if (makefile || website || pkg)
      {
         ToolbarPopupMenu moreMenu = new ToolbarPopupMenu();
         if (makefile || website)
         {
            if (makefile)
               moreMenu.addItem(commands_.rebuildAll().createMenuItem(false));
            moreMenu.addItem(commands_.cleanAll().createMenuItem(false));
            moreMenu.addSeparator();
         }

         // packages get additional commands
         else if (pkg)
         {
            moreMenu.addItem(commands_.devtoolsLoadAll().createMenuItem(false));
            moreMenu.addItem(commands_.rebuildAll().createMenuItem(false));
            moreMenu.addSeparator();
            moreMenu.addItem(commands_.testPackage().createMenuItem(false));
            moreMenu.addSeparator();
            moreMenu.addItem(commands_.checkPackage().createMenuItem(false));
            moreMenu.addSeparator();
            moreMenu.addItem(commands_.buildSourcePackage().createMenuItem(false));
            moreMenu.addItem(commands_.buildBinaryPackage().createMenuItem(false));
            moreMenu.addSeparator();
            moreMenu.addItem(commands_.roxygenizePackage().createMenuItem(false));
            moreMenu.addSeparator();
         }
         moreMenu.addItem(commands_.buildToolsProjectSetup().createMenuItem(false));

         // add more menu
         ToolbarMenuButton moreButton = new ToolbarMenuButton(
               "More",
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
                  "all", "All Formats", defaultFormat == "all");
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
            SelectionCommitEvent.fire(buildSubType(), format_);
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
         MenuItem allMenu = new FormatMenuItem("all", "All Formats", true);
         addItem(allMenu);
         addSeparator();
         String[] formats = session_.getSessionInfo().getQuartoConfig().project_formats;
         for (int i=0; i<formats.length; i++) 
         {
            addItem(new FormatMenuItem(formats[i], formatName(formats[i]) + " Format", false));
         }
      }

      public String getBookType()
      {
         // if there is only one format then return that
         String[] formats = session_.getSessionInfo().getQuartoConfig().project_formats;
         if (formats.length == 1)
         {
            return formats[0];
         }
         else
         {
            for (MenuItem item : getMenuItems())
            {
               FormatMenuItem fmtItem = (FormatMenuItem)item;
               
               if (fmtItem.isChecked())
                  return fmtItem.getFormat();
            }
            return "all";
         }
      }

      public void setBookType(String type)
      {
         // sync menu
         for (MenuItem item : getMenuItems())
         {
            FormatMenuItem fmtItem = (FormatMenuItem)item;
            fmtItem.setIsChecked(fmtItem.getFormat().equals(type));
         }
         syncBuildAllUI();
      }
      
      class FormatMenuItem extends CheckableMenuItem
      {
         public FormatMenuItem(String format, String label, boolean checked)
         {
            super(label);
            format_ = format;
            label_ = label;
            checked_ = checked;
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
            return checked_;
         }
         
         public void setIsChecked(boolean isChecked)
         {
            checked_ = isChecked;
            onStateChanged();
         }
       
         
         @Override
         public void onInvoked()
         {
            QuartoBookBuildPopupMenu.this.setBookType(format_);
            if (onQuartoBookBuildTypeChanged_ != null)
               onQuartoBookBuildTypeChanged_.execute();
         }

         private final String format_;
         private final String label_;
         private boolean checked_;
      
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
   public HasSelectionCommitHandlers<String> buildSubType()
   {
      return new HasSelectionCommitHandlers<String>() {

         @Override
         public void fireEvent(GwtEvent<?> event)
         {
            BuildPane.this.fireEvent(event);
         }

         @Override
         public HandlerRegistration addSelectionCommitHandler(
               SelectionCommitEvent.Handler<String> handler)
         {
            return BuildPane.this.addHandler(handler,
                                             SelectionCommitEvent.getType());
         }

      };
   }
   
   @Override
   public String getBookType()
   {
     return quartoBookBuildPopupMenu_.getBookType();
   }

   @Override
   public void setBookType(String type)
   {
      quartoBookBuildPopupMenu_.setBookType(type);
   }

   @Override
   public void onBookTypeChanged(Command onChanged)
   {
      onQuartoBookBuildTypeChanged_ = onChanged;
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
            buildAllButton_.setText("Build Book");
         }
         else 
         {
            buildAllButton_.setText("Build Website");
         }
      }
      else if (type == SessionInfo.BUILD_TOOLS_QUARTO)
      {
         QuartoConfig config = sessionInfo.getQuartoConfig();
         if (config.project_type == SessionInfo.QUARTO_PROJECT_TYPE_SITE)
         {
            buildAllButton_.setText("Render Site");
         }
         else if (config.project_type == SessionInfo.QUARTO_PROJECT_TYPE_BOOK)
         {
            if (config.project_formats.length == 1)
            {
               buildMenuButton_.setVisible(false);
               buildAllButton_.setText("Render Book");
            }
            else
            {
               buildMenuButton_.setVisible(true);
               if (getBookType() == "all")
                  buildAllButton_.setText("Render All Formats");
               else
                  buildAllButton_.setText("Render " + formatName(getBookType()));
            }
         }
         else
         {
            buildAllButton_.setText("Render Project");
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
         return "DOCX";
      else if (format == "epub")
         return "EPUB";
      else
         return format;
   }


   private ToolbarButton clearBuildButton_;
   private final Commands commands_;
   private final Session session_;
   private final BuildServerOperations server_;
   private String errorsBuildType_;
   private ToolbarButton buildAllButton_;
   private ToolbarMenuButton buildMenuButton_;
   private QuartoBookBuildPopupMenu quartoBookBuildPopupMenu_;
   private Command onQuartoBookBuildTypeChanged_;

   private final CompilePanel compilePanel_;

   
  

  
}
