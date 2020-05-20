/*
 * ChooseEncodingDialog.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ui;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.model.Session;

public class ChooseEncodingDialog extends ModalDialog<String>
{
   public ChooseEncodingDialog(JsArrayString commonEncodings,
                               JsArrayString allEncodings,
                               String currentEncoding,
                               boolean includePromptForEncoding,
                               boolean includeSaveAsDefault,
                               OperationWithInput<String> operation)
   {
      super("Choose Encoding", Roles.getDialogRole(), operation);
      commonEncodings_ = commonEncodings;
      allEncodings_ = allEncodings;
      currentEncoding_ = currentEncoding;
      includePromptForEncoding_ = includePromptForEncoding;
      includeSaveAsDefault_ = includeSaveAsDefault;

      Session session = RStudioGinjector.INSTANCE.getSession();
      systemEncoding_ = session.getSessionInfo().getSystemEncoding();
      systemEncodingNormalized_ = normalizeEncoding(systemEncoding_);
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
      listBox_ = new ListBox();
      listBox_.setVisibleItemCount(15);
      listBox_.setWidth("350px");
      Roles.getListboxRole().setAriaLabelProperty(listBox_.getElement(), "Encodings");

      setEncodings(commonEncodings_, currentEncoding_);

      CheckBox showAll = new CheckBox("Show all encodings");
      showAll.addValueChangeHandler(valueChangeEvent ->
      {
         if (valueChangeEvent.getValue())
            setEncodings(allEncodings_, currentEncoding_);
         else
            setEncodings(commonEncodings_, currentEncoding_);
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

      for (int i = 0; i < encodings.length(); i++)
      {
         listBox_.addItem(encodings.get(i));
      }

      int sysIndex = findSystemEncodingIndex();
      if (!StringUtil.isNullOrEmpty(systemEncoding_))
      {
         // Remove the system encoding (if it is present) so we can move it
         // to the top of the list. If it's already present, use the same
         // label (un-normalized encoding name) so it's consistent with
         // related encodings that are also present in the list.
         String sysEncName = sysIndex < 0 ? systemEncoding_
                                          : listBox_.getValue(sysIndex);
         if (sysIndex >= 0)
            listBox_.removeItem(sysIndex);
         listBox_.insertItem(sysEncName + " (System default)",
                             systemEncoding_,
                             0);
      }

      if (includePromptForEncoding_)
      {
         listBox_.insertItem(ASK_LABEL, "", 0);
      }

      if (isSystemEncoding(encoding))
         setCurrentValue(listBox_.getValue(includePromptForEncoding_ ? 1 : 0));
      else
         setCurrentValue(encoding);
   }

   private int findSystemEncodingIndex()
   {
      for (int i = 0; i < listBox_.getItemCount(); i++)
         if (isSystemEncoding(listBox_.getValue(i)))
            return i;
      return -1;
   }

   private boolean isSystemEncoding(String encoding)
   {
      return !StringUtil.isNullOrEmpty(encoding)
             && normalizeEncoding(encoding).equals(systemEncodingNormalized_);
   }

   public static String normalizeEncoding(String encoding)
   {
      return StringUtil.notNull(encoding).replaceAll("[- ]", "").toLowerCase();
   }

   private ListBox listBox_;
   private final JsArrayString commonEncodings_;
   private final JsArrayString allEncodings_;
   private final String currentEncoding_;
   private final boolean includePromptForEncoding_;
   private final boolean includeSaveAsDefault_;
   private CheckBox saveAsDefault_;
   private final String systemEncoding_;
   private final String systemEncodingNormalized_;
   public static final String ASK_LABEL = "[Ask]";
}
