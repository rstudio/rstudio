/*
 * RnwWeaveRegistry.java
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
package org.rstudio.studio.client.common.rnw;

import java.util.ArrayList;

import org.rstudio.studio.client.workbench.model.Session;

import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class RnwWeaveRegistry
{
   @Inject
   public RnwWeaveRegistry(Provider<Session> pSession)
   {
      pSession_ = pSession;
   }
  
   public String[] getTypeNames()
   {
      ArrayList<RnwWeave> weaveTypes = getTypes();
      String[] typeNames = new String[weaveTypes.size()];
      for (int i=0; i<weaveTypes.size(); i++)
         typeNames[i] = weaveTypes.get(i).getName();
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
         {
            if (typeNames.length > 2)
               str.append(", ");
            else
               str.append(" ");
         }
         if (i == (typeNames.length - 2))
            str.append("and ");
      }
      return str.toString();
   }
   
   public ArrayList<RnwWeave> getTypes()
   {
      if (weaveTypes_ == null)
      {
         JsArray<RnwWeave> types = 
                           pSession_.get().getSessionInfo().getRnwWeaveTypes();
       
         weaveTypes_ = new ArrayList<RnwWeave>();
         for (int i=0; i<types.length(); i++)
            weaveTypes_.add(types.get(i));
      }
      return weaveTypes_;
   }
   
   public RnwWeave findTypeIgnoreCase(String name)
   {
      for (RnwWeave rnwWeave : getTypes())
      {
         if (rnwWeave.getName().equalsIgnoreCase(name))
            return rnwWeave;
      }
      
      return null;
   }

  
   
   private final Provider<Session> pSession_;
   private ArrayList<RnwWeave> weaveTypes_ = null;
}
