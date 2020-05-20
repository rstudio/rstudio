/*
 * ChunkParser.cpp
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

#include <ios>
#include <sstream>

#include <boost/make_shared.hpp>

#include <core/http/ChunkParser.hpp>

namespace rstudio {
namespace core {
namespace http {


bool ChunkParser::parse(const char* buffer, size_t len,
                        std::deque<boost::shared_ptr<std::string> >* pOutChunks)
{
   const char* end = buffer + len;

   while (buffer != end)
   {
      char curChar = *buffer;

      switch (state_)
      {
         case HeaderSize:
            if (curChar == '\r')
            {
               // calculate the chunk size
               std::istringstream(chunkHeader_) >> std::hex >> chunkSize_;
               if (chunkSize_ == 0)
               {
                  state_ = Complete;
               }
               else
               {
                  state_ = HeaderCRLF;
               }
            }
            else if (curChar == '\n')
            {
               state_ = Invalid;
            }
            else
            {
               chunkHeader_.append(1, curChar);
            }
            break;

         case HeaderCRLF:
            if (curChar != '\n')
            {
               state_ = Invalid;
            }
            else
            {
               state_ = Chunk;
            }
            break;

         case Chunk:
            if (!chunk_)
               chunk_ = boost::make_shared<std::string>();

            chunk_->append(1, curChar);
            if (chunk_->size() == chunkSize_)
            {
               pOutChunks->push_back(chunk_);
               state_ = ChunkCRLF;
            }
            break;

         case ChunkCRLF:
            if (curChar == '\n')
            {
               state_ = HeaderSize;
               chunkHeader_.clear();
               chunk_.reset();
               chunkSize_ = 0;
            }
            break;

         // invalid and complete cases signify that we are done parsing chunks
         case Invalid:
            return true;

         case Complete:
            return true;
      }

      buffer++;
   }

   // more chunks to come
   return false;
}

} // namespace http
} // namespace core
} // namespace rstudio
