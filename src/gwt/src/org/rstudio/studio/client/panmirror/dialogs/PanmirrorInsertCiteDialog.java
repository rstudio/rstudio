package org.rstudio.studio.client.panmirror.dialogs;

import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.DomMetrics;
import org.rstudio.core.client.widget.FormListBox;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertCitePreviewPair;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertCiteProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertCiteResult;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertCiteUI;
import org.rstudio.studio.client.panmirror.server.PanmirrorCrossrefServerOperations;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUITools;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUIToolsCitation;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import jsinterop.base.Js;

public class PanmirrorInsertCiteDialog extends ModalDialog<PanmirrorInsertCiteResult>
{

   public PanmirrorInsertCiteDialog(PanmirrorInsertCiteProps citeProps,
         OperationWithInput<PanmirrorInsertCiteResult> operation)
   {
      super(title(citeProps.doi), Roles.getDialogRole(), operation, () -> {
         operation.execute(null);
      });

      RStudioGinjector.INSTANCE.injectMembers(this);
      mainWidget_ = GWT.<Binder> create(Binder.class).createAndBindUi(this);
      citeProps_ = citeProps;

      setBibliographies(citeProps.bibliographyFiles);
      previewScrollPanel_.setSize("100%", "160px");

      if (citeProps_.citeUI != null)
      {
         // We were given a fully rendered citeProps that includes the CiteUI,
         // we can just display it immediately
         onCiteUI(citeProps_.citeUI);
      }
      else
      {
         // We were given an incomplete DOI, we need to look it up
         setEnabled(false);
         canceled_ = false;

         ProgressIndicator indicator = addProgressIndicator(false);
         indicator.onProgress("Looking Up DOI..", () -> {
            canceled_ = true;
            super.closeDialog();
         });

         // Lookup the DOI using Crossref
         server_.crossrefDoi(citeProps_.doi, new ServerRequestCallback<JavaScriptObject>()
         {
            @Override
            public void onResponseReceived(JavaScriptObject response)
            {
               // User canceled the dialog, just ignore the server response
               if (canceled_)
               {
                  return;
               }

               // Get the preview and suggested Id
               citeProps_.work = Js.uncheckedCast(response);
               PanmirrorUIToolsCitation citationTools = new PanmirrorUITools().citation;
               PanmirrorInsertCiteUI citeUI = citationTools.citeUI(citeProps_);
               citeProps_.citeUI = citeUI;
               onCiteUI(citeUI);

               // Enable the UI and Focus the Citation Id
               setEnabled(true);
               citationId_.selectAll();
               citationId_.setFocus(true);              
               
               indicator.onCompleted();
            }

            @Override
            public void onError(ServerError error)
            {
               // User canceled the dialog, just ignore the server response
               if (canceled_)
               {
                  return;
               }

               indicator.onError(error.getUserMessage());
            }
         });
      }
   }

   @Override
   public void focusInitialControl()
   {
      super.focusInitialControl();
      citationId_.selectAll();
   }

   @Override
   protected PanmirrorInsertCiteResult collectInput()
   {
      PanmirrorInsertCiteResult result = new PanmirrorInsertCiteResult();
      result.id = citationId_.getText();
      if (createBibliographyFileName_.isEnabled() && createBibliographyFileName_.getText().length() > 0) {
         result.bibliographyFile = createBibliographyFileName_.getText().trim();
      } else {
         result.bibliographyFile = bibliographies_.getValue(bibliographies_.getSelectedIndex());
      }     
      result.work = citeProps_.work;
      return result;
   }

   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }

   @Inject
   void initialize(PanmirrorCrossrefServerOperations server)
   {
      server_ = server;
   }

   private void onCiteUI(PanmirrorInsertCiteUI citeUI)
   {
      citationId_.setText(citeUI.suggestedId);
      displayPreview(citeUI.previewFields);
   }

   private int addPreviewRow(String label, String value, int row)
   {
      if (value != null && value.length() > 0)
      {
         previewTable_.setText(row, 0, label);
         previewTable_.getFlexCellFormatter().addStyleName(row, 0,
               RES.styles().flexTablePreviewName());
         previewTable_.setText(row, 1, value);
         previewTable_.getFlexCellFormatter().addStyleName(row, 1,
               RES.styles().flexTablePreviewValue());
         return ++row;
      }
      return row;
   }

   private void setEnabled(boolean enabled)
   {
      citationId_.setEnabled(enabled);
      bibliographies_.setEnabled(enabled);
      createBibliographyFileName_.setEnabled(enabled);
      if (enabled)
      {
         mainPanel_.removeStyleName(RES.styles().disabled());
      }
      else
      {
         mainPanel_.addStyleName(RES.styles().disabled());
      }

   }

   private void setBibliographies(String[] bibliographyFiles)
   {
      if (bibliographyFiles.length == 0)
      {
         // There isn't a currently configured bibliography
         // Show create UI
         createBibliographyPanel_.setVisible(true);
         addTobibliographyPanel_.setVisible(false);
         createBibliographyFileName_.setText("references.bib");
      }
      else
      {
         // There is a currently configured bibliography
         // Show add UI
         createBibliographyPanel_.setVisible(false);
         addTobibliographyPanel_.setVisible(true);
         for (String file : bibliographyFiles)
         {
            bibliographies_.addItem(file);
         }
      }
   }

   private void displayPreview(PanmirrorInsertCitePreviewPair[] previewPairs)
   {
      previewTable_.clear();
      int row = 0;
      for (PanmirrorInsertCitePreviewPair pair : previewPairs)
      {
         row = addPreviewRow(pair.name, pair.value, row);
      }
   }
   
   // Root panel
   @UiField
   VerticalPanel mainPanel_;
   
   // Citation Id
   @UiField
   Label citationLabel_;
   @UiField
   TextBox citationId_;

   // Preview
   @UiField
   ScrollPanel previewScrollPanel_;
   @UiField
   FlexTable previewTable_;
   
   // Bibliography information
   @UiField
   VerticalPanel addTobibliographyPanel_;
   @UiField
   FormListBox bibliographies_;
   @UiField
   VerticalPanel createBibliographyPanel_;
   @UiField
   TextBox createBibliographyFileName_;

   
   interface Binder extends UiBinder<Widget, PanmirrorInsertCiteDialog>
   {
   }

   private static String title(String doi) {
      String title = "Citation from DOI";
      String doiTitle = title + ": " + doi;
      if (DomMetrics.measureHTML(doiTitle).width < 350)
         return doiTitle;
      else
         return title;      
   }

   private Widget mainWidget_;
   private PanmirrorCrossrefServerOperations server_;
   private boolean canceled_;
   private PanmirrorInsertCiteProps citeProps_;

   private static PanmirrorDialogsResources RES = PanmirrorDialogsResources.INSTANCE;

}
