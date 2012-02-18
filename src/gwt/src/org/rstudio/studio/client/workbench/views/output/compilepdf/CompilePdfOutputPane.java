/*
 * CompilePdfOutputPane.java
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
package org.rstudio.studio.client.workbench.views.output.compilepdf;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.TableColElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.shell.ShellWidget;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.output.compilepdf.model.CompilePdfError;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;

import java.util.ArrayList;

public class CompilePdfOutputPane extends WorkbenchPane
      implements CompilePdfOutputPresenter.Display,
                 HasSelectionCommitHandlers<CodeNavigationTarget>
{
   @Inject
   public CompilePdfOutputPane(FileTypeRegistry fileTypeRegistry)
   {
      super("Compile PDF");
      fileTypeRegistry_ = fileTypeRegistry;
      ensureWidget();
   }

   @Override
   protected Widget createMainWidget()
   {
      res_ = GWT.create(CompilePdfOutputResources.class);

      panel_ = new SimplePanel();
      
      outputWidget_ = new ShellWidget(new AceEditor());
      outputWidget_.setSize("100%", "100%");
      outputWidget_.setMaxOutputLines(1000);
      outputWidget_.setReadOnly(true);
      outputWidget_.setSuppressPendingInput(true);
      panel_.setWidget(outputWidget_);

      codec_ = new CompilePdfErrorItemCodec(res_, false);
      errorTable_ = new FastSelectTable<CompilePdfError, CodeNavigationTarget, CodeNavigationTarget>(
            codec_,
            res_.styles().selectedRow(),
            true,
            false);
      setWidths();
      errorTable_.setStyleName(res_.styles().table());
      errorTable_.setSize("100%", "100%");
      errorTable_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            if (doubleClick_.checkForDoubleClick(event.getNativeEvent()))
            {
               ArrayList<CodeNavigationTarget> values =
                                                errorTable_.getSelectedValues();
               if (values.size() == 1)
               {
                  SelectionCommitEvent.fire(CompilePdfOutputPane.this,
                                            values.get(0));
               }
            }
         }
         private final DoubleClickState doubleClick_ = new DoubleClickState();
      });
      
      errorTable_.addMouseDownHandler(new MouseDownHandler()
      {
         public void onMouseDown(MouseDownEvent event)
         {
            Element el = DOM.eventGetTarget((Event) event.getNativeEvent());
            if (el != null
                && el.getTagName().equalsIgnoreCase("div")
                && el.getClassName().equals(res_.styles().disclosure()))
            {
               ArrayList<CodeNavigationTarget> values =
                                             errorTable_.getSelectedValues2();
               if (values.size() == 1)
               {
                  SelectionCommitEvent.fire(CompilePdfOutputPane.this,
                                            values.get(0));
               }
            }
         }
      });
      
      errorPanel_ = new ScrollPanel(errorTable_);
      errorPanel_.setSize("100%", "100%");

      return panel_;
   }

   private void setWidths()
   {
      setColumnClasses(errorTable_.getElement().<TableElement>cast(),
                       res_.styles().iconCell(),
                       res_.styles().lineCell(),
                       res_.styles().messageCell());
   }

   private void setColumnClasses(TableElement table,
                                 String... classes)
   {
      TableColElement colGroupElement = Document.get().createColGroupElement();
      for (String clazz : classes)
      {
         TableColElement colElement = Document.get().createColElement();
         colElement.setClassName(clazz);
         colGroupElement.appendChild(colElement);
      }
      table.appendChild(colGroupElement);
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      
      fileImage_ = new Image(); 
      toolbar.addLeftWidget(fileImage_);
      
      fileLabel_ = new ToolbarLabel();
      fileLabel_.addStyleName(ThemeStyles.INSTANCE.subtitle());
      toolbar.addLeftWidget(fileLabel_);
      
      ImageResource stopImage = RStudioGinjector.INSTANCE.getCommands()
                                               .interruptR().getImageResource();
      stopButton_ = new ToolbarButton(stopImage, null);
      stopButton_.setVisible(false);
      toolbar.addRightWidget(stopButton_);
      
      showOutputButton_ = new LeftRightToggleButton("Output",  "Errors", false);
      showOutputButton_.setVisible(false);
      showOutputButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
           showOutputButton_.setVisible(false);
           showErrorsButton_.setVisible(true);
           panel_.setWidget(outputWidget_);
         }
      });
      toolbar.addRightWidget(showOutputButton_);
       
      showErrorsButton_ = new LeftRightToggleButton("Output",  "Errors", true);
      showErrorsButton_.setVisible(false);
      showErrorsButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
           showOutputButton_.setVisible(true);
           showErrorsButton_.setVisible(false);
           panel_.setWidget(errorPanel_);
         }
      });
      toolbar.addRightWidget(showErrorsButton_);
     
      return toolbar;
   }
  
   @Override
   public void compileStarted(String fileName)
   {
      clearAll();

      fileName_ = fileName;

      fileImage_.setResource(fileTypeRegistry_.getIconForFilename(fileName));
      
      String shortFileName = StringUtil.shortPathName(
                                 FileSystemItem.createFile(fileName), 
                                 ThemeStyles.INSTANCE.subtitle(), 
                                 350);
      
      fileLabel_.setText(shortFileName);
      
      showOutputButton_.setVisible(false);
      showErrorsButton_.setVisible(false);
      stopButton_.setVisible(true);
   }

   @Override
   public void clearAll()
   {
      fileName_ = null;
      showOutputButton_.setVisible(false);
      showErrorsButton_.setVisible(false);
      stopButton_.setVisible(false);
      outputWidget_.clearOutput();
      errorTable_.clear();
      setWidths();
      panel_.setWidget(outputWidget_);  
   }
   
   @Override
   public void showOutput(String output)
   {
      outputWidget_.consoleWriteOutput(output);  
   }
   

   @Override
   public void showErrors(JsArray<CompilePdfError> errors)
   {
      boolean showFileHeaders = false;
      ArrayList<CompilePdfError> errorList = new ArrayList<CompilePdfError>();
      for (CompilePdfError error : JsUtil.asIterable(errors))
      {
         if (!error.getPath().equals(fileName_))
            showFileHeaders = true;
         errorList.add(error);
      }

      codec_.setShowFileHeaders(showFileHeaders);
      errorTable_.addItems(errorList, false);
      panel_.setWidget(errorPanel_);
      
      showOutputButton_.setVisible(true);
   }

   @Override
   public void compileCompleted()
   {
      stopButton_.setVisible(false);
   }
   
   @Override
   public HasClickHandlers stopButton()
   {
      return stopButton_;
   }
  
   @Override
   public HasSelectionCommitHandlers<CodeNavigationTarget> errorList()
   {
      return this;
   }
   
   @Override
   public HandlerRegistration addSelectionCommitHandler(
                           SelectionCommitHandler<CodeNavigationTarget> handler)
   {
      return addHandler(handler, SelectionCommitEvent.getType());
   }
  
   private Image fileImage_;
   private ToolbarLabel fileLabel_;
   private ToolbarButton stopButton_;
   private LeftRightToggleButton showOutputButton_;
   private LeftRightToggleButton showErrorsButton_;
   private SimplePanel panel_;
   private ShellWidget outputWidget_;
   private FastSelectTable<CompilePdfError, CodeNavigationTarget, CodeNavigationTarget>
                                                                    errorTable_;
   private ScrollPanel errorPanel_;
   private FileTypeRegistry fileTypeRegistry_;
   private CompilePdfOutputResources res_;
   private String fileName_;
   private CompilePdfErrorItemCodec codec_;
}
