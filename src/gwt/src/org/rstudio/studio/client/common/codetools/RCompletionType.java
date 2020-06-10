/*
 * RCompletionType.java
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
package org.rstudio.studio.client.common.codetools;

public class RCompletionType
{
   public static final int UNKNOWN     =  0;
   public static final int VECTOR      =  1;
   public static final int ARRAY       =  2;
   public static final int DATAFRAME   =  3;
   public static final int LIST        =  4;
   public static final int ENVIRONMENT =  5;
   public static final int FUNCTION    =  6;
   public static final int ARGUMENT    =  7;
   public static final int S4_CLASS    =  8;
   public static final int S4_OBJECT   =  9;
   public static final int S4_GENERIC  = 10;
   public static final int S4_METHOD   = 11;
   public static final int R5_CLASS    = 12;
   public static final int R5_OBJECT   = 13;
   public static final int R5_METHOD   = 14;
   public static final int FILE        = 15;
   public static final int DIRECTORY   = 16;
   public static final int CHUNK       = 17;
   public static final int ROXYGEN     = 18;
   public static final int HELP        = 19;
   public static final int STRING      = 20;
   public static final int PACKAGE     = 21;
   public static final int KEYWORD     = 22;
   public static final int OPTION      = 23;
   public static final int DATASET     = 24;
   
   public static final int SNIPPET     = 98;
   public static final int CONTEXT     = 99;
   
   public static boolean isFunctionType(int type)
   {
      return type == FUNCTION ||
             type == S4_GENERIC ||
             type == S4_METHOD ||
             type == R5_METHOD;
   }
   
   public static boolean isFileType(int type)
   {
      return type == FILE ||
             type == DIRECTORY;
   }
}
