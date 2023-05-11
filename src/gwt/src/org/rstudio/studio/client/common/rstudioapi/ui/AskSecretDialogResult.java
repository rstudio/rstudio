/*
 * AskSecretDialogResult.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

package org.rstudio.studio.client.common.rstudioapi.ui;

public class AskSecretDialogResult
{
   public AskSecretDialogResult(String secret, boolean remember, boolean hasChanged)
   {
      secret_ = secret;
      remember_ = remember;
      hasChanged_ = hasChanged;
   }

   public String getSecret()
   {
      return secret_;
   }

   public boolean getRemember()
   {
      return remember_;
   }

   public boolean getHasChanged()
   {
      return hasChanged_;
   }

   private String secret_;
   private boolean remember_;
   private boolean hasChanged_;
}
