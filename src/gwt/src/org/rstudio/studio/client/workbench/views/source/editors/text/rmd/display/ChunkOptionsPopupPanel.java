/*
 * ChunkOptionsPopupPanel.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.NativeEventHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.LayoutGrid;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.MiniPopupPanel;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.core.client.widget.TextBoxWithCue;
import org.rstudio.core.client.widget.Toggle;
import org.rstudio.core.client.widget.VerticalSpacer;
import org.rstudio.core.client.widget.Toggle.State;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.views.source.ViewsSourceConstants;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display.ChunkOptionValue.OptionLocation;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public abstract class ChunkOptionsPopupPanel extends MiniPopupPanel
{
   // Sub-classes must implement these methods.
   //
   // initOptions should fill the 'chunkOptions_' map and call 'afterInit'
   // after this has completed.
   //
   // synchronize should modify the document to reflect the current state
   // of the UI selection.
   //
   // revert should return the document state to how it was before editing
   // was initiated.
   protected abstract void initOptions(CommandWithArg<Boolean> afterInit);
   protected abstract void synchronize();
   protected abstract void revert();
   private static final ViewsSourceConstants constants_ = GWT.create(ViewsSourceConstants.class);
   @Inject
   private void initialize(FileDialogs fileDialogs,
                           RemoteFileSystemContext rfsContext,
                           GlobalDisplay globalDisplay)
   {
      fileDialogs_ = fileDialogs;
      rfsContext_ = rfsContext;
      globalDisplay_ = globalDisplay;
   }
   
   public ChunkOptionsPopupPanel(boolean includeChunkNameUI, OptionLocation preferredOptionLocation, boolean isVisualEditor)
   {
      super(true);
      setVisible(false);
      
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      preferredOptionLocation_ = preferredOptionLocation;
      isVisualEditor_ = isVisualEditor;
      chunkOptions_ = new HashMap<>();
      
      panel_ = new VerticalPanel();
      add(panel_);
      
      header_ = new Label();
      header_.addStyleName(RES.styles().headerLabel());
      header_.setVisible(false);
      panel_.add(header_);
      
      tbChunkLabel_ = new TextBoxWithCue(constants_.unnamedChunk());
      tbChunkLabel_.addStyleName(RES.styles().textBox());
      tbChunkLabel_.addChangeHandler(changeEvent -> synchronize());
      tbChunkLabel_.setElementId(ElementIds.getElementId(ElementIds.CHUNK_OPTIONS_NAME));
      
      panel_.addHandler(keyUpEvent ->
      {
         int keyCode = keyUpEvent.getNativeKeyCode();
         if (keyCode == KeyCodes.KEY_ESCAPE ||
             keyCode == KeyCodes.KEY_ENTER)
         {
            ChunkOptionsPopupPanel.this.hide();
            display_.focus();
            return;
         }
      }, KeyUpEvent.getType());
      
      tbChunkLabel_.addKeyUpHandler(keyUpEvent ->
      {
         int keyCode = keyUpEvent.getNativeKeyCode();
         if (keyCode == KeyCodes.KEY_ESCAPE ||
             keyCode == KeyCodes.KEY_ENTER)
         {
            ChunkOptionsPopupPanel.this.hide();
            display_.focus();
            return;
         }
         
         synchronize();
         
      });
      
      int gridRows = includeChunkNameUI ? 2 : 1;
      LayoutGrid nameAndOutputGrid = new LayoutGrid(gridRows, 2);

      chunkLabel_ = new FormLabel(constants_.chunkNameColon(), tbChunkLabel_);
      chunkLabel_.addStyleName(RES.styles().chunkLabel());
      
      if (includeChunkNameUI)
         nameAndOutputGrid.setWidget(0, 0, chunkLabel_);

      tbChunkLabel_.addStyleName(RES.styles().chunkName());
      
      if (includeChunkNameUI)
         nameAndOutputGrid.setWidget(0, 1, tbChunkLabel_);
      
      outputComboBox_ = new ListBox();
      String[] options = new String[] {
            OUTPUT_USE_DOCUMENT_DEFAULT,
            OUTPUT_SHOW_OUTPUT_ONLY,
            OUTPUT_SHOW_CODE_AND_OUTPUT,
            OUTPUT_SHOW_NOTHING,
            OUTPUT_SKIP_THIS_CHUNK
      };
      
      for (String option : options)
         outputComboBox_.addItem(option);
      
      outputComboBox_.addChangeHandler(changeEvent ->
      {
         String value = outputComboBox_.getItemText(outputComboBox_.getSelectedIndex());
         if (value == OUTPUT_USE_DOCUMENT_DEFAULT)
         {
            unset("echo");
            unset("eval");
            unset("include");
         }
         else if (value == OUTPUT_SHOW_CODE_AND_OUTPUT)
         {
            setTrue("echo");
            unset("eval");
            unset("include");
         }
         else if (value == OUTPUT_SHOW_OUTPUT_ONLY)
         {
            setFalse("echo");
            unset("eval");
            unset("include");
         }
         else if (value == OUTPUT_SHOW_NOTHING)
         {
            unset("echo");
            unset("eval");
            setFalse("include");
         }
         else if (value == OUTPUT_SKIP_THIS_CHUNK)
         {
            setFalse("eval");
            setFalse("include");
            unset("echo");
         }
         synchronize();
      });
      
      int row = includeChunkNameUI ? 1 : 0;
      FormLabel outputLabel = new FormLabel(constants_.outputColon());
      nameAndOutputGrid.setWidget(row, 0, outputLabel);
      nameAndOutputGrid.setWidget(row, 1, outputComboBox_);
      ElementIds.assignElementId(outputComboBox_, ElementIds.CHUNK_OPTIONS_OUTPUT);
      outputLabel.setFor(outputComboBox_);
      
      panel_.add(nameAndOutputGrid);
      
      showWarningsInOutputCb_ = makeTriStateToggle(
            constants_.showWarnings(),
            "warning",
            ElementIds.CHUNK_OPTIONS_WARNINGS);
      panel_.add(showWarningsInOutputCb_);

      showMessagesInOutputCb_ = makeTriStateToggle(
            constants_.showMessages(),
            "message",
            ElementIds.CHUNK_OPTIONS_MESSAGES);
      panel_.add(showMessagesInOutputCb_);

      cacheChunkCb_ = makeTriStateToggle(
            constants_.cacheChunk(),
            "cache",
            ElementIds.CHUNK_OPTIONS_CACHE);
      panel_.add(cacheChunkCb_);
      cacheChunkCb_.setVisible(false);

      printTableAsTextCb_ = makeTriStateToggle(
            constants_.usePagedTables(),
            "paged.print",
            ElementIds.CHUNK_OPTIONS_TABLES);
      panel_.add(printTableAsTextCb_);
      printTableAsTextCb_.setVisible(false);
      
      useCustomFigureCheckbox_ = new Toggle(
            constants_.useCustomFigureSize(),
            false,
            ElementIds.CHUNK_OPTIONS_FIGURESIZE);
      useCustomFigureCheckbox_.addStyleName(RES.styles().checkBox());
      useCustomFigureCheckbox_.addValueChangeHandler(new ValueChangeHandler<Toggle.State>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Toggle.State> event)
         {
            boolean value = event.getValue() == Toggle.State.ON;
            figureDimensionsPanel_.setVisible(value);
            
            if (!value)
            {
               figWidthBox_.setText("");
               figHeightBox_.setText("");
               unset("fig.width");
               unset("fig.height");
               synchronize();
            }
         }
      });
      panel_.add(useCustomFigureCheckbox_);
      
      figureDimensionsPanel_ = new Grid(2, 2);
      figureDimensionsPanel_.getElement().getStyle().setMarginTop(5, Unit.PX);
      
      figWidthBox_ = makeInputBox("fig.width", false);
      FormLabel widthLabel = new FormLabel(constants_.widthInchesColon(), figWidthBox_);
      widthLabel.getElement().getStyle().setMarginLeft(20, Unit.PX);
      figureDimensionsPanel_.setWidget(0, 0, widthLabel);
      figureDimensionsPanel_.setWidget(0, 1, figWidthBox_);
      
      figHeightBox_ = makeInputBox("fig.height", false);
      FormLabel heightLabel = new FormLabel(constants_.heightInchesColon(), figHeightBox_);
      heightLabel.getElement().getStyle().setMarginLeft(20, Unit.PX);
      figureDimensionsPanel_.setWidget(1, 0, heightLabel);
      figureDimensionsPanel_.setWidget(1, 1, figHeightBox_);
      
      panel_.add(figureDimensionsPanel_);
      
      enginePanel_ = new Grid(2, 3);
      enginePanel_.getElement().getStyle().setMarginTop(5, Unit.PX);
      
      enginePathBox_ = makeInputBox("engine.path", true);
      enginePathBox_.getElement().getStyle().setWidth(120, Unit.PX);
      Label enginePathLabel = new Label(constants_.enginePathColon());
      SmallButton enginePathBrowseButton = new SmallButton("...");
      enginePathBrowseButton.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            // infer the start navigation directory
            String path = enginePathBox_.getValue();
            FileSystemItem initialPath = path.isEmpty()
                  ? FileSystemItem.createDir("~/")
                  : FileSystemItem.createDir(FilePathUtils.dirFromFile(path));
            
            fileDialogs_.openFile(
                  constants_.selectEngine(),
                  rfsContext_,
                  initialPath,
                  new ProgressOperationWithInput<FileSystemItem>()
                  {
                     @Override
                     public void execute(FileSystemItem input, ProgressIndicator indicator)
                     {
                        if (input == null)
                        {
                           indicator.onCompleted();
                           return;
                        }
                        
                        String path = StringUtil.notNull(input.getPath());
                        path = path.replaceAll("\\\\", "\\\\\\\\");
                        enginePathBox_.setValue(path);
                        set("engine.path", StringUtil.ensureQuoted(path));
                        synchronize();
                        indicator.onCompleted();
                     }
                  });
         }
      });
      enginePanel_.setWidget(0, 0, enginePathLabel);
      enginePanel_.setWidget(0, 1, enginePathBox_);
      enginePanel_.setWidget(0, 2, enginePathBrowseButton);
      
      engineOptsBox_ = makeInputBox("engine.opts", true);
      engineOptsBox_.getElement().getStyle().setWidth(120, Unit.PX);
      Label engineOptsLabel = new Label(constants_.engineOptionsColon());
      enginePanel_.setWidget(1, 0, engineOptsLabel);
      enginePanel_.setWidget(1, 1, engineOptsBox_);
      
      panel_.add(enginePanel_);
      
      HorizontalPanel footerPanel = new HorizontalPanel();
      footerPanel.getElement().getStyle().setWidth(100, Unit.PCT);
      
      FlowPanel linkPanel = new FlowPanel();
      linkPanel.add(new VerticalSpacer("8px"));
      HelpLink helpLink = new HelpLink(constants_.chunkOptions(), "chunk-options", false);
      linkPanel.add(helpLink);
      
      HorizontalPanel buttonPanel = new HorizontalPanel();
      buttonPanel.addStyleName(RES.styles().buttonPanel());
      buttonPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
      
      revertButton_ = new SmallButton(constants_.revertCapitalized(), ElementIds.CHUNK_OPTIONS_REVERT);
      revertButton_.getElement().getStyle().setMarginRight(8, Unit.PX);
      revertButton_.addClickHandler(clickEvent ->
      {
         revert();
         hideAndFocusEditor();
      });
      buttonPanel.add(revertButton_);
      
      applyButton_ = new SmallButton(constants_.applyCapitalized(), ElementIds.CHUNK_OPTIONS_APPLY);
      applyButton_.addClickHandler(clickEvent ->
      {
         synchronize();
         hideAndFocusEditor();
      });
      buttonPanel.add(applyButton_);
      
      footerPanel.setVerticalAlignment(VerticalPanel.ALIGN_BOTTOM);
      footerPanel.add(linkPanel);
      
      footerPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
      footerPanel.add(buttonPanel);
      
      panel_.add(footerPanel);
   }
   
   protected void setHeader(String text, boolean visible)
   {
      header_.setText(text);
      header_.setVisible(visible);
   }
   
   public void focus()
   {
      tbChunkLabel_.setFocus(true);
   }
   
   private TextBox makeInputBox(final String option, final boolean enquote)
   {
      final TextBox box = new TextBox();
      DomUtils.setPlaceholder(box, "Default");
      box.setWidth("40px");
      
      DomUtils.addKeyHandlers(box, new NativeEventHandler()
      {
         @Override
         public void onNativeEvent(NativeEvent nativeEvent)
         {
            Scheduler.get().scheduleDeferred(() ->
            {
               String text = box.getText().trim();
               boolean isEmpty = StringUtil.isNullOrEmpty(text);
               
               if (enquote && !isEmpty)
               {
                  text = StringUtil.ensureQuoted(text);
                  text = text.replaceAll("\\\\", "\\\\\\\\");
               }
               
               if (isEmpty)
                  unset(option);
               else
                  set(option, text);
               
               synchronize();
            });
         }
      });
      
      return box;
   }
   
   private Toggle makeTriStateToggle(String label, final String option, final String id)
   {
      Toggle toggle = new Toggle(label, true, id);
      toggle.addValueChangeHandler((ValueChangeEvent<State> event) -> {
         State state = event.getValue();
         switch (state)
         {
         case INDETERMINATE:
            unset(option);
            break;
         case OFF:
            setFalse(option);
            break;
         case ON:
            setTrue(option);
            break;
         }
         synchronize();
      });
      return toggle;
   }
   
   protected boolean has(String key)
   {
      return chunkOptions_.containsKey(key);
   }
   
   protected ChunkOptionValue get(String key)
   {
      if (!has(key))
         return null;
      
      return chunkOptions_.get(key);
   }
   
   protected boolean getBoolean(String key)
   {
      if (!has(key))
         return false;
      
      return isTrue(chunkOptions_.get(key));
   }

   /**
    * Set an option. If the option already exists it will be set in the same location,
    * otherwise it uses the default location for this document.
    */
   protected void set(String key, String value)
   {
      chunkOptions_.put(key, new ChunkOptionValue(value, locationForOption(key)));
   }

   /**
    * Set an option in a specific location.
    */
    protected void set(String key, String value, OptionLocation optionLocation)
   {
      chunkOptions_.put(key, new ChunkOptionValue(value, optionLocation));
   }

   /**
    * Set an option to the "TRUE" constant appropriate for its location (R vs. YAML)
    */
   protected void setTrue(String key)
   {
      chunkOptions_.put(key, new ChunkOptionValue(true, locationForOption(key)));
   }

   /**
    * Set an option to the "FALSE" constant appropriate for its location (R vs. YAML)
    */
   protected void setFalse(String key)
   {
      chunkOptions_.put(key, new ChunkOptionValue(false, locationForOption(key)));
   }

   protected void unset(String key)
   {
      chunkOptions_.remove(key);
   }
   
   protected boolean select(String option)
   {
      for (int i = 0; i < outputComboBox_.getItemCount(); i++)
      {
         if (outputComboBox_.getItemText(i) == option)
         {
            outputComboBox_.setSelectedIndex(i);
            return true;
         }
      }
      
      return false;
   }
   
   /**
    * Where to write a given option (same location if it already exists, otherwise the document's
    * preferred location).
    */
   protected OptionLocation locationForOption(String key)
   {
      return chunkOptions_.get(key) != null ? chunkOptions_.get(key).getLocation() : preferredOptionLocation_;
   }

   private void updateOutputComboBox()
   {
      boolean hasEcho = has("echo");
      boolean hasEval = has("eval");
      boolean hasIncl = has("include");
      
      boolean isEcho = hasEcho && getBoolean("echo");
      boolean isEval = hasEval && getBoolean("eval");
      boolean isIncl = hasIncl && getBoolean("include");
      
      if (hasEcho && !hasEval && !hasIncl)
         select(isEcho ? OUTPUT_SHOW_CODE_AND_OUTPUT : OUTPUT_SHOW_OUTPUT_ONLY);
     
      if (!hasEcho && !hasEval && hasIncl && !isIncl)
         select(OUTPUT_SHOW_NOTHING);
      
      if (!hasEcho && hasEval && !isEval && hasIncl && !isIncl)
         select(OUTPUT_SKIP_THIS_CHUNK);
   }
   
   public void init(DocDisplay display, Position position)
   {
      display_ = display;
      position_ = position;
      chunkOptions_.clear();
      
      useCustomFigureCheckbox_.setValue(false);
      figureDimensionsPanel_.setVisible(false);

      CommandWithArg<Boolean> afterInit = new CommandWithArg<Boolean>()
      {
         @Override
         public void execute(Boolean showUi)
         {
            if (!showUi)
            {
               globalDisplay_.showMessage(
                  MessageDialog.INFO,
                  constants_.unableToEditTitle(),
                  constants_.unableToEditMessage()); 
              return;
            }

            updateOutputComboBox();
            boolean hasRelevantFigureSettings =
                  has("fig.width") ||
                  has("fig.height");

            useCustomFigureCheckbox_.setValue(hasRelevantFigureSettings);
            if (hasRelevantFigureSettings)
               useCustomFigureCheckbox_.setVisible(true);
            figureDimensionsPanel_.setVisible(hasRelevantFigureSettings);

            if (has("fig.width"))
               figWidthBox_.setText(get("fig.width").getOptionValue());
            else
               figWidthBox_.setText("");

            if (has("fig.height"))
               figHeightBox_.setText(get("fig.height").getOptionValue());
            else
               figHeightBox_.setText("");

            if (has("warning"))
               showWarningsInOutputCb_.setValue(getBoolean("warning"));

            if (has("message"))
               showMessagesInOutputCb_.setValue(getBoolean("message"));

            if (has("paged.print"))
               printTableAsTextCb_.setValue(getBoolean("paged.print"));

            if (has("cache"))
               cacheChunkCb_.setValue(getBoolean("cache"));
            
            if (has("engine.path"))
            {
               String enginePath = StringUtil.stringValue(get("engine.path").getOptionValue());
               enginePath = enginePath.replaceAll("\\\\\\\\", "\\\\");
               enginePathBox_.setValue(enginePath);
            }
            
            if (has("engine.opts"))
            {
               String engineOpts = StringUtil.stringValue(get("engine.opts").getOptionValue());
               engineOpts = engineOpts.replaceAll("\\\\\\\\", "\\\\");
               engineOptsBox_.setValue(engineOpts);
            }
            
            setVisible(true);
         }
      };
      
      initOptions(afterInit);
      
   }
   
   private boolean isTrue(ChunkOptionValue option)
   {
      if (option.getLocation() == OptionLocation.FirstLine)
      {
         // R-style
         return StringUtil.equals(option.getOptionValue(), "TRUE") || 
                StringUtil.equals(option.getOptionValue(), "T");
      }
      else
      {
         // YAML-style: standard is "true" and "false" but also detect
         // other forms used in the wild
         String value = option.getOptionValue().toLowerCase();
         return StringUtil.equals(value, "true") || 
                StringUtil.equals(value, "yes") || 
                StringUtil.equals(value, "on");
      }
   }
   
   @Override
   public void hide()
   {
      position_ = null;
      chunkOptions_.clear();
      super.hide();
   }
   
   private void hideAndFocusEditor()
   {
      hide();
      display_.focus();
   }
   
   private int getPriority(String key)
   {
      if (StringUtil.equals(key, "label"))
         return 11;
      else if (StringUtil.equals(key, "eval"))
         return 10;
      else if (StringUtil.equals(key, "echo"))
         return 9;
      else if (StringUtil.equals(key, "warning") || 
               StringUtil.equals(key, "error") || 
               StringUtil.equals(key, "message"))
         return 8;
      else if (key.startsWith("fig."))
         return 8;
      return 0;
   }
   
   /**
    * Sort all options, regardless of location
    */
   protected Map<String, String> sortedOptions(Map<String, String> options)
   {
      List<Map.Entry<String, String>> entries = new ArrayList<>(options.entrySet());

      Collections.sort(entries, new Comparator<Map.Entry<String, String>>() {
         public int compare(Map.Entry<String, String> a, Map.Entry<String, String> b)
         {
            int lhsGroup = getPriority(a.getKey());
            int rhsGroup = getPriority(b.getKey());
            
            if (lhsGroup < rhsGroup)
               return 1;
            else if (lhsGroup > rhsGroup)
               return -1;
            
            return a.getKey().compareToIgnoreCase(b.getKey());
         }
      });

      LinkedHashMap<String, String> sortedMap = new LinkedHashMap<>();
      for (Map.Entry<String, String> entry : entries) {
         sortedMap.put(entry.getKey(), entry.getValue());
      }
      return sortedMap;
   }

   /**
    * Sort and denormalize names (e.g. fig.cap vs. fig-cap) of all options from a particular location.
    *
    * If there's a label= entry for the first-line, remove it (first line label has special handling)
    */
   protected Map<String, String> sortedOptionsDenormalized(Map<String, ChunkOptionValue> options, OptionLocation optionLocation)
   {
      return sortedOptions(filterOptionsMap(options, optionLocation, true, true));
   }

   /**
    * Get all options (unsorted and not denormalized) from a particular location.
    */
   protected Map<String, String> unsortedOptions(Map<String, ChunkOptionValue> options, OptionLocation optionLocation)
   {
      return filterOptionsMap(options, optionLocation, false, false);
   }

   /**
    * Get options from a particular location.

    * @param options full list of options
    * @param optionLocation only return options from this location
    * @param denormalize if true, adjust option names to match location's semantics (fig.cap for R,
    *                    fig-cap for YAML)
    * @param excludeLabel if true, don't return label=foo for this location
    * @return list of options
    */
   private Map<String, String> filterOptionsMap(Map<String, ChunkOptionValue> options,
                                                OptionLocation optionLocation,
                                                boolean denormalize,
                                                boolean excludeLabel)
   {
      Map<String, String> filteredEntries = new LinkedHashMap<>();
      for (Map.Entry<String, ChunkOptionValue> entry : options.entrySet()) {
         if (entry.getValue().getLocation() == optionLocation)
         {
            String key = entry.getKey();
            String value = entry.getValue().getOptionValue();

            if (StringUtil.equals("label", key))
            {
               if (excludeLabel && optionLocation == OptionLocation.FirstLine)
                  continue;
               
               if (StringUtil.isNullOrEmpty(value))
                  continue;
            }

            if (denormalize)
               key = ChunkOptionValue.denormalizeOptionName(key, optionLocation);

            filteredEntries.put(key, entry.getValue().getOptionValue());
         }
      }
      return filteredEntries;
   }

   /**
    * Get the label string to use for given location.
    * @param location
    * @return Label string or empty string if no label for this location
    */
   protected String getLabelForLocation(OptionLocation location)
   {
      ChunkOptionValue labelInfo = get("label");
      if (labelInfo == null || labelInfo.getLocation() != location)
         return "";
      return labelInfo.getOptionValue();
   }

    /**
     * Get value of label=FOO entry, if any.
     * @return value of label entry, or an empty string
     */
    protected String getLabelValue()
    {
      ChunkOptionValue labelInfo = get("label");
      if (labelInfo == null)
         return "";
      return labelInfo.getOptionValue();
    }

   protected final VerticalPanel panel_;
   protected final Label header_;
   protected final FormLabel chunkLabel_;
   protected final TextBoxWithCue tbChunkLabel_;
   protected final ListBox outputComboBox_;
   protected final Grid figureDimensionsPanel_;
   protected final TextBox figWidthBox_;
   protected final TextBox figHeightBox_;
   protected final Grid enginePanel_;
   protected final TextBox enginePathBox_;
   protected final TextBox engineOptsBox_;
   protected final SmallButton revertButton_;
   protected final SmallButton applyButton_;
   protected final Toggle useCustomFigureCheckbox_;
   protected final Toggle showWarningsInOutputCb_;
   protected final Toggle showMessagesInOutputCb_;
   protected final Toggle printTableAsTextCb_;
   protected final Toggle cacheChunkCb_;
   
   protected String originalFirstLine_; // opening chunk line, i.e. ```{r}
   protected String originalOptionLines_; // chunk option line(s) using #| prefix

   protected String chunkPreamble_;
   protected final boolean isVisualEditor_;
   
   protected final OptionLocation preferredOptionLocation_;
   protected HashMap<String, ChunkOptionValue> chunkOptions_;
   
   protected DocDisplay display_;
   protected Position position_;
   
   private static final String OUTPUT_USE_DOCUMENT_DEFAULT =
         constants_.useDocumentDefaultParentheses();

   private static final String OUTPUT_SHOW_OUTPUT_ONLY =
         constants_.showOutputOnly();
   
   private static final String OUTPUT_SHOW_CODE_AND_OUTPUT =
         constants_.showCodeAndOutput();
   
   private static final String OUTPUT_SHOW_NOTHING =
         constants_.showNothingRunCode();
   
   private static final String OUTPUT_SKIP_THIS_CHUNK =
         constants_.showNothingDontRunCode();
   
   public interface Styles extends CssResource
   {
      String headerLabel();
      
      String textBox();
      
      String chunkLabel();
      String chunkName();
      String labelPanel();
      
      String buttonPanel();
      
      String checkBox();
   }
   
   public interface Resources extends ClientBundle
   {
      @Source("ChunkOptionsPopupPanel.css")
      Styles styles();
   }
   
   private static Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }
   
   // Injected ----
   protected FileDialogs fileDialogs_;
   protected RemoteFileSystemContext rfsContext_;
   protected GlobalDisplay globalDisplay_;
}
