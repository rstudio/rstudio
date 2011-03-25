package org.rstudio.studio.client.workbench.views.source.editors.text.ui;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;

public class ChooseEncodingDialog extends ModalDialog<String>
{
   public ChooseEncodingDialog(JsArrayString commonEncodings,
                               JsArrayString allEncodings,
                               String currentEncoding,
                               OperationWithInput<String> operation)
   {
      super("Choose Encoding", operation);
      commonEncodings_ = commonEncodings;
      allEncodings_ = allEncodings;
      currentEncoding_ = currentEncoding;
   }

   private void setCurrentValue(String currentEncoding)
   {
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

      VerticalPanel panel = new VerticalPanel();
      panel.add(listBox_);
      panel.add(showAll);

      return panel;
   }

   private void setEncodings(JsArrayString encodings, String encoding)
   {
      listBox_.clear();
      for (int i = 0; i < encodings.length(); i++)
         listBox_.addItem(encodings.get(i));

      setCurrentValue(encoding);
   }

   private ListBox listBox_;
   private final JsArrayString commonEncodings_;
   private final JsArrayString allEncodings_;
   private final String currentEncoding_;
}
