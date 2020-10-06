/*
 * CodeSearchDialog.java
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
package org.rstudio.studio.client.workbench.codesearch.ui;

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.studio.client.workbench.codesearch.CodeSearch;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Provider;

public class CodeSearchDialog extends ModalDialogBase 
                              implements CodeSearch.Observer

{
   public CodeSearchDialog(Provider<CodeSearch> pCodeSearch)
   {
      super(Roles.getDialogRole());
      
      setGlassEnabled(false);
      setAutoHideEnabled(true);
      
      setText("Go to File/Function");
      
      pCodeSearch_ = pCodeSearch;
   }
   
   @Override
   protected Widget createMainWidget()
   {     
      VerticalPanel mainPanel = new VerticalPanel();
      mainPanel.addStyleName(
         CodeSearchResources.INSTANCE.styles().codeSearchDialogMainWidget());
      codeSearch_ = pCodeSearch_.get();
      codeSearch_.setObserver(this);
      mainPanel.add(codeSearch_.getSearchWidget());
      return mainPanel;
   }
   
   @Override
   protected void positionAndShowDialog(final Command onCompleted)
   {
      setPopupPositionAndShow((int offsetWidth, int offsetHeight) ->
      {
         int left = (Window.getClientWidth() / 2) - (offsetWidth / 2);
         setPopupPosition(left, 15);
         onCompleted.execute();
      });
   }
   
   @Override
   protected void focusInitialControl()
   { 
      ((CanFocus)codeSearch_.getSearchWidget()).focus();
   }
   
   @Override
   protected void onUnload()
   {
      super.onUnload();
      if (codeSearch_ != null)
         codeSearch_.detachEventBusHandlers();
   }
   
   @Override
   public void onCompleted()
   {
      setRestoreFocusOnClose(false);
      closeDialog();  
   }
   
   @Override
   public void onCancel()
   {
      // delay to prevent ESC key from ever getting into the editor
      Scheduler.get().scheduleDeferred(() -> closeDialog());
   }
   
   @Override
   public String getCueText()
   {
      return "";
   }
  
   Provider<CodeSearch> pCodeSearch_;
   CodeSearch codeSearch_;
   
   
}
