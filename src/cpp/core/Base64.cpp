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
#include <boost/archive/iterators/binary_from_base64.hpp>
#include <boost/archive/iterators/transform_width.hpp>
#include <boost/archive/iterators/ostream_iterator.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FileSerializer.hpp>

namespace rstudio {
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

Error encode(const FilePath& inputFile, std::string* pOutput)
{
   std::string contents;
   Error error = core::readStringFromFile(inputFile, &contents);
   if (error)
      return error;

   return encode(contents, pOutput);
}


Error decode(const std::string& input, std::string* pOutput)
{
   using namespace boost::archive::iterators;

   typedef transform_width<binary_from_base64<
                           std::string::const_iterator>, 8, 6 > text_b64;

   // cast away const so we can temporarily manipulate the input string without
   // making a copy 
   std::string& base64 = const_cast<std::string&>(input);
   unsigned pad = 0;

   try
   {
      // remove = padding from end and replace with base64 encoding for 0
      // (count instances removed)
      while (base64.at(base64.size() - (pad + 1)) == '=')
        base64[base64.size() - (pad++ + 1)] = 'A';

      // perform the decoding
      *pOutput = std::string(text_b64(base64.begin()), text_b64(base64.end()));

      // erase padding from output
      pOutput->erase(pOutput->end() - pad, pOutput->end());
   }
   CATCH_UNEXPECTED_EXCEPTION

   // restore the input string contents
   for (size_t i = 0; i < pad; i++)
      base64[base64.size() - (i + 1)] = '=';

   return Success();
}

} // namespace base64
} // namespace core
} // namespace rstudio




