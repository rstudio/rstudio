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
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.MiniPopupPanel;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.core.client.widget.TextBoxWithCue;
import org.rstudio.core.client.widget.TriStateCheckBox;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import com.google.gwt.core.client.GWT;
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
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ChunkOptionsPopupPanel extends MiniPopupPanel
{
   public ChunkOptionsPopupPanel()
   {
      super(true);
      
      chunkOptions_ = new HashMap<String, String>();
      checkboxMap_ = new HashMap<String, TriStateCheckBox>();
      
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
      
      Label chunkLabel = new Label("Chunk name:");
      chunkLabel.addStyleName(RES.styles().chunkLabel());
      labelPanel.add(chunkLabel);
      
      tbChunkLabel_.addStyleName(RES.styles().chunkName());
      labelPanel.add(tbChunkLabel_);
      
      panel_.add(labelPanel);
      
      for (Map.Entry<String, String> entry : BOOLEAN_CHUNK_OPTIONS.entrySet())
      {
         addCheckboxController(entry.getKey(), entry.getValue());
      }
      
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
   
   public void focus()
   {
      tbChunkLabel_.setFocus(true);
   }
   
   public void init(AceEditorWidget widget, Position position)
   {
      widget_ = widget;
      position_ = position;
      chunkOptions_.clear();
      
      originalLine_ = widget_.getEditor().getSession().getLine(position_.getRow());
      parseChunkHeader(originalLine_, chunkOptions_);
      
      for (String option : BOOLEAN_CHUNK_OPTIONS.keySet())
      {
         TriStateCheckBox cb = checkboxMap_.get(option);
         assert cb != null :
            "No checkbox for boolean option '" + option + "'";
         
         if (chunkOptions_.containsKey(option))
         {
            boolean truthy = isTrue(chunkOptions_.get(option));
            if (truthy)
               cb.setState(TriStateCheckBox.STATE_ON);
            else
               cb.setState(TriStateCheckBox.STATE_OFF);
         }
         else
         {
            cb.setState(TriStateCheckBox.STATE_INDETERMINATE);
         }
      }
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
   
   private void addCheckboxController(final String optionName,
                                      final String label)
   {
      final TriStateCheckBox cb = new TriStateCheckBox(label);
      cb.addStyleName(RES.styles().checkBox());
      cb.addValueChangeHandler(new ValueChangeHandler<TriStateCheckBox.State>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<TriStateCheckBox.State> event)
         {
            TriStateCheckBox.State state = event.getValue();
            if (state == TriStateCheckBox.STATE_INDETERMINATE)
               chunkOptions_.remove(optionName);
            else if (state == TriStateCheckBox.STATE_OFF)
               chunkOptions_.put(optionName, "FALSE");
            else
               chunkOptions_.put(optionName,  "TRUE");
            synchronize();
         }
      });
      checkboxMap_.put(optionName, cb);
      panel_.add(cb);
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
   private final HashMap<String, TriStateCheckBox> checkboxMap_;
   
   private String originalLine_;
   private String chunkPreamble_;
   private HashMap<String, String> chunkOptions_;
   
   private AceEditorWidget widget_;
   private Position position_;
   
   private static final HashMap<String, String> BOOLEAN_CHUNK_OPTIONS;
   
   static {
      BOOLEAN_CHUNK_OPTIONS = new HashMap<String, String>();
      BOOLEAN_CHUNK_OPTIONS.put("eval", "Evaluate R code");
      BOOLEAN_CHUNK_OPTIONS.put("include", "Include chunk output");
      BOOLEAN_CHUNK_OPTIONS.put("echo", "Echo R code");
   }
   
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
