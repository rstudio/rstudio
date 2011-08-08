/*
 * RStudioGinjector.java
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
package org.rstudio.studio.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import org.rstudio.studio.client.application.Application;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.ui.impl.DesktopApplicationHeader;
import org.rstudio.studio.client.application.ui.impl.WebApplicationHeader;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.NewFileMenu;
import org.rstudio.studio.client.common.impl.DesktopFileDialogs;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.source.DocsMenu;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;

@GinModules(RStudioGinModule.class)
public interface RStudioGinjector extends Ginjector
{
   void injectMembers(NewFileMenu newFileMenu);
   void injectMembers(DocsMenu docsMenu);
   void injectMembers(DesktopApplicationHeader desktopApplicationHeader);
   void injectMembers(WebApplicationHeader webApplicationHeader);
   void injectMembers(AceEditor aceEditor);
   void injectMembers(DesktopFileDialogs desktopFileDialogs);

   public static final RStudioGinjector INSTANCE = GWT.create(RStudioGinjector.class);

   Application getApplication() ;
   EventBus getEventBus() ;
   GlobalDisplay getGlobalDisplay();
   RemoteFileSystemContext getRemoteFileSystemContext();
   FileDialogs getFileDialogs();
   FileTypeRegistry getFileTypeRegistry();
   Commands getCommands();
   UIPrefs getUIPrefs();
   Session getSession();
}
