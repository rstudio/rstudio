package org.rstudio.studio.client.workbench.views.source.editors.text.ui;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;

public class ChooseEncodingDialog extends ModalDialog<String>
{
   public ChooseEncodingDialog(JsArrayString commonEncodings,
                               JsArrayString allEncodings,
                               String currentEncoding,
                               boolean includePromptForEncoding,
                               boolean includeSaveAsDefault,
                               OperationWithInput<String> operation)
   {
      super("Choose Encoding", operation);
      commonEncodings_ = commonEncodings;
      allEncodings_ = allEncodings;
      currentEncoding_ = currentEncoding;
      includePromptForEncoding_ = includePromptForEncoding;
      includeSaveAsDefault_ = includeSaveAsDefault;
   }

   private void setCurrentValue(String currentEncoding)
   {
      currentEncoding = StringUtil.notNull(currentEncoding);
      if (!includePromptForEncoding_ && "".equals(currentEncoding))
         return;

      // Select current value if it exists--if not, add it
      for (int i = 0; i < listBox_.getItemCount(); i++)
         if (listBox_.getValue(i).equalsIgnoreCase(currentEncoding))
         {
            listBox_.setSelectedIndex(i);
            return;
         }

      listBox_.insertItem(currentEncoding, 0);
      listBox_.setSelectedIndex(0);
   }

   @Override
   protected String collectInput()
   {
      if (listBox_.getSelectedIndex() >= 0)
         return listBox_.getValue(listBox_.getSelectedIndex());
      else
         return null;
   }

   @Override
   protected boolean validate(String input)
   {
      return input != null;
   }

   @Override
   protected Widget createMainWidget()
   {
      listBox_ = new ListBox(true);
      listBox_.setVisibleItemCount(15);
      listBox_.setWidth("350px");

      setEncodings(commonEncodings_, currentEncoding_);

      CheckBox showAll = new CheckBox("Show all encodings");
      showAll.addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         public void onValueChange(ValueChangeEvent<Boolean> e)
         {
            if (e.getValue())
               setEncodings(allEncodings_, currentEncoding_);
            else
               setEncodings(commonEncodings_, currentEncoding_);
         }
      });
      setCheckBoxMargins(showAll, 8, 12);

      VerticalPanel panel = new VerticalPanel();
      panel.add(listBox_);
      panel.add(showAll);

      if (includeSaveAsDefault_)
      {
         saveAsDefault_ = new CheckBox("Set as default encoding for " +
                                       "source files");
         setCheckBoxMargins(showAll, 8, 0);
         setCheckBoxMargins(saveAsDefault_, 3, 12);
         panel.add(saveAsDefault_);
      }

      return panel;
   }

   private void setCheckBoxMargins(CheckBox checkBox,
                                   int topMargin,
                                   int bottomMargin)
   {
      checkBox.getElement().getStyle().setDisplay(Display.BLOCK);
      checkBox.getElement().getStyle().setMarginTop(topMargin, Unit.PX);
      checkBox.getElement().getStyle().setMarginBottom(bottomMargin, Unit.PX);
   }

   public boolean isSaveAsDefault()
   {
      return saveAsDefault_ != null && saveAsDefault_.getValue();
   }

   private void setEncodings(JsArrayString encodings, String encoding)
   {
      listBox_.clear();

      if (includePromptForEncoding_)
      {
         listBox_.addItem("[Ask]", "");
      }

      for (int i = 0; i < encodings.length(); i++)
         listBox_.addItem(encodings.get(i));

      setCurrentValue(encoding);
   }

   private ListBox listBox_;
   private final JsArrayString commonEncodings_;
   private final JsArrayString allEncodings_;
   private final String currentEncoding_;
   private final boolean includePromptForEncoding_;
   private final boolean includeSaveAsDefault_;
   private CheckBox saveAsDefault_;
}
