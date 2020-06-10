/*
 * MapUtil.java
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
package org.rstudio.core.client;

import java.util.Map;

import org.rstudio.core.client.container.SafeMap;

public class MapUtil
{
   public interface ForEachCommand<K, V>
   {
      public void execute(K key, V value);
   }
   
   public static <K, V> void forEach(Map<K, V> map, ForEachCommand<K, V> command)
   {
      for (Map.Entry<K, V> entry : map.entrySet())
         command.execute(entry.getKey(), entry.getValue());
   }
   
   public static <K, V> void forEach(SafeMap<K, V> map, ForEachCommand<K, V> command)
   {
      for (Map.Entry<K, V> entry : map.entrySet())
         command.execute(entry.getKey(), entry.getValue());
   }
}
