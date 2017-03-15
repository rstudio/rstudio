/*
 * DataImport.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.environment.dataimport;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.DomMetrics;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.GridViewerFrame;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressIndicatorDelay;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.reditor.EditorLanguage;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.views.environment.dataimport.model.DataImportAssembleResponse;
import org.rstudio.studio.client.workbench.views.environment.dataimport.model.DataImportPreviewResponse;
import org.rstudio.studio.client.workbench.views.environment.dataimport.model.DataImportServerOperations;
import org.rstudio.studio.client.workbench.views.environment.dataimport.res.DataImportResources;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditorWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DataImport extends Composite
{
   private static DataImportUiBinder uiBinder = GWT
         .create(DataImportUiBinder.class);
   
   private DataImportServerOperations server_;
   private GlobalDisplay globalDisplay_;
   
   private final int maxRows_ = 50;
   private final int maxCols_ = 5000;
   private final int maxFactors_ = 64;
   
   private ProgressIndicatorDelay progressIndicator_;
   private DataImportOptionsUi dataImportOptionsUi_;
   private DataImportResources dataImportResources_;
   private String codePreview_;
   
   private final int minWidth = 680;
   private final int minHeight = 400;
   
   private final String codePreviewErrorMessage_ = "Code Creation Error";
   
   private DataImportOptions importOptions_;
   
   private Integer zIndex_;
   
   private DataImportColumnTypesMenu columnTypesMenu_;
   
   private DataImportPreviewResponse lastSuccessfulResponse_;
   
   private final DataImportModes dataImportMode_;
   
   private JavaScriptObject localFiles_;
   
   interface DataImportUiBinder extends UiBinder<Widget, DataImport>
   {
   }

   public <T> DataImport(DataImportModes dataImportMode,
                         ProgressIndicator progressIndicator,
                         final String path)
   {
      dataImportResources_ = GWT.create(DataImportResources.class);
      dataImportMode_ = dataImportMode;
      
      copyButton_ = makeCopyButton();

      progressIndicator_ = new ProgressIndicatorDelay(progressIndicator);
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      initWidget(uiBinder.createAndBindUi(this));
      
      Size size = DomMetrics.adjustedElementSizeToDefaultMax();
      setSize(Math.max(minWidth, size.width) + "px", Math.max(minHeight, size.height) + "px");
      
      setCodeAreaDefaults();
      
      columnTypesMenu_ = new DataImportColumnTypesMenu();
      
      dataImportOptionsUi_ = getOptionsUiForMode(dataImportMode);
      importOptions_ = getOptions();
      
      optionsHost_.add(dataImportOptionsUi_);
      
      dataImportOptionsUi_.addValueChangeHandler(new ValueChangeHandler<DataImportOptions>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<DataImportOptions> dataImportOptions)
         {
            previewDataImport();
            
            if (dataImportMode_ == DataImportModes.XLS)
            {
               resetColumnDefinitions();
            }
         }
      });
      
      assembleDataImport(null);
      
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         public void execute()
         {
            dataImportFileChooser_.setFocus();
            
            if (!path.isEmpty()) {
               dataImportFileChooser_.locationTextBox_.setValue(path);
               dataImportFileChooser_.switchToUpdateMode(true);
               onFileUpdated();
            }
         }
      });
   }
   
   public String getCode()
   {
      return codeArea_.getEditor().getSession().getValue();
   }
   
   public void setZIndex(Integer zIndex)
   {
      zIndex_ = zIndex;
   }
   
   private DataImportOptionsUi getOptionsUiForMode(DataImportModes mode)
   {
      switch (mode)
      {
      case Text:
         return new DataImportOptionsUiCsv();
      case SAV:
      case SAS:
      case Stata:
         return new DataImportOptionsUiSav(mode);
      case XLS:
         return new DataImportOptionsUiXls();
      case XML:
         return new DataImportOptionsUiXml();
      case JSON:
         return new DataImportOptionsUiJson();
      case ODBC:
         return new DataImportOptionsUiOdbc();
      case JDBC:
         return new DataImportOptionsUiJdbc();
      case Mongo:
         return new DataImportOptionsUiMongo();
      }
      
      return null;
   }
   
   private String enhancePreviewErrorMessage(String error)
   {
      String headerMessage = "";
      
      switch (dataImportMode_)
      {
      case Text:
         headerMessage = "Is this a valid CSV file?\n\n";
         break;
      case SAV:
      case SAS:
      case Stata:
         headerMessage =  "Is this a valid SPSS, SAS or STATA file?\n\n";
         break;
      case XLS:
         headerMessage =  "Is this a valid Excel file?\n\n";
         break;
      default:
         break;
      }
      
      return headerMessage + error;
   }
   
   @Inject
   private void initialize(WorkbenchServerOperations server,
                           GlobalDisplay globalDisplay)
   {
      server_ = server;
      globalDisplay_ = globalDisplay;
   }
   
   void onFileUpdated()
   {
      // Invalidate cached files, click update to refresh stale files
      cleanPreviewResources();
      
      if (dataImportFileChooser_.getText() != importOptions_.getImportLocation())
      {
         lastSuccessfulResponse_ = null;
         resetColumnDefinitions();
      }
      
      importOptions_.setImportLocation(
         !dataImportFileChooser_.getText().isEmpty() ?
               dataImportFileChooser_.getText() :
         null);
      dataImportOptionsUi_.clearOptions();
      dataImportOptionsUi_.setImportLocation(dataImportFileChooser_.getText());
      previewDataImport();
   }
   
   @UiFactory
   DataImportFileChooser makeFileOrUrlChooserTextBox() {
      DataImportFileChooser dataImportFileChooser = new DataImportFileChooser(
            new Operation()
            {
               @Override
               public void execute()
               {
                  onFileUpdated();
               }
            },
            true);
         
      return dataImportFileChooser;
   }
   
   PushButton makeCopyButton()
   {
      return new PushButton(new Image(new ImageResource2x(
         dataImportResources_.copyImage(),
         dataImportResources_.copyImage2x())), new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent arg0)
         {
            DomUtils.copyCodeToClipboard(codePreview_);
         }
      });
   }
   
   void resetColumnDefinitions()
   {
      importOptions_.resetColumnDefinitions();
   }
   
   @UiField
   DataImportFileChooser dataImportFileChooser_;
   
   @UiField
   GridViewerFrame gridViewer_;
   
   @UiField
   HTMLPanel optionsHost_;
   
   @UiField
   AceEditorWidget codeArea_;
   
   @UiField(provided=true)
   PushButton copyButton_;
   
   private void promptForParseString(
      String title,
      String parseString,
      final Operation complete,
      final String columnName,
      final String columnType)
   {
      globalDisplay_.promptForText(
         title,
         "Please enter the format string",
         parseString,
         new OperationWithInput<String>()
         {
            @Override
            public void execute(final String formatString)
            {
               importOptions_.setColumnDefinition(columnName, columnType, formatString);
               complete.execute();
            }
         });
   }

   private void promptForFactorString(
      String title,
      final Operation complete,
      final String columnName,
      final String columnType)
   {
      globalDisplay_.promptForText(
         title,
         "Please insert a comma separated list of factors",
         "",
         new OperationWithInput<String>()
         {
            @Override
            public void execute(final String formatString)
            {
               String[] parts = formatString.split(",");

               String factorsString = "";
               boolean first = true;
               for (String part : parts)
               {
                  part = part.replaceAll("^\\s+|\\s+$", "");

                  if (!first) factorsString = factorsString + ", ";
                  factorsString = factorsString + "\"" + part + "\"";
                  first = false;
               }

               importOptions_.setColumnDefinition(columnName, columnType, "c(" + factorsString + ")");
               complete.execute();
            }
         });
   }
   
   private Operation onColumnMenuShow(final DataImportPreviewResponse response)
   {
      final Operation completeAndPreview = new Operation()
      {
         @Override
         public void execute()
         {
            previewDataImport();
         }
      };
      
      return new Operation()
      {
         @Override
         public void execute()
         {
            final DataImportDataActiveColumn column = gridViewer_.getActiveColumn();
            
            columnTypesMenu_.setOnChange(new OperationWithInput<String>()
            {
               @Override
               public void execute(final String input)
               {
                  columnTypesMenu_.hide();
                  
                  if (input == "guess")
                  {
                     importOptions_.setColumnType(column.getName(), null);
                     completeAndPreview.execute();
                  }
                  else if (input == "date")
                  {
                     if (dataImportMode_ == DataImportModes.Text)
                     {
                        promptForParseString(
                              "Date Format", "%m/%d/%Y", completeAndPreview, column.getName(), input);
                     }
                     else
                     {
                        importOptions_.setColumnType(column.getName(), input);
                        completeAndPreview.execute();
                     }
                  }
                  else if (input == "time")
                  {
                     promptForParseString(
                        "Time Format", "%H:%M", completeAndPreview, column.getName(), input);
                  }
                  else if (input == "dateTime")
                  {
                     promptForParseString(
                        "Date and Time Format", "%m/%d/%Y %H:%M", completeAndPreview, column.getName(), input);
                  }
                  else if (input == "factor")
                  {
                     promptForFactorString(
                        "Factors", completeAndPreview, column.getName(), input);
                  }
                  else
                  {
                     importOptions_.setColumnType(column.getName(), input);
                     completeAndPreview.execute();
                  }
               }
            }, new OperationWithInput<String>()
            {
               @Override
               public void execute(String input)
               {
                  if (input == "include") {
                     importOptions_.setOnlyColumn(column.getName(), false);
                     if (importOptions_.getColumnType(column.getName()) == "skip") {
                        importOptions_.setColumnType(column.getName(), null);
                     }
                  }
                  
                  if (input == "only") {
                     importOptions_.setOnlyColumn(column.getName(), true);
                     if (importOptions_.getColumnType(column.getName()) == "skip") {
                        importOptions_.setColumnType(column.getName(), null);
                     }
                  }
                  
                  if (input == "skip") {
                     importOptions_.setOnlyColumn(column.getName(), false);
                     importOptions_.setColumnType(column.getName(), "skip");
                  }
                  
                  columnTypesMenu_.hide();
                  previewDataImport();
               }
            });
            
            columnTypesMenu_.setPopupPosition(
                  gridViewer_.getAbsoluteLeft() + column.getLeft(),
                  gridViewer_.getAbsoluteTop() + column.getTop());
            
            columnTypesMenu_.setSize(column.getWidth() + "px", "");
            
            if (zIndex_ != null)
            {
               columnTypesMenu_.getElement().getStyle().setZIndex(zIndex_);
            }
            
            boolean columnOnly = importOptions_.getColumnOnly(column.getName());
            String columnType = importOptions_.getColumnType(column.getName());
            
            columnTypesMenu_.resetSelected();
            columnTypesMenu_.setSelected(columnType != null ? columnType : "guess");
            if (columnOnly)
            {
               columnTypesMenu_.setSelected("only");
            }
            
            columnTypesMenu_.setVisibleColumns(response.getSupportedColumnTypes());
            
            if (someColumnsHaveNoName(lastSuccessfulResponse_)) {
               columnTypesMenu_.setError(
                     "All columns must have names in order to " +
                     "perform column operations.");
               columnTypesMenu_.setWidth("200px");
            }
            
            columnTypesMenu_.show();
         }
      };
   }
   
   public DataImportOptions getOptions()
   {
      DataImportOptions options = dataImportOptionsUi_.getOptions();
      
      if (importOptions_ != null)
      {
         options.setOptions(importOptions_);
      }
      
      if (localFiles_ != null)
      {
         options.setLocalFiles(localFiles_);
      }
      
      return options;
   }
   
   @Override
   public void onDetach()
   {
      cleanPreviewResources();
      super.onDetach();
   }
   
   private void cleanPreviewResources()
   {
      if (localFiles_ != null)
      {
         server_.previewDataImportClean(getOptions(), new ServerRequestCallback<Void>()
         {
            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
            }
         });
      }
      
      localFiles_ = null;
   }
   
   private void setGridViewerData(DataImportPreviewResponse response)
   {
      gridViewer_.setOption("nullsAsNAs", "true");
      gridViewer_.setOption("ordering", "false");
      gridViewer_.setOption("rowNumbers", "false");
      gridViewer_.setData(response);
      
      if (response.getSupportedColumnTypes() != null && response.getSupportedColumnTypes().length > 0)
      {
         gridViewer_.setColumnDefinitionsUIVisible(true, onColumnMenuShow(response), new Operation()
         {
            @Override
            public void execute()
            {
               columnTypesMenu_.hide();
            }
         });
      }
   }
   
   private void previewDataImport()
   {
      Operation previewDataImportOperation = new Operation()
      {
         @Override
         public void execute()
         {
            DataImportOptions previewImportOptions = getOptions();
            
            if (dataImportFileChooser_.getText() == "")
            {
               gridViewer_.setData(null);
               return;
            }
            
            previewImportOptions.setMaxRows(maxRows_);
            
            progressIndicator_.onProgress("Retrieving preview data...", new Operation()
            {
               @Override
               public void execute()
               {
                  progressIndicator_.clearProgress();
                  cleanPreviewResources();
                  
                  server_.previewDataImportAsyncAbort(new ServerRequestCallback<Void>()
                  {
                     @Override
                     public void onResponseReceived(Void empty)
                     {
                     }
                     
                     @Override
                     public void onError(ServerError error)
                     {
                        Debug.logError(error);
                        progressIndicator_.onError(error.getMessage());
                     }
                  });
               }
            });
            
            server_.previewDataImportAsync(previewImportOptions, maxCols_, maxFactors_,
                  new ServerRequestCallback<DataImportPreviewResponse>()
            {
               @Override
               public void onResponseReceived(DataImportPreviewResponse response)
               {
                  if (response == null || response.getErrorMessage() != null)
                  {
                     if (response != null)
                     {
                        setGridViewerData(response);
                        response.setColumnDefinitions(lastSuccessfulResponse_);
                        progressIndicator_.onError(
                              enhancePreviewErrorMessage(response.getErrorMessage())
                        );
                     }
                     return;
                  }
                  
                  // Set the column definitions to allow subsequent calls to assemble
                  // generate preview code based on data.
                  importOptions_.setBaseColumnDefinitions(response);
                  
                  lastSuccessfulResponse_ = response;
                  
                  dataImportOptionsUi_.setPreviewResponse(response);

                  if (response.getLocalFiles() != null)
                  {
                     localFiles_ = response.getLocalFiles();
                  }
                  
                  gridViewer_.setOption("status",
                        "Previewing first " + toLocaleString(maxRows_) + 
                        " entries. " + (
                              response.getParsingErrors() > 0 ?
                              Integer.toString(response.getParsingErrors()) + " parsing errors." : "")
                        );
                  
                  assignColumnDefinitions(response, importOptions_.getColumnDefinitions());
                  
                  setGridViewerData(response);
                  
                  progressIndicator_.onCompleted();
               }
               
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
                  cleanPreviewResources();
                  gridViewer_.setData(null);
                  progressIndicator_.onError(error.getMessage());
               }
            });
         }
      };
      
      assembleDataImport(previewDataImportOperation);
   }
   
   private void setCodeAreaDefaults()
   {
      codeArea_.getEditor().getSession().setEditorMode(
            EditorLanguage.LANG_R.getParserName(), false);
      codeArea_.getEditor().getSession().setUseWrapMode(true);
      codeArea_.getEditor().getSession().setWrapLimitRange(20, 120);
      codeArea_.getEditor().getRenderer().setShowGutter(false);
   }
   
   private void assembleDataImport(final Operation onComplete)
   {
      server_.assembleDataImport(getOptions(),
            new ServerRequestCallback<DataImportAssembleResponse>()
      {
         @Override
         public void onResponseReceived(DataImportAssembleResponse response)
         {
            if (response.getErrorMessage() != null)
            {
               progressIndicator_.onError(response.getErrorMessage());
               return;
            }
            
            codePreview_ = response.getImportCode();
            dataImportOptionsUi_.setAssembleResponse(response);
            if (response.getLocalFiles() != null)
            {
               localFiles_ = response.getLocalFiles();
            }
            
            setCodeAreaDefaults();
            codeArea_.setCode(codePreview_);
            
            if (onComplete != null)
            {
               onComplete.execute();
            }
         }
         
         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
            globalDisplay_.showErrorMessage(codePreviewErrorMessage_, error.getMessage());
         }
      });
   }
   
   private final native String toLocaleString(int number) /*-{
      return number.toLocaleString ? number.toLocaleString() : number;
   }-*/;
   
   public final native void assignColumnDefinitions(
      JavaScriptObject response, 
      JavaScriptObject definitions) /*-{
      if (!definitions)
         return;
         
      var hasOnlyColumns = Object.keys(definitions).some(function(key) {
         return definitions[key].only;
      });
         
      Object.keys(response.columns).forEach(function(key) {
         var col = response.columns[key];
         if (definitions[col.col_name]) {
            col.col_type_assigned = definitions[col.col_name].assignedType;
            if (col.col_type_assigned == "skip")
            {
               col.col_disabled = true;
            }
         }
         if (hasOnlyColumns) {
            col.col_disabled = !definitions[col.col_name] || !definitions[col.col_name].only;
         }
      });
   }-*/;
   
   public final native boolean someColumnsHaveNoName(JavaScriptObject response) /*-{   
      if (!response.columns)
         return false;
      
      return response.columns.some(function(column) {
         return !column.col_name && column.col_type != 'rownames';
      });
   }-*/;
}
