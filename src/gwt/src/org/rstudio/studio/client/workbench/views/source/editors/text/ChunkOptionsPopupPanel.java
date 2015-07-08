/*
 * ChunkOptionsPopupPanel.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.NativeEventHandler;
import org.rstudio.core.client.widget.MiniPopupPanel;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.core.client.widget.TextBoxWithCue;
import org.rstudio.core.client.widget.ThemedCheckBox;
import org.rstudio.core.client.widget.TriStateCheckBox;
import org.rstudio.core.client.widget.TriStateCheckBox.State;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

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
   protected abstract void initOptions(Command afterInit);
   protected abstract void synchronize();
   protected abstract void revert();
   
   public ChunkOptionsPopupPanel(boolean includeChunkNameUI)
   {
      super(true);
      setVisible(false);
      
      chunkOptions_ = new HashMap<String, String>();
      originalChunkOptions_ = new HashMap<String, String>();
      
      panel_ = new VerticalPanel();
      add(panel_);
      
      header_ = new Label();
      header_.addStyleName(RES.styles().headerLabel());
      header_.setVisible(false);
      panel_.add(header_);
      
      tbChunkLabel_ = new TextBoxWithCue("Unnamed chunk");
      tbChunkLabel_.addStyleName(RES.styles().textBox());
      tbChunkLabel_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            synchronize();
         }
      });
      
      panel_.addHandler(new KeyUpHandler()
      {
         @Override
         public void onKeyUp(KeyUpEvent event)
         {
            int keyCode = event.getNativeKeyCode();
            if (keyCode == KeyCodes.KEY_ESCAPE ||
                keyCode == KeyCodes.KEY_ENTER)
            {
               ChunkOptionsPopupPanel.this.hide();
               widget_.getEditor().focus();
               return;
            }
         }
      }, KeyUpEvent.getType());
      
      tbChunkLabel_.addKeyUpHandler(new KeyUpHandler()
      {
         @Override
         public void onKeyUp(KeyUpEvent event)
         {
            int keyCode = event.getNativeKeyCode();
            if (keyCode == KeyCodes.KEY_ESCAPE ||
                keyCode == KeyCodes.KEY_ENTER)
            {
               ChunkOptionsPopupPanel.this.hide();
               widget_.getEditor().focus();
               return;
            }
            
            synchronize();
            
         }
      });
      
      int gridRows = includeChunkNameUI ? 2 : 1;
      Grid nameAndOutputGrid = new Grid(gridRows, 2);

      chunkLabel_ = new Label("Name:");
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
      
      outputComboBox_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            String value = outputComboBox_.getItemText(outputComboBox_.getSelectedIndex());
            if (value.equals(OUTPUT_USE_DOCUMENT_DEFAULT))
            {
               unset("echo");
               unset("eval");
               unset("include");
            }
            else if (value.equals(OUTPUT_SHOW_CODE_AND_OUTPUT))
            {
               set("echo", "TRUE");
               unset("eval");
               unset("include");
            }
            else if (value.equals(OUTPUT_SHOW_OUTPUT_ONLY))
            {
               set("echo", "FALSE");
               unset("eval");
               unset("include");
            }
            else if (value.equals(OUTPUT_SHOW_NOTHING))
            {
               unset("echo");
               unset("eval");
               set("include", "FALSE");
            }
            else if (value.equals(OUTPUT_SKIP_THIS_CHUNK))
            {
               set("eval", "FALSE");
               set("include", "FALSE");
               unset("echo");
            }
            synchronize();
         }
      });
      
      int row = includeChunkNameUI ? 1 : 0;
      nameAndOutputGrid.setWidget(row, 0, new Label("Output:"));
      nameAndOutputGrid.setWidget(row, 1, outputComboBox_);
      
      panel_.add(nameAndOutputGrid);
      
      panel_.add(verticalSpacer(4));
      
      showWarningsInOutputCb_ = makeTriStateCheckBox(
            "Show warnings",
            "warning");
      panel_.add(showWarningsInOutputCb_);
      
      panel_.add(verticalSpacer(6));
      
      showMessagesInOutputCb_ = makeTriStateCheckBox(
            "Show messages",
            "message");
      panel_.add(showMessagesInOutputCb_);
      
      panel_.add(verticalSpacer(6));
      
      useCustomFigureCheckbox_ = new ThemedCheckBox("Use custom figure size");
      useCustomFigureCheckbox_.addStyleName(RES.styles().checkBox());
      useCustomFigureCheckbox_.addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            boolean value = event.getValue();
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
      
      figWidthBox_ = makeInputBox("fig.width");
      Label widthLabel = new Label("Width (inches):");
      widthLabel.getElement().getStyle().setMarginLeft(20, Unit.PX);
      figureDimensionsPanel_.setWidget(0, 0, widthLabel);
      figureDimensionsPanel_.setWidget(0, 1, figWidthBox_);
      
      figHeightBox_ = makeInputBox("fig.height");
      Label heightLabel = new Label("Height (inches):");
      heightLabel.getElement().getStyle().setMarginLeft(20, Unit.PX);
      figureDimensionsPanel_.setWidget(1, 0, heightLabel);
      figureDimensionsPanel_.setWidget(1, 1, figHeightBox_);
      
      panel_.add(figureDimensionsPanel_);
      
      panel_.add(verticalSpacer(8));
      
      HorizontalPanel footerPanel = new HorizontalPanel();
      footerPanel.getElement().getStyle().setWidth(100, Unit.PCT);
      
      FlowPanel linkPanel = new FlowPanel();
      HelpLink helpLink = new HelpLink("Chunk options", "chunk-options", false);
      linkPanel.add(helpLink);
      
      HorizontalPanel buttonPanel = new HorizontalPanel();
      buttonPanel.addStyleName(RES.styles().buttonPanel());
      buttonPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
      
      SmallButton revertButton = new SmallButton("Revert");
      revertButton.getElement().getStyle().setMarginRight(8, Unit.PX);
      revertButton.addClickHandler(new ClickHandler()
      {
         
         @Override
         public void onClick(ClickEvent event)
         {
            revert();
            hideAndFocusEditor();
         }
      });
      buttonPanel.add(revertButton);
      
      SmallButton applyButton = new SmallButton("Apply");
      applyButton.addClickHandler(new ClickHandler()
      {
         
         @Override
         public void onClick(ClickEvent event)
         {
            synchronize();
            hideAndFocusEditor();
         }
      });
      buttonPanel.add(applyButton);
      
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
   
   private TextBox makeInputBox(final String option)
   {
      final TextBox box = new TextBox();
      box.getElement().setAttribute("placeholder", "Default");
      box.setWidth("40px");
      
      DomUtils.addKeyHandlers(box, new NativeEventHandler()
      {
         @Override
         public void onNativeEvent(NativeEvent event)
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  String text = box.getText().trim();
                  if (StringUtil.isNullOrEmpty(text))
                     unset(option);
                  else
                     set(option, text);
                  synchronize();
               }
            });
         }
      });
      
      return box;
   }
   
   private TriStateCheckBox makeTriStateCheckBox(String label, final String option)
   {
      TriStateCheckBox checkBox = new TriStateCheckBox(label);
      checkBox.addValueChangeHandler(
            new ValueChangeHandler<TriStateCheckBox.State>()
            {
               @Override
               public void onValueChange(ValueChangeEvent<State> event)
               {
                  State state = event.getValue();
                  if (state == TriStateCheckBox.STATE_INDETERMINATE)
                     unset(option);
                  else if (state == TriStateCheckBox.STATE_OFF)
                     set(option, "FALSE");
                  else if (state == TriStateCheckBox.STATE_ON)
                     set(option, "TRUE");
                  synchronize();
               }
            });
      return checkBox;
   }
   
   protected boolean has(String key)
   {
      return chunkOptions_.containsKey(key);
   }
   
   protected String get(String key)
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
   
   protected void set(String key, String value)
   {
      chunkOptions_.put(key,  value);
   }
   
   protected void unset(String key)
   {
      chunkOptions_.remove(key);
   }
   
   protected boolean select(String option)
   {
      for (int i = 0; i < outputComboBox_.getItemCount(); i++)
      {
         if (outputComboBox_.getItemText(i).equals(option))
         {
            outputComboBox_.setSelectedIndex(i);
            return true;
         }
      }
      
      return false;
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
   
   public void init(AceEditorWidget widget, Position position)
   {
      widget_ = widget;
      position_ = position;
      chunkOptions_.clear();
      originalChunkOptions_.clear();
      
      useCustomFigureCheckbox_.setValue(false);
      figureDimensionsPanel_.setVisible(false);
            
      Command afterInit = new Command()
      {
         @Override
         public void execute()
         {
            updateOutputComboBox();
            boolean hasRelevantFigureSettings =
                  has("fig.width") ||
                  has("fig.height");

            useCustomFigureCheckbox_.setValue(hasRelevantFigureSettings);
            if (hasRelevantFigureSettings)
               useCustomFigureCheckbox_.setVisible(true);
            figureDimensionsPanel_.setVisible(hasRelevantFigureSettings);

            if (has("fig.width"))
               figWidthBox_.setText(get("fig.width"));
            else
               figWidthBox_.setText("");

            if (has("fig.height"))
               figHeightBox_.setText(get("fig.height"));
            else
               figHeightBox_.setText("");

            if (has("warning"))
               showWarningsInOutputCb_.setValue(getBoolean("warning"));

            if (has("message"))
               showMessagesInOutputCb_.setValue(getBoolean("message"));
            
            setVisible(true);
         }
      };
      
      initOptions(afterInit);
      
   }
   
   private boolean isTrue(String string)
   {
      return string.equals("TRUE") || string.equals("T");
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
      widget_.getEditor().focus();
   }
   
   private FlowPanel verticalSpacer(int sizeInPixels)
   {
      FlowPanel panel = new FlowPanel();
      panel.setWidth("100%");
      panel.setHeight("" + sizeInPixels + "px");
      return panel;
   }
   
   private int getPriority(String key)
   {
      if (key.equals("eval"))
         return 10;
      else if (key.equals("echo"))
         return 9;
      else if (key.equals("warning") || key.equals("error") || key.equals("message"))
         return 8;
      else if (key.startsWith("fig."))
         return 8;
      return 0;
   }
   
   protected Map<String, String> sortedOptions(Map<String, String> options)
   {
      List<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>(options.entrySet());

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

      LinkedHashMap<String, String> sortedMap = new LinkedHashMap<String, String>();
      for (Map.Entry<String, String> entry : entries) {
         sortedMap.put(entry.getKey(), entry.getValue());
      }
      return sortedMap;
   }
   
   protected final VerticalPanel panel_;
   protected final Label header_;
   protected final Label chunkLabel_;
   protected final TextBoxWithCue tbChunkLabel_;
   protected final ListBox outputComboBox_;
   protected final Grid figureDimensionsPanel_;
   protected final TextBox figWidthBox_;
   protected final TextBox figHeightBox_;
   protected final ThemedCheckBox useCustomFigureCheckbox_;
   protected final TriStateCheckBox showWarningsInOutputCb_;
   protected final TriStateCheckBox showMessagesInOutputCb_;
   
   protected String originalLine_;
   protected String chunkPreamble_;
   
   protected HashMap<String, String> chunkOptions_;
   protected HashMap<String, String> originalChunkOptions_;
   
   protected AceEditorWidget widget_;
   protected Position position_;
   
   private static final String OUTPUT_USE_DOCUMENT_DEFAULT =
         "(Use document default)";

   private static final String OUTPUT_SHOW_OUTPUT_ONLY =
         "Show output only";
   
   private static final String OUTPUT_SHOW_CODE_AND_OUTPUT =
         "Show code and output";
   
   private static final String OUTPUT_SHOW_NOTHING =
         "Show nothing (run code)";
   
   private static final String OUTPUT_SKIP_THIS_CHUNK =
         "Show nothing (don't run code)";
   
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
}
