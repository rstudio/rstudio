package org.rstudio.studio.client.panmirror.dialogs;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomMetrics;
import org.rstudio.core.client.widget.FormListBox;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertCiteField;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertCiteProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertCiteResult;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertCiteUI;
import org.rstudio.studio.client.panmirror.server.PanmirrorDOIServerOperations;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUITools;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUIToolsCitation;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.UserState;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import elemental2.core.JsObject;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;

public class PanmirrorInsertCiteDialog extends ModalDialog<PanmirrorInsertCiteResult>
{

   private enum BibliographyType {
      biblatex {
         @Override
         public String displayName()
         {
            return "BibLaTeX";
         }

         @Override
         public String fileExtension()
         {
            return "bib";
         }
      },
      yaml {
         @Override
         public String displayName()
         {
            return "CSL-YAML";
         }

         @Override
         public String fileExtension()
         {
            return "yaml";
         }
      },
      json {
         @Override
         public String displayName()
         {
            return "CSL-JSON";
         }

         @Override
         public String fileExtension()
         {
            return "json";
         }
      };
      
      public abstract String displayName();
      public abstract String fileExtension();
   }
   
   public PanmirrorInsertCiteDialog(PanmirrorInsertCiteProps citeProps,
         OperationWithInput<PanmirrorInsertCiteResult> operation)
   {
      super(title(citeProps.doi, citeProps.provider), Roles.getDialogRole(), operation, () -> {
         operation.execute(null);
      });

      RStudioGinjector.INSTANCE.injectMembers(this);
      mainWidget_ = GWT.<Binder> create(Binder.class).createAndBindUi(this);
      citeProps_ = citeProps;
      
      citationId_.getElement().setAttribute("spellcheck", "false");
      
      // Bibliography Types (for when user is creating a new bibliography)
      for (BibliographyType bibType : BibliographyType.values()) {
         createBibliographyTypes_.addItem(bibType.displayName(), bibType.fileExtension());   
      }
      
      createBibliographyTypes_.addChangeHandler(new ChangeHandler(){

         @Override
         public void onChange(ChangeEvent arg0)
         {
            String extension = createBibliographyTypes_.getSelectedValue();
            String currentFileName = createBibliographyFileName_.getValue();
            createBibliographyFileName_.setValue(ensureExtension(currentFileName, extension)); 
            userState_.bibliographyDefaultType().setGlobalValue(extension);
            userState_.writeState();
         }});

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
         server_.doiFetchCSL(citeProps_.doi, new ServerRequestCallback<JavaScriptObject>()
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
               JsPropertyMap<Object> responseObj = Js.asAny(response).asPropertyMap();
               String status = (String) responseObj.get("status");
               if (status.equals("ok")) {

                  citeProps_.csl = (JsObject) responseObj.getAsAny("message");
                  
                  PanmirrorUIToolsCitation citationTools = new PanmirrorUITools().citation;
                  PanmirrorInsertCiteUI citeUI = citationTools.citeUI(citeProps_);
                  citeProps_.citeUI = citeUI;
                  onCiteUI(citeUI);

                  // Enable the UI and Focus the Citation Id
                  setEnabled(true);
                  citationId_.selectAll();
                  citationId_.setFocus(true);              
                  indicator.onCompleted();

               } else {
                  indicator.onCompleted();
                  displayErrorForStatus(status);
               }
            }

            @Override
            public void onError(ServerError error)
            {
               // User canceled the dialog, just ignore the server response
               if (canceled_)
               {
                  return;
               }
               indicator.onCompleted();
               displayError(kUnknownError);
            }
         });
      }
   }
   

   @Inject
   void initialize(PanmirrorDOIServerOperations server, 
                   UserState userState)
   {
      server_ = server;
      userState_ = userState;
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
      if (addTobibliographyPanel_.isVisible() && bibliographies_.getSelectedIndex() < bibliographies_.getItemCount()) 
      {
         result.bibliographyFile = bibliographies_.getValue(bibliographies_.getSelectedIndex());
      } 
      else 
      {
         result.bibliographyFile = createBibliographyFileName_.getText().trim();
      }
      result.csl = citeProps_.csl;
      return result;
   }
   
   @Override
   protected boolean validate(PanmirrorInsertCiteResult result)
   {
      GlobalDisplay globalDisplay = RStudioGinjector.INSTANCE.getGlobalDisplay();
      
      
      if (StringUtil.isNullOrEmpty(result.id))
      {
         globalDisplay.showErrorMessage(
            "Error", "You must provide a value for the citation id."
         );
         citationId_.setFocus(true);
         return false;
      }
      
      
      if (!isValidCitationId(result.id)) {
         globalDisplay.showErrorMessage(
               "Error", "Please provide a validation citation Id."
            );
            citationId_.setFocus(true);
            return false;        
      }
      
      // Bib text keys should be treated in a case insensitive manner
      // http://maverick.inria.fr/~Xavier.Decoret/resources/xdkbibtex/bibtex_summary.html#stringdef
      for (String existingId : citeProps_.existingIds) {
         if (existingId.compareToIgnoreCase(result.id) == 0)
         {
            globalDisplay.showErrorMessage(
                  "Error", "Please select a unique citation Id."
               );
               citationId_.setFocus(true);
               return false;                   
         }
      }
      
      if (StringUtil.isNullOrEmpty(result.bibliographyFile)) 
      {
         if (addTobibliographyPanel_.isVisible()) {
            globalDisplay.showErrorMessage(
                  "Error", "You must select a bibliography."
               );
            bibliographies_.setFocus(true);            
         } else {
            globalDisplay.showErrorMessage(
                  "Error", "You must provide a bibliography file name."
               );            
            createBibliographyFileName_.setFocus(true);
         }
         return false;         
      }
      
      return true;
   }


   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
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
   
   // Very hard to figure out valid citation ids, but 
   // they look to be based upon this:
   // https://bibdesk.sourceforge.io/manual/BibDeskHelp_2.html
   private boolean isValidCitationId(String citationId) {      
      String invalidCharsRegex = ".*[\\s@',\\\\#}{~%&$^_].*";
      return !citationId.matches(invalidCharsRegex);
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
         createBibliographyTypes_.setVisible(true);
         createBibliographyTypes_.setSelectedIndex(
            createBibliographyTypes_.getIndexFromValue(userState_.bibliographyDefaultType().getValue())
         );
         createBibliographyFileName_.setText(ensureExtension("references", createBibliographyTypes_.getSelectedValue()));
      }
      else
      {
         // There is a currently configured bibliography
         // Show add UI
         createBibliographyPanel_.setVisible(false);
         addTobibliographyPanel_.setVisible(true);
         createBibliographyTypes_.setVisible(false);
         for (String file : bibliographyFiles)
         {
            bibliographies_.addItem(file);
         }
      }
   } 

   private void displayPreview(PanmirrorInsertCiteField[] fields)
   {
      previewTable_.clear();
      int row = 0;
      for (PanmirrorInsertCiteField field : fields)
      {
         row = addPreviewRow(field.name, field.value, row);
      }
   }
   
   private void displayError(String message) {
      GlobalDisplay globalDisplay = RStudioGinjector.INSTANCE.getGlobalDisplay();
      globalDisplay.showErrorMessage(
            "DOI Unavailable", 
            message,
            new Operation(){
               @Override
               public void execute()
               {
                  PanmirrorInsertCiteDialog.super.closeDialog();
               }});
   }
   
   private void displayErrorForStatus(String status) { 
      if (status.equals("notfound")) {

         displayError(kNoDataError);
         
      } else if (status.equals("nohost")) {
         displayError(kServerError);
         
      } else {
         // Error
         displayError(kUnknownError);
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
   @UiField
   FormListBox createBibliographyTypes_;

   
   interface Binder extends UiBinder<Widget, PanmirrorInsertCiteDialog>
   {
   }

   private static String title(String doi, String provider) {
      String title = "Citation from DOI";
      if (provider != null && provider.length() > 0) {
         return "Citation from " + provider;
      } else {
         String doiTitle = title + ": " + doi;
         if (DomMetrics.measureHTML(doiTitle).width < 350)
            return doiTitle;
         else
            return title;
      }
   }
   
   private static String ensureExtension(String fileName, String extension) {
      int lastDot = fileName.lastIndexOf(".");
      if (lastDot == -1) {
         return fileName + "." + extension;
      } else {
         String fileNoExt = fileName.substring(0, lastDot);
         return fileNoExt + "." + extension;   
      }           

   }
   
   private static String kUnknownError = "An error occurred while loading citation data for this DOI.";
   private static String kNoDataError = "Citation data for this DOI couldn't be found.";
   private static String kServerError = "Unable to reach server to load citation data for this DOI.";
   

   private Widget mainWidget_;
   private PanmirrorDOIServerOperations server_;
   private UserState userState_;
   private boolean canceled_;
   private PanmirrorInsertCiteProps citeProps_;

   private static PanmirrorDialogsResources RES = PanmirrorDialogsResources.INSTANCE;

}
