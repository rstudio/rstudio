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

import java.util.HashMap;
import java.util.Map;

import org.rstudio.core.client.Pair;
import org.rstudio.core.client.RegexUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.TextCursor;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.NativeEventHandler;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.MiniPopupPanel;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.core.client.widget.TextBoxWithCue;
import org.rstudio.core.client.widget.ThemedCheckBox;
import org.rstudio.core.client.widget.TriStateCheckBox;
import org.rstudio.core.client.widget.TriStateCheckBox.State;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

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
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ChunkOptionsPopupPanel extends MiniPopupPanel
{
   public ChunkOptionsPopupPanel()
   {
      super(true);
      
      chunkOptions_ = new HashMap<String, String>();
      originalChunkOptions_ = new HashMap<String, String>();
      
      panel_ = new VerticalPanel();
      add(panel_);
      
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
      
      HorizontalPanel labelPanel = new HorizontalPanel();
      labelPanel.addStyleName(RES.styles().labelPanel());
      labelPanel.setVerticalAlignment(VerticalPanel.ALIGN_MIDDLE);
      
      Label chunkLabel = new Label("Name:");
      chunkLabel.addStyleName(RES.styles().chunkLabel());
      labelPanel.add(chunkLabel);
      
      tbChunkLabel_.addStyleName(RES.styles().chunkName());
      labelPanel.add(tbChunkLabel_);
      
      panel_.add(labelPanel);
      
      outputComboBox_ = new SelectWidget(
            "Output:",
            new String[] {
                  OUTPUT_USE_DOCUMENT_DEFAULT,
                  OUTPUT_SHOW_CODE_AND_OUTPUT,
                  OUTPUT_SHOW_OUTPUT_ONLY,
                  OUTPUT_SHOW_NOTHING
            });
      
      outputComboBox_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            String value = outputComboBox_.getValue();
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
            synchronize();
         }
      });
      
      panel_.add(outputComboBox_);
      
      showWarningsInOutputCb_ = makeTriStateCheckBox(
            "Show warnings in output",
            "warning");
      panel_.add(showWarningsInOutputCb_);
      showMessagesInOutputCb_ = makeTriStateCheckBox(
            "Show messages in output",
            "message");
      panel_.add(showMessagesInOutputCb_);
      
      FlowPanel spacer = new FlowPanel();
      spacer.setWidth("100%");
      spacer.setHeight("5px");
      panel_.add(spacer);
      
      useCustomFigureCheckbox_ = new ThemedCheckBox("Use custom figure size");
      useCustomFigureCheckbox_.addStyleName(RES.styles().checkBox());
      useCustomFigureCheckbox_.addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            figureDimensionsPanel_.setVisible(event.getValue());
         }
      });
      panel_.add(useCustomFigureCheckbox_);
      
      figureDimensionsPanel_ = new Grid(2, 2);
      
      figWidthBox_ = makeInputBox("fig.width");
      figureDimensionsPanel_.setWidget(0, 0, new Label("Width (in inches):"));
      figureDimensionsPanel_.setWidget(0, 1, figWidthBox_);
      
      figHeightBox_ = makeInputBox("fig.height");
      figureDimensionsPanel_.setWidget(1, 0, new Label("Height (in inches):"));
      figureDimensionsPanel_.setWidget(1, 1, figHeightBox_);
      
      panel_.add(figureDimensionsPanel_);
      
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
      revertButton.getElement().getStyle().setMarginTop(-1, Unit.PX);
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
      applyButton.getElement().getStyle().setMarginTop(-1, Unit.PX);
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
   
   private boolean has(String key)
   {
      return chunkOptions_.containsKey(key);
   }
   
   public String get(String key)
   {
      return chunkOptions_.get(key);
   }
   
   private boolean getBoolean(String key)
   {
      return isTrue(chunkOptions_.get(key));
   }
   
   private void set(String key, String value)
   {
      chunkOptions_.put(key,  value);
   }
   
   private void unset(String key)
   {
      chunkOptions_.remove(key);
   }
   
   private void revert(String key)
   {
      if (originalChunkOptions_.containsKey(key))
         chunkOptions_.put(key, originalChunkOptions_.get(key));
      else
         chunkOptions_.remove(key);
   }
   
   public void init(AceEditorWidget widget, Position position)
   {
      widget_ = widget;
      position_ = position;
      chunkOptions_.clear();
      originalChunkOptions_.clear();
      
      originalLine_ = widget_.getEditor().getSession().getLine(position_.getRow());
      parseChunkHeader(originalLine_, originalChunkOptions_);
      for (Map.Entry<String, String> pair : originalChunkOptions_.entrySet())
         chunkOptions_.put(pair.getKey(), pair.getValue());
      
      boolean hasRelevantFigureSettings =
            has("fig.width") ||
            has("fig.height");
      
      useCustomFigureCheckbox_.setValue(hasRelevantFigureSettings);
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
   }
   
   private boolean isTrue(String string)
   {
      return string.equals("TRUE") || string.equals("T");
   }
   
   private String extractChunkPreamble(String extractedChunkHeader,
                                       String modeId)
   {
      if (modeId.equals("mode/sweave"))
         return "";
      
      int firstSpaceIdx = extractedChunkHeader.indexOf(' ');
      if (firstSpaceIdx == -1)
         return extractedChunkHeader;
      
      int firstCommaIdx = extractedChunkHeader.indexOf(',');
      if (firstCommaIdx == -1)
         firstCommaIdx = extractedChunkHeader.length();
      
      String label = extractedChunkHeader.substring(
            0, Math.min(firstSpaceIdx, firstCommaIdx)).trim();
      
      return label;
   }
   
   private String extractChunkLabel(String extractedChunkHeader)
   {
      int firstSpaceIdx = extractedChunkHeader.indexOf(' ');
      if (firstSpaceIdx == -1)
         return "";
      
      int firstCommaIdx = extractedChunkHeader.indexOf(',');
      if (firstCommaIdx == -1)
         firstCommaIdx = extractedChunkHeader.length();
      
      return firstCommaIdx <= firstSpaceIdx ?
            "" :
            extractedChunkHeader.substring(firstSpaceIdx + 1, firstCommaIdx).trim();
   }
   
   private void parseChunkHeader(String line,
                                 HashMap<String, String> chunkOptions)
   {
      String modeId = widget_.getEditor().getSession().getMode().getId();
      
      Pattern pattern = null;
      if (modeId.equals("mode/rmarkdown"))
         pattern = RegexUtil.RE_RMARKDOWN_CHUNK_BEGIN;
      else if (modeId.equals("mode/sweave"))
         pattern = RegexUtil.RE_SWEAVE_CHUNK_BEGIN;
      else if (modeId.equals("mode/rhtml"))
         pattern = RegexUtil.RE_RHTML_CHUNK_BEGIN;
      
      if (pattern == null) return;
      
      Match match = pattern.match(line,  0);
      if (match == null) return;
      
      String extracted = match.getGroup(1);
      chunkPreamble_ = extractChunkPreamble(extracted, modeId);
      
      String chunkLabel = extractChunkLabel(extracted);
      if (StringUtil.isNullOrEmpty(chunkLabel))
      {
         tbChunkLabel_.setCueMode(true);
      }
      else
      {
         tbChunkLabel_.setCueMode(false);
         tbChunkLabel_.setText(extractChunkLabel(extracted));
      }
      
      int firstCommaIndex = extracted.indexOf(',');
      String arguments = extracted.substring(firstCommaIndex + 1);
      TextCursor cursor = new TextCursor(arguments);
      
      int startIndex = 0;
      while (true)
      {
         if (!cursor.fwdToCharacter('=', false))
            break;
         
         int equalsIndex = cursor.getIndex();
         int endIndex = arguments.length();
         if (cursor.fwdToCharacter(',', true))
            endIndex = cursor.getIndex();
         
         chunkOptions.put(
               arguments.substring(startIndex, equalsIndex).trim(),
               arguments.substring(equalsIndex + 1, endIndex).trim());
         
         startIndex = cursor.getIndex() + 1;
      }
   }
   
   @Override
   public void hide()
   {
      position_ = null;
      chunkOptions_.clear();
      super.hide();
   }
   
   private Pair<String, String> getChunkHeaderBounds(String modeId)
   {
      if (modeId.equals("mode/rmarkdown"))
         return new Pair<String, String>("```{", "}");
      else if (modeId.equals("mode/sweave"))
         return new Pair<String, String>("<<", ">>=");
      else if (modeId.equals("mode/rhtml"))
         return new Pair<String, String>("<!--", "");
      else if (modeId.equals("mode/c_cpp"))
         return new Pair<String, String>("/***", "");
      
      return null;
   }
   
   private void synchronize()
   {
      String modeId = widget_.getEditor().getSession().getMode().getId();
      Pair<String, String> chunkHeaderBounds =
            getChunkHeaderBounds(modeId);
      if (chunkHeaderBounds == null)
         return;
      
      String label = tbChunkLabel_.getText();
      String newLine =
            chunkHeaderBounds.first +
            chunkPreamble_;
      
      if (!label.isEmpty())
      {
         if (StringUtil.isNullOrEmpty(chunkPreamble_))
            newLine += label;
         else
            newLine += " " + label;
      }
      
      if (!chunkOptions_.isEmpty())
      {
         if (!(StringUtil.isNullOrEmpty(chunkPreamble_) &&
             label.isEmpty()))
         {
            newLine += ", ";
         }
         
         newLine += StringUtil.collapse(chunkOptions_, "=", ", ");
      }
      
      newLine +=
            chunkHeaderBounds.second +
            "\n";
      
      widget_.getEditor().getSession().replace(
            Range.fromPoints(
                  Position.create(position_.getRow(), 0),
                  Position.create(position_.getRow() + 1, 0)), newLine);
   }
   
   private void revert()
   {
      if (position_ == null)
         return;
      
      Range replaceRange = Range.fromPoints(
            Position.create(position_.getRow(), 0),
            Position.create(position_.getRow() + 1, 0));
      
      widget_.getEditor().getSession().replace(
            replaceRange,
            originalLine_ + "\n");
   }
   
   private void hideAndFocusEditor()
   {
      hide();
      widget_.getEditor().focus();
   }
   
   private final VerticalPanel panel_;
   private final TextBoxWithCue tbChunkLabel_;
   private final SelectWidget outputComboBox_;
   private final Grid figureDimensionsPanel_;
   private final TextBox figWidthBox_;
   private final TextBox figHeightBox_;
   private final ThemedCheckBox useCustomFigureCheckbox_;
   private final TriStateCheckBox showWarningsInOutputCb_;
   private final TriStateCheckBox showMessagesInOutputCb_;
   
   private String originalLine_;
   private String chunkPreamble_;
   
   private HashMap<String, String> chunkOptions_;
   private HashMap<String, String> originalChunkOptions_;
   
   private AceEditorWidget widget_;
   private Position position_;
   
   private static final String OUTPUT_USE_DOCUMENT_DEFAULT =
         "(Use Document Default)";

   private static final String OUTPUT_SHOW_CODE_AND_OUTPUT =
         "Show Code and Output";

   private static final String OUTPUT_SHOW_OUTPUT_ONLY =
         "Show Output Only (Hide Code)";
   
   private static final String OUTPUT_SHOW_NOTHING =
         "Show Nothing";
   
   public interface Styles extends CssResource
   {
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
