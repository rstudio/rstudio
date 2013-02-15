/*
 * Base64.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <sstream>

#include <algorithm>

#include <boost/archive/iterators/base64_from_binary.hpp>
#include <boost/archive/iterators/transform_width.hpp>
#include <boost/archive/iterators/ostream_iterator.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>


namespace core {
namespace base64 {

Error encode(const std::string& input, std::string* pOutput)
{
   using namespace boost::archive::iterators;

   try
   {
      typedef base64_from_binary<transform_width<const char *,6,8> > b64_text;
      std::stringstream os;
      std::copy(b64_text(input.c_str()),
                b64_text(input.c_str() + input.size()),
                ostream_iterator<char>(os));

      pOutput->clear();
      pOutput->reserve(((input.size() * 4) / 3) + 3);
      pOutput->append(os.str());

      std::size_t mod = input.size() % 3;
      if (mod == 1)
         pOutput->append("==");
      else if(mod == 2)
         pOutput->append("=");

      return Success();
   }
   CATCH_UNEXPECTED_EXCEPTION

   // keep compiler happy
   return Success();
}


} // namespace base64
} // namespace core




