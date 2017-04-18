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

#include <core/Macros.hpp>
#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FileSerializer.hpp>

#include <boost/scoped_array.hpp>

namespace rstudio {
namespace core {
namespace base64 {

typedef unsigned char Byte;

namespace {

std::string table()
{
   return "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
}

std::size_t encoded_size(std::size_t n)
{
   return (n + 2) / 3 * 4;
}

class Encoder
{
public:

   Encoder()
   {
      init(table());
   }

   explicit Encoder(const std::string& table)
   {
      init(table);
   }
   
   // COPYING: copyable members
   
private:
   
   void init(const std::string& table)
   {
      const Byte* pData = (const Byte*) table.c_str();
      table_.assign(pData, pData + table.size());
   }

public:
   
   Error operator()(const std::string& string, std::string* pOutput)
   {
      return (*this)(string.c_str(), string.size(), pOutput);
   }

   Error operator()(const char* pData, std::size_t n, std::string* pOutput)
   {
      return (*this)((const Byte*) pData, n, pOutput);
   }
   
   Error operator()(const Byte* pData, std::size_t n, std::string* pOutput)
   {
      std::size_t size = encoded_size(n);
      boost::scoped_array<Byte> pBuffer(new Byte[size + 1]);
      pBuffer[size] = '\0';
      
      Byte* pTable = &table_[0];
      Byte* it = pBuffer.get();
      while (n >= 3)
      {
         *it++ = pTable[pData[0] >> 2];
         *it++ = pTable[((pData[0] & 0x03) << 4) | ((pData[1] & 0xF0) >> 4)];
         *it++ = pTable[((pData[1] & 0x0F) << 2) | ((pData[2] & 0xC0) >> 6)];
         *it++ = pTable[pData[2] & 0x3F];

         n -= 3;
         pData += 3;
      }

      if (n == 0)
         goto FINISH;

      *it++ = pTable[pData[0] >> 2];
      if (n == 1)
      {
         *it++ = pTable[(pData[0] & 0x03) << 4];
         *it++ = '=';
         *it++ = '=';
         goto FINISH;
      }

      *it++ = pTable[((pData[0] & 0x03) << 4) | ((pData[1] & 0xF0) >> 4)];
      *it++ = pTable[(pData[1] & 0x0F) << 2];
      *it++ = '=';

FINISH:
      pOutput->assign((const char*) pBuffer.get(), size);
      return Success();
   }

private:
   std::vector<Byte> table_;
};

} // end anonymous namespace

Error encode(const char* pData, std::size_t n, std::string* pOutput)
{
   Encoder encode;
   return encode(pData, n, pOutput);
}

Error encode(const std::string& input, std::string* pOutput)
{
   Encoder encode;
   return encode(input, pOutput);
}

Error encode(const FilePath& inputFile, std::string* pOutput)
{
   std::string contents;
   Error error = core::readStringFromFile(inputFile, &contents);
   if (error)
      return error;

   return encode(contents, pOutput);
}

namespace {

std::size_t decoded_size(std::size_t n)
{
   return n * 3 / 4;
}

class Decoder
{
public:

   Decoder()
   {
      init(table());
   }

   explicit Decoder(const std::string& table)
   {
      init(table);
   }
   
   // COPYING: copyable members

private:

   void init(const std::string& table)
   {
      table_.resize(1 << CHAR_BIT, (Byte) -1);
      const Byte* pData = reinterpret_cast<const Byte*>(table.c_str());
      for (Byte i = 0, n = table.size(); i < n; ++i)
         table_[pData[i]] = i;
   }
   
   bool invalid(Byte byte)
   {
      return byte == (Byte) -1;
   }
   
   Error decodeError(const ErrorLocation& location, const std::string& reason)
   {
      Error error = systemError(
               boost::system::errc::illegal_byte_sequence,
               location);
      error.addProperty("reason", reason);
      return error;
   }
   
