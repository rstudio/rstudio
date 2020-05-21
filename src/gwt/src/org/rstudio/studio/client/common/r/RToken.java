/*
 * RToken.java
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
package org.rstudio.studio.client.common.r;

public class RToken
{
   public RToken(int tokenType, String content, int offset, int length)
   {
      super();
      this.tokenType_ = tokenType;
      this.content_ = content;
      this.offset_ = offset;
      this.length_ = length;
   }
   
   public int getTokenType()
   {
      return tokenType_;
   }
   public String getContent()
   {
      return content_;
   }
   public int getOffset()
   {
      return offset_;
   }
   public int getLength()
   {
      return length_;
   }
   
   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((content_ == null) ? 0 : content_.hashCode());
      result = prime * result + length_;
      result = prime * result + offset_;
      result = prime * result + tokenType_;
      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      RToken other = (RToken) obj;
      if (content_ == null)
      {
         if (other.content_ != null)
            return false;
      } else if (!content_.equals(other.content_))
         return false;
      if (length_ != other.length_)
         return false;
      if (offset_ != other.offset_)
         return false;
      if (tokenType_ != other.tokenType_)
         return false;
      return true;
   }
   
   public static final int LPAREN         = '(';
   public static final int RPAREN         = ')';
   public static final int LBRACKET       = '[';
   public static final int RBRACKET       = ']';
   public static final int LBRACE         = '{';
   public static final int RBRACE         = '}';
   public static final int COMMA          = ',';
   public static final int SEMI           = ';';
   public static final int WHITESPACE     = 0x1001;
   public static final int STRING         = 0x1002;
   public static final int NUMBER         = 0x1003;
   public static final int ID             = 0x1004;
   public static final int OPER           = 0x1005;
   public static final int UOPER          = 0x1006;
   public static final int ERROR          = 0x1007;
   public static final int LDBRACKET      = 0x1008; // [[
   public static final int RDBRACKET      = 0x1009; // ]]
   public static final int COMMENT        = 0x100A;

   private final int tokenType_;
   private final String content_;
   private final int offset_;
   private final int length_;
}
