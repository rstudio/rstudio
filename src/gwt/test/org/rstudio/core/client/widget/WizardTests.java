/*
 * WizardTests.java
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class WizardTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   // A page whose validator throws, as NewDirectoryPage did for an empty name.
   private static class ThrowingPage extends WizardPage<String, String>
   {
      ThrowingPage()
      {
         super("Throwing", "", "Throwing", null, null);
      }

      @Override
      protected Widget createWidget()
      {
         return new Label();
      }

      @Override
      protected void initialize(String initData)
      {
      }

      @Override
      protected String collectInput()
      {
         return "";
      }

      @Override
      protected void validateAsync(String input,
                                   OperationWithInput<Boolean> onValidated)
      {
         calls++;
         throw new IllegalStateException("validator failed");
      }

      @Override
      public void focus()
      {
      }

      int calls = 0;
   }

   public void testThrowingValidatorReportsInvalidAndStaysUsable()
   {
      ThrowingPage page = new ThrowingPage();
      Wizard<String, String> wizard = new Wizard<>(
            "Test Wizard", "OK", Roles.getDialogRole(), "", page, null);
      List<Boolean> results = new ArrayList<>();

      // ModalDialog waits on this callback to leave its validating state; a
      // missing call leaves the OK button ignoring every later click
      wizard.validateAsync("", results::add);
      wizard.validateAsync("", results::add);

      assertEquals("each attempt should reach the page", 2, page.calls);
      assertEquals("each attempt should report invalid input",
                   Arrays.asList(false, false), results);
   }
}
