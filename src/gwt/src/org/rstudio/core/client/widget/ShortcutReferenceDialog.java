/*
 * ShortcutReferenceDialog.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.core.client.widget;

import org.rstudio.core.client.CoreClientConstants;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Modal dialog hosting a keyboard shortcut reference: one heading and table
 * per group of shortcuts, in a scrollable region.
 */
public class ShortcutReferenceDialog extends ModalDialogBase
{
   public ShortcutReferenceDialog(String caption, Widget content)
   {
      super(Roles.getDialogRole());
      setThemeAware(true);
      setText(caption);
      content_ = content;
      addOkButton(new ThemedButton(constants_.closeText(), event -> closeDialog()));
   }

   @Override
   protected Widget createMainWidget()
   {
      scroller_ = new ScrollPanel(content_);
      scroller_.addStyleName(RES.styles().content());

      // Focusable so the keyboard can scroll it and screen readers start
      // reading at the reference rather than at the Close button
      scroller_.getElement().setTabIndex(0);
      Roles.getRegionRole().set(scroller_.getElement());
      Roles.getRegionRole().setAriaLabelProperty(scroller_.getElement(), getText());
      return scroller_;
   }

   @Override
   protected void focusInitialControl()
   {
      scroller_.getElement().focus();
   }

   public interface Styles extends CssResource
   {
      String content();
      String groups();
   }

   public interface Resources extends ClientBundle
   {
      @Source("ShortcutReferenceDialog.css")
      Styles styles();
   }

   public static final Resources RES = GWT.create(Resources.class);
   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }

   private final Widget content_;
   private ScrollPanel scroller_;
   private static final CoreClientConstants constants_ = GWT.create(CoreClientConstants.class);
}
