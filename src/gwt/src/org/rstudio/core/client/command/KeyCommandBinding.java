/*
 * KeyCommandBinding.java
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
package org.rstudio.core.client.command;

/**
 * Represents a key sequence associated with a command binding
 */
public class KeyCommandBinding
{
   public KeyCommandBinding(KeyMap.CommandBinding binding, KeySequence keys)
   {
      binding_ = binding;
      keys_ = keys;
   }

   public KeyMap.CommandBinding getBinding()
   {
      return binding_;
   }

   public KeySequence getKeys()
   {
      return keys_;
   }

   private final KeyMap.CommandBinding binding_;
   private final KeySequence keys_;
}
