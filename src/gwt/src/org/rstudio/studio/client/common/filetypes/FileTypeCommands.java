/*
 * FileTypeCommands.java
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
package org.rstudio.studio.client.common.filetypes;

import java.util.ArrayList;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandHandler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.HTMLCapabilities;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.packages.events.InstalledPackagesChangedEvent;
import org.rstudio.studio.client.workbench.views.packages.events.InstalledPackagesChangedHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FileTypeCommands
{
   public static class CommandWithId
   {
      private CommandWithId(String commandId, AppCommand command)
      {
         this.commandId = commandId;
         this.command = command;
      }

      public final String commandId;
      public final AppCommand command;
   }

   @Inject
   public FileTypeCommands(EventBus eventBus, 
                           Commands commands,
                           Session session,
                           final HTMLPreviewServerOperations server)
   {
      eventBus_ = eventBus;
      commands_ = commands;
      session_ = session;

      rMDCommand_ = addRFileType(FileTypeRegistry.RMARKDOWN, 
                                 "R _Markdown",
                                 "Create a new R Markdown file");
      rHTMLCommand_ = addRFileType(FileTypeRegistry.RHTML, 
                                   "R _HTML",
                                   "Create a new R HTML file");
      addRFileType(FileTypeRegistry.RD, 
                   "R _Documentation",
                   "Create a new R documentation file");
      
      // NOTE: we currently don't show these in the menus because
      // there are too many and they aren't core enough to most R users
      addTextFileType(FileTypeRegistry.TEX, "Te_X File", null);
      addTextFileType(FileTypeRegistry.MARKDOWN, "Mar_kdown File", null);  
      addTextFileType(FileTypeRegistry.HTML, "HTM_L File", null);
      addTextFileType(FileTypeRegistry.CSS, "_CSS File", null);
      addTextFileType(FileTypeRegistry.JS, "_JavaScript File", null);
          
      eventBus.addHandler(InstalledPackagesChangedEvent.TYPE,
                          new InstalledPackagesChangedHandler() {
         @Override
         public void onInstalledPackagesChanged(InstalledPackagesChangedEvent e)
         {
            server.getHTMLCapabilities(
                  new ServerRequestCallback<HTMLCapabilities>() {

                     @Override
                     public void onResponseReceived(HTMLCapabilities caps)
                     {
                        setHTMLCapabilities(caps);
                     }
                     @Override
                     public void onError(ServerError error)
                     {
                        Debug.logError(error);
                     }
                  });
         }
      });
   }
  
   public ArrayList<CommandWithId> rFileCommandsWithIds()
   {
      return rFileTypeCommands_;
   }
   
   // NOTE: we currently don't show these in the menus because
   // there are too many and they aren't core enough to most R users
   public ArrayList<CommandWithId> textFileCommandsWithIds()
   {
      return textFileTypeCommands_;
   }
   
   public TextFileType[] statusBarFileTypes()
   {
      ArrayList<TextFileType> types = new ArrayList<TextFileType>();
      types.add(FileTypeRegistry.R);
      types.add(FileTypeRegistry.SWEAVE);
      if (rMDCommand_.isEnabled())
         types.add(FileTypeRegistry.RMARKDOWN);
      if (rHTMLCommand_.isEnabled())
         types.add(FileTypeRegistry.RHTML);
      types.add(FileTypeRegistry.RD);
      types.add(FileTypeRegistry.TEXT);
      types.add(FileTypeRegistry.TEX);
      types.add(FileTypeRegistry.MARKDOWN);
      types.add(FileTypeRegistry.HTML);
      types.add(FileTypeRegistry.CSS);
      types.add(FileTypeRegistry.JS);
      
      return (TextFileType[])types.toArray(new TextFileType[0]);
   }
   
   public HTMLCapabilities getHTMLCapabiliites()
   {
      if (htmlCapabilities_ == null)
         setHTMLCapabilities(session_.getSessionInfo().getHTMLCapabilities());
      
      return htmlCapabilities_;
   }
   
   public void setHTMLCapabilities(HTMLCapabilities caps)
   {
      htmlCapabilities_ = caps;
      rMDCommand_.setEnabled(caps.isRMarkdownSupported());
      rMDCommand_.setVisible(caps.isRMarkdownSupported());
      rHTMLCommand_.setEnabled(caps.isRHtmlSupported());
      rHTMLCommand_.setVisible(caps.isRHtmlSupported());
   }
   
   private AppCommand addRFileType(TextFileType fileType, 
                                   String menuLabel,
                                   String desc) 
   {
      return addType(rFileTypeCommands_, fileType, menuLabel, desc);
   }
   
   private AppCommand addTextFileType(TextFileType fileType,  
                                      String menuLabel,
                                      String desc) 
   {
      return addType(textFileTypeCommands_, fileType, menuLabel, desc);
   }

   
   private AppCommand addType(ArrayList<CommandWithId> typeList,
                              final TextFileType fileType, 
                              String menuLabel,
                              String desc)
   {
      AppCommand command = new AppCommand();
      command.setMenuLabel(menuLabel);
      command.setImageResource(fileType.getDefaultIcon());
      if (desc != null)
         command.setDesc(desc);
      command.addHandler(new CommandHandler()
      {
         public void onCommand(AppCommand command)
         {
            eventBus_.fireEvent(new OpenSourceFileEvent(null,
                                                        fileType));
         }
      });

      String commandId = commandIdForType(fileType);
      commands_.addCommand(commandId, command);
      typeList.add(new CommandWithId(commandId, command));
      return command;
   }
   
   private static String commandIdForType(FileType fileType)
   {
      return "filetype_" + fileType.getTypeId();
   }

   private final EventBus eventBus_;
   private final Commands commands_;
   private final Session session_;
   
   private final ArrayList<CommandWithId> rFileTypeCommands_ =
         new ArrayList<CommandWithId>();
   
   private final ArrayList<CommandWithId> textFileTypeCommands_ =
         new ArrayList<CommandWithId>();
   
   private final AppCommand rMDCommand_;
   private final AppCommand rHTMLCommand_;
   private HTMLCapabilities htmlCapabilities_;

}
