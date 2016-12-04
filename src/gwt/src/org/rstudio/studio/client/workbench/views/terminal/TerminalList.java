/*
 * TerminalList.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

import java.util.Iterator;
import java.util.LinkedHashMap;

import org.rstudio.studio.client.common.console.ConsoleProcessInfo;

/**
 * List of terminals, with sufficient metadata to display a list of
 * available terminals and reconnect to them.
 */
public class TerminalList implements Iterable<TerminalList.TerminalMetadata>
{
   public static class TerminalMetadata
   {
      /**
       * Create a TerminalMetadata object
       * @param handle terminal handle, unique key
       * @param title terminal title
       * @param sequence terminal sequence number
       */
      public TerminalMetadata(String handle, String title, int sequence)
      {
         handle_ = handle;
         title_ = title;
         sequence_ = sequence;
      }

      public String getHandle() { return handle_; }
      public String getTitle() { return title_; }
      public int getSequence() { return sequence_; }

      private String handle_;
      private String title_;
      private int sequence_;
   }

   /**
    * Append new terminal to the list.
    * @param terminal terminal metadata to add
    */
   public void addTerminal(TerminalMetadata terminal)
   {
      terminals_.put(terminal.getHandle(), terminal);
   }
   
   void removeTerminal(String handle)
   {
      terminals_.remove(handle);
   }

 /**
    * Number of terminals in cache.
    * @return number of terminals tracked by this object
    */
   public int terminalCount()
   {
      return terminals_.size();
   }

   /**
    * Return 0-based index of a terminal in the list.
    * @param handle terminal to find
    * @return 0-based index of terminal, -1 if not found
    */
   public int indexOfTerminal(String handle)
   {
      int i = 0;
      for (final java.util.Map.Entry<String, TerminalMetadata> item : terminals_.entrySet())
      {
         if (item.getValue().getHandle().equals(handle))
         {
            return i;
         }
         i++;
      }

      return -1;
   }

   /**
    * Return terminal handle at given 0-based index
    * @param handle terminal to find
    * @return handle of terminal at index, or null if invalid index
    */
   public String terminalHandleAtIndex(int i)
   {
      int j = 0;
      for (final java.util.Map.Entry<String, TerminalMetadata> item : terminals_.entrySet())
      {
         if (i == j)
         {
            return item.getValue().getHandle();
         }
         j++;
      }
      return null;
   }

   /**
    * Get metadata for terminal with given handle.
    * @param handle handle of terminal of interest
    * @return terminal metadata or null if not found
    */
   public TerminalMetadata getMetadataForHandle(String handle)
   {
      return terminals_.get(handle);
   }

   /**
    * Choose a 1-based sequence number one higher than the highest currently 
    * known terminal number. We don't try to fill gaps if terminals are closed 
    * in the middle of the opened tabs.
    * @return Highest currently known terminal plus one
    */
   public int nextTerminalSequence()
   {
      int maxNum = ConsoleProcessInfo.SEQUENCE_NO_TERMINAL;
      for (final java.util.Map.Entry<String, TerminalMetadata> item : terminals_.entrySet())
      {
         maxNum = Math.max(maxNum, item.getValue().getSequence());
      }
      return maxNum + 1;
   }

   @Override
   public Iterator<TerminalMetadata> iterator()
   {
      return new Iterator<TerminalMetadata>()
      {
         private Iterator<java.util.Map.Entry<String, TerminalMetadata>> iterator = 
               terminals_.entrySet().iterator();

         @Override
         public boolean hasNext()
         {
            return iterator.hasNext();
         }

         @Override
         public TerminalMetadata next()
         {
            return iterator.next().getValue();
         }
         
         @Override
         public void remove()
         {
            throw new UnsupportedOperationException(); 
         }
      };
   }

   /**
    * Map of terminal handles to terminal metadata; order they are added
    * is the order they will be iterated.
    */
   private LinkedHashMap<String, TerminalMetadata> terminals_ = 
                new LinkedHashMap<String, TerminalMetadata>();
}