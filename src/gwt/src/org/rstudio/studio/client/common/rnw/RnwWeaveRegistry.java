/*
 * RnwWeaveRegistry.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.rnw;

import java.util.ArrayList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RnwWeaveRegistry
{
   @Inject
   public RnwWeaveRegistry()
   {
      defaultType_ = new RnwSweave();
      register(defaultType_);
      register(new RnwPgfSweave());
      register(new RnwKnitr());
   }
   
   public RnwWeave getDefaultType()
   {
      return defaultType_;
   }
   
   public String[] getTypeNames()
   {
      String[] typeNames = new String[weaveTypes_.size()];
      for (int i=0; i<weaveTypes_.size(); i++)
         typeNames[i] = weaveTypes_.get(i).getName();
      return typeNames;
   }
   
   public String getPrintableTypeNames()
   {
      StringBuffer str = new StringBuffer();
      String[] typeNames = getTypeNames();
      for (int i=0; i<typeNames.length; i++)
      {
         str.append(typeNames[i]);
         if (i != (typeNames.length - 1))
            str.append(", ");
         if (i == (typeNames.length - 2))
            str.append("and ");
      }
      return str.toString();
   }
   
   public ArrayList<RnwWeave> getTypes()
   {
      return weaveTypes_;
   }
   
   public RnwWeave findTypeIgnoreCase(String name)
   {
      for (RnwWeave rnwWeave : weaveTypes_)
      {
         if (rnwWeave.getName().equalsIgnoreCase(name))
            return rnwWeave;
      }
      
      return null;
   }

   private void register(RnwWeave weave)
   {
      weaveTypes_.add(weave);
   }
   
   private final RnwWeave defaultType_;
   private ArrayList<RnwWeave> weaveTypes_ = new ArrayList<RnwWeave>();
}