   Error decodeLengthError(std::size_t size, const ErrorLocation& location)
   {
      std::stringstream ss;
      ss << "string length " << size << " is not a multiple of 4";
      return decodeError(location, ss.str());
   }
   
   Error decodeByteError(Byte byte, const ErrorLocation& location)
   {
      std::stringstream ss;
      ss << "invalid byte '" << byte << "'";
      return decodeError(location, ss.str());
   }

public:

   Error operator()(const std::string& input, std::string* pOutput)
   {
      return (*this)(
               reinterpret_cast<const Byte*>(input.c_str()),
               input.size(),
               pOutput);
   }

   Error operator()(const char* pEncoded,
                    std::size_t n,
                    std::string* pOutput)
   {
      return (*this)(
               reinterpret_cast<const Byte*>(pEncoded),
               n,
               pOutput);
   }
   
   Error operator()(const Byte* pEncoded,
                    std::size_t n,
                    std::string* pOutput)
   {
      if (n % 4 != 0)
         return decodeLengthError(n, ERROR_LOCATION);

      std::size_t size = decoded_size(n);
      boost::scoped_array<Byte> pBuffer(new Byte[size + 1]);
      pBuffer.get()[size] = '\0';
      Byte* it = pBuffer.get();

      Byte* pTable = &table_[0];
      
      Byte lhsByte, rhsByte;
      while (n != 4)
      {
         lhsByte = pTable[pEncoded[0]];
         if (UNLIKELY(invalid(lhsByte)))
            return decodeByteError(pEncoded[0], ERROR_LOCATION);
         
         rhsByte = pTable[pEncoded[1]];
         if (UNLIKELY(invalid(rhsByte)))
            return decodeByteError(pEncoded[1], ERROR_LOCATION);
         
         *it++ = (lhsByte << 2) | (rhsByte >> 4);

         lhsByte = pTable[pEncoded[2]];
         if (UNLIKELY(invalid(lhsByte)))
            return decodeByteError(pEncoded[2], ERROR_LOCATION);
         
         *it++ = (rhsByte << 4) | (lhsByte >> 2);

         rhsByte = pTable[pEncoded[3]];
         if (UNLIKELY(invalid(rhsByte)))
            return decodeByteError(pEncoded[3], ERROR_LOCATION);
         
         *it++ = (lhsByte << 6) | rhsByte;

         n -= 4;
         pEncoded += 4;
      }
      
      lhsByte = pTable[pEncoded[0]];
      if (UNLIKELY(invalid(lhsByte)))
         return decodeByteError(pEncoded[0], ERROR_LOCATION);
      
      rhsByte = pTable[pEncoded[1]];
      if (UNLIKELY(invalid(rhsByte)))
         return decodeByteError(pEncoded[1], ERROR_LOCATION);
      
      *it++ = (lhsByte << 2) | (rhsByte >> 4);

      if (pEncoded[2] == '=')
      {
         size -= 2;
         goto FINISH;
      }

      lhsByte = pTable[pEncoded[2]];
      if (UNLIKELY(invalid(lhsByte)))
         return decodeByteError(pEncoded[2], ERROR_LOCATION);
      
      *it++ = (rhsByte << 4) | (lhsByte >> 2);

      if (pEncoded[3] == '=')
      {
         size -= 1;
         goto FINISH;
      }

      rhsByte = pTable[pEncoded[3]];
      if (UNLIKELY(invalid(rhsByte)))
         return decodeByteError(pEncoded[3], ERROR_LOCATION);

      *it++ = (lhsByte << 6) | rhsByte;

FINISH:
      pOutput->assign(
               reinterpret_cast<const char*>(pBuffer.get()),
               size);
      return Success();
   }

private:
   std::vector<Byte> table_;
};

} // end anonymous namespace

Error decode(const std::string& input, std::string* pOutput)
{
   Decoder decode;
   return decode(input, pOutput);
}

Error decode(const char* pData, std::size_t n, std::string* pOutput)
{
   Decoder decode;
   return decode(pData, n, pOutput);
}

} // namespace base64
} // namespace core
} // namespace rstudio
