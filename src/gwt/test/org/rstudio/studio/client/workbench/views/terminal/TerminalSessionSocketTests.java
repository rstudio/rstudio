/*
 * TerminalSessionSocketTests.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.terminal;

import com.google.gwt.junit.client.GWTTestCase;
import junit.framework.Assert;

public class TerminalSessionSocketTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testCommonPasswordPrompts()
   {
      Assert.assertFalse(
            TerminalSessionSocket.PASSWORD_PATTERN.test("Nothing to see here"));
      Assert.assertTrue(
            TerminalSessionSocket.PASSWORD_PATTERN.test("Password:"));
      Assert.assertTrue(
            TerminalSessionSocket.PASSWORD_PATTERN.test("password:"));
      Assert.assertFalse(
            TerminalSessionSocket.PASSWORD_PATTERN.test("Password"));
      Assert.assertFalse(
            TerminalSessionSocket.PASSWORD_PATTERN.test("password"));
      Assert.assertTrue(
            TerminalSessionSocket.PASSWORD_PATTERN.test("Passphrase:"));
      Assert.assertTrue(
            TerminalSessionSocket.PASSWORD_PATTERN.test("passphrase:"));
      Assert.assertFalse(
            TerminalSessionSocket.PASSWORD_PATTERN.test("Passphrase"));
      Assert.assertFalse(
            TerminalSessionSocket.PASSWORD_PATTERN.test("passphrase"));
   }

   public void testPasswordPromptsWithExtraGunk()
   {
      Assert.assertTrue(
            TerminalSessionSocket.PASSWORD_PATTERN.test("My HappyPassword:  "));
      Assert.assertTrue(
            TerminalSessionSocket.PASSWORD_PATTERN.test("\n\rpassword:::"));
      Assert.assertTrue(
            TerminalSessionSocket.PASSWORD_PATTERN.test("The Passphrase: is here"));
      Assert.assertTrue(
            TerminalSessionSocket.PASSWORD_PATTERN.test("passphrasepasswordpassphrase: "));
   }

}
