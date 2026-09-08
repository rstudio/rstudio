/*
 * ZlibUtil.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/ZlibUtil.hpp>

#include <cstring> // for memcpy
#include <istream>
#include <stdexcept>

#include <core/Log.hpp>

#include <zlib.h>

namespace rstudio {
namespace core {
namespace zlib {

namespace
{
class ByteBuffer
{
public:
   ByteBuffer(size_t length)
   {
      buffer_ = new Bytef[length];
      bufferLen_ = length;
      dataLen_ = 0;
   }

   ~ByteBuffer()
   {
      if (NULL != buffer_)
         delete[] buffer_;
   }

   void append(const std::string& data)
   {
      size_t len = data.size() + 1;
      resize(len + dataLen_);
      std::memcpy(buffer_ + dataLen_, data.c_str(), len);
      dataLen_ += len;
   }

   void append(const unsigned char* data, size_t len)
   {
      resize(len + dataLen_);
      std::memcpy(buffer_ + dataLen_, data, len);
      dataLen_ += len;
   }

   void resize(size_t newLength)
   {
      if (newLength > bufferLen_)
      {
         Bytef* temp = buffer_;
         buffer_ = new Bytef[newLength];
         std::memcpy(buffer_, temp, dataLen_);
         delete[] temp;
         bufferLen_ = newLength;
      }
   }

   size_t dataSize() const
   {
      return dataLen_;
   }

   size_t bufferSize() const
   {
      return bufferLen_;
   }

   Bytef* get()
   {
      return buffer_;
   }

   std::string toString() const
   {
      return std::string(reinterpret_cast<char*>(buffer_));
   }

   void setDataSize(size_t dataSize)
   {
      dataLen_ = dataSize;
   }

private:
   Bytef* buffer_;
   size_t bufferLen_;
   size_t dataLen_;
};

inline void updateOut(ByteBuffer* pDestBuff, size_t* pRemainOut, z_streamp pzStream)
{
   if (pzStream->avail_out == 0)
   {
      if (*pRemainOut == 0)
      {
         pDestBuff->resize(pDestBuff->bufferSize() * 2);
         *pRemainOut += (pDestBuff->bufferSize() - pDestBuff->dataSize());
      }

      if (*pRemainOut > UINT32_MAX)
      {
         pzStream->avail_out = UINT32_MAX;
         pzStream->next_out = pDestBuff->get() + pDestBuff->dataSize();
         *pRemainOut -= UINT32_MAX;
      }
      else if (*pRemainOut > 0)
      {
         // Safe since we checked remain_out <= UINT32_MAX
         pzStream->avail_out = static_cast<uInt>(*pRemainOut);
         pzStream->next_out = pDestBuff->get() + pDestBuff->dataSize();
         *pRemainOut = 0;
      }
   }
}

inline void updateIn(ByteBuffer* pSrcBuff, size_t* pRemainIn, z_streamp pzStream)
{
   if ((pzStream->avail_in == 0) && (*pRemainIn > 0))
   {
      if (*pRemainIn > UINT32_MAX)
      {
         pzStream->avail_in = UINT32_MAX;
         *pRemainIn -= UINT32_MAX;
      }
      else
      {
         pzStream->avail_in = static_cast<uInt>(*pRemainIn);
         *pRemainIn = 0;
      }

      pzStream->next_in = pSrcBuff->get() + UINT32_MAX;
   }
}

inline void makeZStream(ByteBuffer& srcBuff,
                        ByteBuffer& destBuff,
                        size_t* pRemainIn,
                        size_t* pRemainOut,
                        z_streamp pzStream)
{
   if (srcBuff.dataSize() > UINT32_MAX)
      *pRemainIn = srcBuff.dataSize() - UINT32_MAX;
   else
      *pRemainIn = 0;

   if (destBuff.bufferSize() > UINT32_MAX)
      *pRemainOut = destBuff.bufferSize() - UINT32_MAX;
   else
      *pRemainOut = 0;

   pzStream->zalloc = Z_NULL;
   pzStream->zfree = Z_NULL;
   pzStream->opaque = Z_NULL;
   pzStream->avail_in = (*pRemainIn > 0) ? UINT32_MAX : static_cast<uInt>(srcBuff.dataSize());
   pzStream->next_in = srcBuff.get();
   pzStream->avail_out = (*pRemainOut > 0) ? UINT32_MAX : static_cast<uInt>(destBuff.bufferSize());
   pzStream->next_out = destBuff.get();
}

} // anonymous namespace

Error compressString(const std::string& toCompress, std::vector<unsigned char>* compressedData)
{
   if (toCompress.empty())
   {
      *compressedData = std::vector<unsigned char>();
      return Success();
   }

   size_t dataLen = toCompress.size();
   ByteBuffer srcBuff(dataLen);
   ByteBuffer destBuff(dataLen);

   srcBuff.append(toCompress);

   z_stream zStream;
   size_t remainIn, remainOut;
   makeZStream(srcBuff, destBuff, &remainIn, &remainOut, &zStream);

   int res = deflateInit(&zStream, Z_BEST_COMPRESSION);
   if (res != Z_OK)
      return systemError(res, "ZLib initialization error", ERROR_LOCATION);

   while (res != Z_STREAM_END)
   {
      updateOut(&destBuff, &remainOut, &zStream);
      res = deflate(&zStream, Z_FINISH);
      destBuff.setDataSize(zStream.total_out);
      if (res == Z_STREAM_ERROR)
      {
         deflateEnd(&zStream);
         LOG_DEBUG_MESSAGE("Could not compress string \"" +
                           toCompress + "\"");
         return systemError(res, "ZLib deflation error", ERROR_LOCATION);
      }
      updateIn(&srcBuff, &remainIn, &zStream);
   }

   deflateEnd(&zStream);

   compressedData->assign(destBuff.get(), destBuff.get() + destBuff.dataSize());
   return Success();
}

Error decompressString(const std::vector<unsigned char>& compressedData,
                       std::string* str)
{
   if (compressedData.empty())
   {
      *str = "";
      return Success();
   }

   size_t dataLen = compressedData.size();
   ByteBuffer srcBuff(dataLen);
   srcBuff.append(compressedData.data(), dataLen);

   // It's unlikely we were able to compress the original data to < 1/2 of it's size.
   // If we did, we can make multiple decompression passes.
   ByteBuffer destBuff(dataLen * 2);

   z_stream zStream;
   size_t remainIn, remainOut;
   makeZStream(srcBuff, destBuff, &remainIn, &remainOut, &zStream);

   int res = inflateInit(&zStream);
   if (res != Z_OK)
      return systemError(res, "ZLib initialization error", ERROR_LOCATION);

   while (res != Z_STREAM_END)
   {
      updateOut(&destBuff, &remainOut, &zStream);

      res = inflate(&zStream, Z_NO_FLUSH);
      destBuff.setDataSize(zStream.total_out);
      if ((res == Z_DATA_ERROR) || (res == Z_NEED_DICT) || (res == Z_MEM_ERROR) ||
          (res == Z_BUF_ERROR && zStream.avail_in == 0 && remainIn == 0))
      {
         inflateEnd(&zStream);
         LOG_DEBUG_MESSAGE("Could not decompress data\"" +
                           std::string(reinterpret_cast<const char*>(compressedData.data()), dataLen) +
                           "\"");
         return systemError(res, "ZLib inflation error", ERROR_LOCATION);
      }

      updateIn(&srcBuff, &remainIn, &zStream);
   }

   inflateEnd(&zStream);

   *str = destBuff.toString();
   return Success();
}

struct GzipDecompressingStreambuf::Impl
{
   explicit Impl(std::istream& source)
      : source(source),
        sourceEof(false),
        memberComplete(false),
        finished(false)
   {
      std::memset(&stream, 0, sizeof(stream));
   }

   std::istream& source;
   z_stream stream;
   bool sourceEof;
   bool memberComplete;
   bool finished;
   char input[8192];
   char output[8192];
};

GzipDecompressingStreambuf::GzipDecompressingStreambuf(std::istream& source)
   : pImpl_(new Impl(source))
{
   // 15 is the maximum window size; adding 16 selects gzip framing (zlib
   // then also verifies each member's trailer CRC on decompression)
   int result = inflateInit2(&pImpl_->stream, 15 + 16);
   if (result != Z_OK)
      throw std::runtime_error(std::string("zlib initialization failed: ") + zError(result));
}

GzipDecompressingStreambuf::~GzipDecompressingStreambuf()
{
   inflateEnd(&pImpl_->stream);
}

GzipDecompressingStreambuf::int_type GzipDecompressingStreambuf::underflow()
{
   if (gptr() < egptr())
      return traits_type::to_int_type(*gptr());

   if (pImpl_->finished)
      return traits_type::eof();

   z_stream& stream = pImpl_->stream;

   for (;;)
   {
      // refill the input buffer when it is empty
      if (stream.avail_in == 0 && !pImpl_->sourceEof)
      {
         pImpl_->source.read(pImpl_->input, sizeof(pImpl_->input));
         if (pImpl_->source.bad())
            throw std::runtime_error("error reading compressed stream");

         if (pImpl_->source.eof())
            pImpl_->sourceEof = true;

         stream.next_in = reinterpret_cast<Bytef*>(pImpl_->input);
         stream.avail_in = static_cast<uInt>(pImpl_->source.gcount());
      }

      // at a member boundary, either finish (nothing follows) or expect
      // another concatenated gzip member
      if (pImpl_->memberComplete)
      {
         if (stream.avail_in == 0 && pImpl_->sourceEof)
         {
            pImpl_->finished = true;
            return traits_type::eof();
         }

         int result = inflateReset(&stream);
         if (result != Z_OK)
            throw std::runtime_error(std::string("zlib reset failed: ") + zError(result));

         pImpl_->memberComplete = false;
      }

      stream.next_out = reinterpret_cast<Bytef*>(pImpl_->output);
      stream.avail_out = sizeof(pImpl_->output);

      int result = inflate(&stream, Z_NO_FLUSH);
      std::size_t produced = sizeof(pImpl_->output) - stream.avail_out;

      if (result == Z_STREAM_END)
      {
         pImpl_->memberComplete = true;
      }
      else if (result == Z_BUF_ERROR)
      {
         // inflate could make no progress: recoverable by supplying more
         // input, so with the source exhausted the stream is truncated
         if (stream.avail_in == 0 && pImpl_->sourceEof)
            throw std::runtime_error("gzip stream is truncated");
      }
      else if (result != Z_OK)
      {
         const char* msg = (stream.msg != nullptr) ? stream.msg : zError(result);
         throw std::runtime_error(std::string("gzip decompression failed: ") + msg);
      }

      if (produced > 0)
      {
         setg(pImpl_->output, pImpl_->output, pImpl_->output + produced);
         return traits_type::to_int_type(*gptr());
      }
   }
}

} // namespace zlib
} // namespace core
} // namespace rstudio
