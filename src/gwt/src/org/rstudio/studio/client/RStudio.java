/*
 * RStudio.java
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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import org.rstudio.codemirror.client.resources.CodeMirrorResources;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.files.filedialog.FileDialogResources;
import org.rstudio.core.client.jsonrpc.JSON2;
import org.rstudio.core.client.resources.CoreResources;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.SlideLabel;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.ThemedPopupPanel;
import org.rstudio.core.client.widget.images.ProgressImages;
import org.rstudio.studio.client.application.ui.appended.ApplicationEndedPopupPanel;
import org.rstudio.studio.client.application.ui.serializationprogress.ApplicationSerializationProgress;
import org.rstudio.studio.client.application.ui.support.SupportPopupMenu;
import org.rstudio.studio.client.common.StudioResources;
import org.rstudio.studio.client.common.reditor.resources.REditorResources;
import org.rstudio.studio.client.impl.BrowserFence;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.history.view.HistoryPane;
import org.rstudio.studio.client.workbench.views.history.view.Shelf;
import org.rstudio.studio.client.workbench.views.packages.ui.InstallPackageDialog;
import org.rstudio.studio.client.workbench.views.plots.ui.ExportDialog;
import org.rstudio.studio.client.workbench.views.plots.ui.PrintDialog;
import org.rstudio.studio.client.workbench.views.plots.ui.manipulator.ManipulatorResources;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.PublishPdfDialog;
import org.rstudio.studio.client.workbench.views.workspace.dataimport.ImportFileSettingsDialog;
import org.rstudio.studio.client.workbench.views.workspace.dataimport.ImportGoogleSpreadsheetDialog;

public class RStudio implements EntryPoint
{  
   public void onModuleLoad() 
   {
      Debug.injectDebug();

      Document.get().getBody().getStyle().setBackgroundColor("#e1e2e5");

      BrowserFence fence = GWT.create(BrowserFence.class);
      fence.go(new Command()
      {
         public void execute()
         {
            Command dismissProgressAnimation = showProgress();
            delayLoad(dismissProgressAnimation);
         }
      });
   }

   private Command showProgress()
   {
      String progressUrl = ProgressImages.createLargeGray().getUrl();
      final DivElement div = Document.get().createDivElement();
      div.setInnerHTML("<img src=\"" + progressUrl + "\"/>");
      div.getStyle().setWidth(100, Style.Unit.PCT);
      div.getStyle().setMarginTop(200, Style.Unit.PX);
      div.getStyle().setProperty("textAlign", "center");
      div.getStyle().setZIndex(1000);
      Document.get().getBody().appendChild(div);

      return new Command()
      {
         public void execute()
         {
            Document.get().getBody().removeChild(div);
         }
      };
   }

   private void delayLoad(final Command dismissProgressAnimation)
   {
      GWT.runAsync(new RunAsyncCallback()
      {
         public void onFailure(Throwable reason)
         {
            dismissProgressAnimation.execute();
            Window.alert("Error: " + reason.getMessage());
         }

         public void onSuccess()
         {
            load(dismissProgressAnimation);
         }
      });
   }

   private void load(Command dismissProgressAnimation)
   {
      ThemeResources.INSTANCE.themeStyles().ensureInjected();
      CoreResources.INSTANCE.styles().ensureInjected();
      StudioResources.INSTANCE.styles().ensureInjected();
      ConsoleResources.INSTANCE.consoleStyles().ensureInjected();
      REditorResources.INSTANCE.styles().ensureInjected(); 
      FileDialogResources.INSTANCE.styles().ensureInjected();
      CodeMirrorResources.INSTANCE.linenumbers().ensureInjected();
      ManipulatorResources.INSTANCE.manipulatorStyles().ensureInjected();
      
      SupportPopupMenu.ensureStylesInjected();
      SlideLabel.ensureStylesInjected();
      ThemedButton.ensureStylesInjected();
      ThemedPopupPanel.ensureStylesInjected();
      PrintDialog.ensureStylesInjected();
      InstallPackageDialog.ensureStylesInjected();
      PublishPdfDialog.ensureStylesInjected();
      ExportDialog.ensureStylesInjected();
      ApplicationEndedPopupPanel.ensureStylesInjected();
      ApplicationSerializationProgress.ensureStylesInjected();
      HistoryPane.ensureStylesInjected();
      Shelf.ensureStylesInjected();
      ImportFileSettingsDialog.ensureStylesInjected();
      ImportGoogleSpreadsheetDialog.ensureStylesInjected();
      FindReplaceBar.ensureStylesInjected();
      FontSizer.ensureStylesInjected();

      JSON2.ensureInjected();

      StyleInjector.inject(
            "button::-moz-focus-inner {border:0}");

      RStudioGinjector.INSTANCE.getApplication().go(RootLayoutPanel.get(),
                                                    dismissProgressAnimation);
   }
}
