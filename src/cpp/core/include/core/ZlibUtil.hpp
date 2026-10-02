/*
 * ZlibUtil.hpp
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

#ifndef CORE_ZLIB_ZLIB_HPP
#define CORE_ZLIB_ZLIB_HPP

#include <iosfwd>
#include <memory>
#include <streambuf>
#include <string>

#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {
namespace zlib {

Error compressString(const std::string& toCompress, std::vector<unsigned char>* compressedData);

Error decompressString(const std::vector<unsigned char>& compressedData, std::string* str);

// Presents a gzip stream, read incrementally from an underlying std::istream,
// as a std::streambuf of decompressed bytes. Concatenated gzip members are
// decompressed in sequence, as with gzip itself.
//
// Errors -- a corrupt or truncated stream, a trailer CRC mismatch, or a
// failure reading the underlying stream -- are reported by throwing
// std::runtime_error; a std::istream reading through this streambuf
// surfaces that as badbit, distinguishing errors from a clean end of
// stream (eofbit). The constructor itself may also throw if zlib fails
// to initialize.
class GzipDecompressingStreambuf : public std::streambuf
{
public:
   explicit GzipDecompressingStreambuf(std::istream& source);
   virtual ~GzipDecompressingStreambuf();

   GzipDecompressingStreambuf(const GzipDecompressingStreambuf&) = delete;
   GzipDecompressingStreambuf& operator=(const GzipDecompressingStreambuf&) = delete;

protected:
   int_type underflow() override;

private:
   struct Impl;
   std::unique_ptr<Impl> pImpl_;
};

} // namespace zlib
} // namespace core
} // namespace rstudio

#endif
