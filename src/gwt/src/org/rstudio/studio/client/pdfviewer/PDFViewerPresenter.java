/*
 * PDFViewerPresenter.java
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
package org.rstudio.studio.client.pdfviewer;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.*;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.common.compilepdf.dialog.CompilePdfProgressDialog;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfCompletedEvent;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfStartedEvent;
import org.rstudio.studio.client.common.compilepdf.model.CompilePdfResult;
import org.rstudio.studio.client.common.compilepdf.model.CompilePdfServerOperations;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.synctex.Synctex;
import org.rstudio.studio.client.common.synctex.events.SynctexStatusChangedEvent;
import org.rstudio.studio.client.common.synctex.events.SynctexViewPdfEvent;
import org.rstudio.studio.client.common.synctex.model.PdfLocation;
import org.rstudio.studio.client.pdfviewer.events.InitCompleteEvent;
import org.rstudio.studio.client.pdfviewer.events.PageClickEvent;
import org.rstudio.studio.client.pdfviewer.model.PDFViewerParams;
import org.rstudio.studio.client.pdfviewer.model.SyncTexCoordinates;
import org.rstudio.studio.client.pdfviewer.pdfjs.PDFView;
import org.rstudio.studio.client.pdfviewer.pdfjs.events.PDFLoadEvent;
import org.rstudio.studio.client.pdfviewer.pdfjs.events.PageChangeEvent;
import org.rstudio.studio.client.pdfviewer.ui.PDFViewerToolbarDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;

public class PDFViewerPresenter implements IsWidget, 
                                           CompilePdfStartedEvent.Handler,
                                           CompilePdfCompletedEvent.Handler,
                                           SynctexViewPdfEvent.Handler,
                                           SynctexStatusChangedEvent.Handler
{
   public interface Binder extends CommandBinder<Commands, PDFViewerPresenter>
   {}

   public interface Display extends IsWidget,
                                    PageClickEvent.HasPageClickHandlers
   {     
      void setURL(String url);
      PDFViewerToolbarDisplay getToolbarDisplay();
      HandlerRegistration addInitCompleteHandler(
                                             InitCompleteEvent.Handler handler);
      void closeWindow();
      void toggleThumbnails();

      void updateSelectedPage(int pageNumber);

      /**
       * @return Coordinates of the page area that's at the top of the viewer,
       * or null if no pages loaded (or no page is currently onscreen for some
       * other reason).
       */
      SyncTexCoordinates getTopCoordinates();
   }
   
   @Inject
   public PDFViewerPresenter(Display view,
                             EventBus eventBus,
                             Binder binder,
                             Commands commands,
                             FileTypeRegistry fileTypeRegistry,
                             CompilePdfServerOperations server,
                             GlobalDisplay globalDisplay,
                             Synctex synctex)
   {
      view_ = view;
      fileTypeRegistry_ = fileTypeRegistry;
      server_ = server;
      globalDisplay_ = globalDisplay;
      commands_ = commands;
      synctex_ = synctex;
      
      binder.bind(commands, this);
      
      
      
      eventBus.addHandler(CompilePdfStartedEvent.TYPE, this);
      eventBus.addHandler(CompilePdfCompletedEvent.TYPE, this);
      
      eventBus.addHandler(SynctexViewPdfEvent.TYPE, this);
      
      synctex_.enableCommands(false);
      eventBus.addHandler(SynctexStatusChangedEvent.TYPE, this);
      
      Window.addWindowClosingHandler(new ClosingHandler() {

         @Override
         public void onWindowClosing(ClosingEvent event)
         {
            if (compileIsRunning_)
               terminateRunningCompile();
         }
      });

      final PDFViewerToolbarDisplay toolbar = view_.getToolbarDisplay();
     
      toolbar.getViewPdfExternal().addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            commands_.showPdfExternal().execute();
         }
      });
      
      toolbar.getJumpToSource().setVisible(false);
      toolbar.getJumpToSource().addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            commands_.synctexSearch().execute();
         }
      });
      
      toolbar.getPrevButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            PDFView.previousPage();
         }
      });
      toolbar.getNextButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            PDFView.nextPage();
         }
      });
      toolbar.getThumbnailsButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            view_.toggleThumbnails();
         }
      });

      final HasValue<String> pageNumber =
                                      toolbar.getPageNumber();
      pageNumber.addValueChangeHandler(new ValueChangeHandler<String>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            String value = pageNumber.getValue();
            try
            {
               int intVal = Integer.parseInt(value);
               if (intVal != PDFView.currentPage()
                   && intVal >= 1 && intVal <= PDFView.pageCount())
               {
                  PDFView.goToPage(intVal);
                  toolbar.selectPageNumber();
                  return;
               }
            }
            catch (NullPointerException ignored)
            {
            }
            catch (NumberFormatException ignored)
            {
            }

            pageNumber.setValue(PDFView.currentPage() + "", false);
         }
      });
      toolbar.getZoomIn().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            PDFView.zoomIn();
         }
      });
      toolbar.getZoomOut().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            PDFView.zoomOut();
         }
      });

      releaseOnDismiss_.add(PDFView.addPageChangeHandler(new PageChangeEvent.Handler()
      {
         @Override
         public void onPageChange(PageChangeEvent event)
         {
            updatePageNumber();
         }
      }));
      releaseOnDismiss_.add(PDFView.addPDFLoadHandler(new PDFLoadEvent.Handler()
      {
         @Override
         public void onPDFLoad(PDFLoadEvent event)
         {
            toolbar.setPageCount(PDFView.pageCount());
            if (navigateHereOnLoad_ != null)
            {
               PDFView.navigateTo(navigateHereOnLoad_);
               navigateHereOnLoad_ = null;
            }
            else
            {
               PDFView.scrollToTop();
            }
         }
      }));

      view_.addPageClickHandler(new PageClickEvent.Handler()
      {
         @Override
         public void onPageClick(PageClickEvent event)
         {
            if (synctex_.isSynctexAvailable())
            {
               SyncTexCoordinates coord = event.getCoordinates();
               synctexInverseSearch(coord.getX(), coord.getY());
            }
         }
      });
   }

   private void updatePageNumber()
   {
      view_.getToolbarDisplay().getPageNumber().setValue(
                                             PDFView.currentPage() + "", false);
      view_.updateSelectedPage(PDFView.currentPage());
   }
   
   public void onActivated(PDFViewerParams pdfParams)
   {
   }
  
   
   @Override
   public void onCompilePdfStarted(CompilePdfStartedEvent event)
   {
      view_.getToolbarDisplay().setPdfFile(
                        FileSystemItem.createFile(event.getPdfPath()));

      updateState(true);
      
      dismissProgressDialog();
      PDFView.setLoadingVisible(false);
      
      activeProgressDialog_  = new CompilePdfProgressDialog();
      
      activeProgressDialog_.addClickHandler(new ClickHandler() {

         @Override
         public void onClick(ClickEvent event)
         {
            if (compileIsRunning_)
            {
               terminateRunningCompile();
            }
            else
            {
               dismissProgressDialog();
               
               if (PDFView.pageCount() == 0)
                  view_.closeWindow();
            }
         }

      });

      activeProgressDialog_.addSelectionCommitHandler(
            new SelectionCommitHandler<CodeNavigationTarget>() {

               @Override
               public void onSelectionCommit(
                     SelectionCommitEvent<CodeNavigationTarget> event)
               {
                  CodeNavigationTarget target = event.getSelectedItem();
                  
                  editFile(FileSystemItem.createFile(target.getFile()), 
                                                     target.getPosition());
                  dismissProgressDialog();
               }
            });

      activeProgressDialog_.showModal();
   }
   
   @Override
   public void onCompilePdfCompleted(CompilePdfCompletedEvent event)
   {  
      CompilePdfResult result = event.getResult();
      
      updateState(false, result);
      
      if (result.getSucceeded())
      {
         navigateHereOnLoad_ = result.getPdfPath().equals(StringUtil.notNull(lastSuccessfulPdfPath_))
                               ? PDFView.getNavigateDest()
                               : null;
         lastSuccessfulPdfPath_ = result.getPdfPath();

         view_.setURL(result.getViewPdfUrl());
      }
   }
   
   @Override
   public void onSynctexViewPdf(SynctexViewPdfEvent event)
   {
      PDFView.goToPage(event.getPdfLocation().getPage());
   }
   
   @Override
   public void onSynctexStatusChanged(SynctexStatusChangedEvent event)
   {
      synctex_.enableCommands(synctex_.isSynctexAvailable());
      
      view_.getToolbarDisplay().getJumpToSource().setVisible(
                                             synctex_.isSynctexAvailable());
   }
   
   @Handler
   public void onSynctexSearch()
   {
      synctexInverseSearch(120,120);
   }
   
   private void synctexInverseSearch(int x, int y)
   {
      String pdfPath = lastResult_.getPdfPath();
      if (pdfPath != null)
      {
         synctex_.inverseSearch(PdfLocation.create(pdfPath,
                                                   PDFView.currentPage(),
                                                   x, 
                                                   y, 
                                                   0, 
                                                   0));
      }
   }
   
   @Handler
   public void onShowPdfExternal()
   {
      String pdfPath = getCompiledPdfPath();
      if (pdfPath != null)
      {
         if (Desktop.isDesktop())
         {
            Desktop.getFrame().showPdf(pdfPath);
         }
         else
         {
            String pdfURL = server_.getFileUrl(
                                       FileSystemItem.createFile(pdfPath));
            NewWindowOptions options = new NewWindowOptions();
            options.setName("_rstudio_compile_pdf");
            globalDisplay_.openWindow(pdfURL, options);
         }
      }
   }

   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
   }

   public HandlerRegistration addInitCompleteHandler(InitCompleteEvent.Handler handler)
   {
      return view_.addInitCompleteHandler(handler);
   }

   private void editFile(FileSystemItem file, FilePosition position)
   {
      fileTypeRegistry_.editFile(file, position);
 
      // firefox and chrome frame won't allow window re-activation
      // so we close the parent window to force this
      if (BrowseCap.isFirefox() || BrowseCap.isChromeFrame())
         view_.closeWindow();
   }
  
   private void updateState(boolean running)
   {
      updateState(running, null);
   }
   
   private void updateState(boolean running, CompilePdfResult result)
   {
      compileIsRunning_ = running;
      lastResult_ = result;
      
      boolean havePdf = getCompiledPdfPath() != null;
      commands_.showPdfExternal().setEnabled(havePdf);
   }
   
   private String getCompiledPdfPath()
   {
      if (lastResult_ != null)
         return lastResult_.getPdfPath();
      else
         return null;
   }
   
   private void dismissProgressDialog()
   {
      if (activeProgressDialog_ != null)
      {
         activeProgressDialog_.dismiss();
         activeProgressDialog_ = null;
      }
   }
   
   private void terminateRunningCompile()
   {
      server_.terminateCompilePdf(new ServerRequestCallback<Boolean>() 
      {
         @Override
         public void onError(ServerError error)
         {
            globalDisplay_.showErrorMessage("Error", 
                                            error.getUserMessage());
            
         }
      });
   }

   private boolean compileIsRunning_ = false;
  
   private CompilePdfProgressDialog activeProgressDialog_;
   private CompilePdfResult lastResult_ = null;
   
   private final Display view_;
   private final Synctex synctex_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final CompilePdfServerOperations server_;
   private final GlobalDisplay globalDisplay_;
   private final Commands commands_; 

   private HandlerRegistrations releaseOnDismiss_ = new HandlerRegistrations();

   private String lastSuccessfulPdfPath_;
   private JavaScriptObject navigateHereOnLoad_;
}
