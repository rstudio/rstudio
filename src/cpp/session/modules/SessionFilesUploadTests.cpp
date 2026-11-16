/*
 * SessionFilesUploadTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include "SessionFilesUpload.hpp"

#include <random>
#include <string>
#include <vector>

#include <gtest/gtest.h>

namespace rstudio {
namespace session {
namespace tests {

using core::Error;
using core::Success;
using modules::files::multipartBoundary;
using modules::files::UploadFormParser;

namespace {

const std::string kBoundary = "----formBoundary7MA4YWxkTrZu0gW";
const std::string kTargetDirectory = "/home/user/project";

// awkward but legal file contents: CRLFs, quotes, and a delimiter that stops
// one byte short of the boundary
const std::string kPayload =
      "hello\r\nworld\n\"quoted\" --not-a-boundary\r\n" +
      std::string("\r\n--") + kBoundary.substr(0, kBoundary.size() - 1) + "\r\n";

struct Parsed
{
   Error error;
   bool hasFile = false;
   std::string fileName;
   std::string targetDirectory;
   std::string payload;
};

std::string part(const std::string& disposition,
                 const std::string& value,
                 const std::string& extraHeaders = std::string())
{
   return "Content-Disposition: " + disposition + "\r\n" + extraHeaders + "\r\n" + value;
}

std::string filePart(const std::string& payload, const std::string& fileName = "hello.txt")
{
   return part("form-data; name=\"File\"; filename=\"" + fileName + "\"",
               payload,
               "Content-Type: text/plain\r\n");
}

std::string targetDirectoryPart(const std::string& directory = kTargetDirectory)
{
   return part("form-data; name=\"targetDirectory\"", directory);
}

std::string formBody(const std::vector<std::string>& parts,
                     const std::string& afterClose = "\r\n",
                     const std::string& padding = std::string())
{
   std::string body;
   for (std::size_t i = 0; i < parts.size(); i++)
   {
      if (i > 0)
         body += "\r\n";

      body += "--" + kBoundary + padding + "\r\n" + parts[i];
   }

   body += "\r\n--" + kBoundary + "--" + afterClose;
   return body;
}

// what the Files pane sends: the file input, then the hidden field
std::string browserBody(const std::string& payload = kPayload)
{
   return formBody({ filePart(payload), targetDirectoryPart() });
}

// parse `body` in chunks that end at each offset in `splits`
Parsed parseChunks(const std::string& body, const std::vector<std::size_t>& splits)
{
   Parsed parsed;
   UploadFormParser parser(kBoundary, [&](const char* data, std::size_t size)
   {
      parsed.payload.append(data, size);
      return Success();
   });

   std::size_t start = 0;
   for (std::size_t i = 0; i <= splits.size(); i++)
   {
      std::size_t end = (i < splits.size()) ? splits[i] : body.size();
      bool complete = (i == splits.size());
      parsed.error = parser.consume(std::string_view(body).substr(start, end - start), complete);
      if (parsed.error)
         break;

      start = end;
   }

   parsed.hasFile = parser.hasFile();
   parsed.fileName = parser.fileName();
   parsed.targetDirectory = parser.targetDirectory();
   return parsed;
}

Parsed parse(const std::string& body)
{
   return parseChunks(body, {});
}

bool sameResult(const Parsed& expected, const Parsed& actual)
{
   // a failing body may stream some file data before it fails
   if (expected.error)
      return static_cast<bool>(actual.error);

   return !actual.error &&
          actual.hasFile == expected.hasFile &&
          actual.fileName == expected.fileName &&
          actual.targetDirectory == expected.targetDirectory &&
          actual.payload == expected.payload;
}

// Parse `body` split into two chunks at every offset, then one byte at a
// time, and describe the first result that differs from parsing it whole.
// The upload handler gets chunks cut wherever the socket reads happened to
// land, so a delimiter can straddle any two chunks.
std::string firstSplitMismatch(const std::string& body)
{
   Parsed whole = parse(body);

   for (std::size_t split = 0; split <= body.size(); split++)
   {
      if (!sameResult(whole, parseChunks(body, { split })))
         return "split at offset " + std::to_string(split);
   }

   std::vector<std::size_t> everyByte;
   for (std::size_t offset = 1; offset < body.size(); offset++)
      everyByte.push_back(offset);

   if (!sameResult(whole, parseChunks(body, everyByte)))
      return "one byte at a time";

   return std::string();
}

std::string randomBytes(std::size_t size)
{
   std::mt19937 generator(18861);
   std::uniform_int_distribution<int> byte(0, 255);

   std::string bytes(size, '\0');
   for (char& ch : bytes)
      ch = static_cast<char>(byte(generator));

   return bytes;
}

} // anonymous namespace

TEST(SessionFilesUploadTest, ParsesTheBrowserForm)
{
   std::string body = browserBody();

   Parsed parsed = parse(body);
   ASSERT_FALSE(parsed.error);
   EXPECT_TRUE(parsed.hasFile);
   EXPECT_EQ("hello.txt", parsed.fileName);
   EXPECT_EQ(kTargetDirectory, parsed.targetDirectory);
   EXPECT_EQ(kPayload, parsed.payload);

   EXPECT_EQ("", firstSplitMismatch(body));
}

TEST(SessionFilesUploadTest, ParsesTheTargetDirectoryFirst)
{
   std::string body = formBody({ targetDirectoryPart(), filePart(kPayload) });

   Parsed parsed = parse(body);
   ASSERT_FALSE(parsed.error);
   EXPECT_EQ("hello.txt", parsed.fileName);
   EXPECT_EQ(kTargetDirectory, parsed.targetDirectory);
   EXPECT_EQ(kPayload, parsed.payload);

   EXPECT_EQ("", firstSplitMismatch(body));
}

TEST(SessionFilesUploadTest, ParsesAnEmptyFile)
{
   std::string body = browserBody("");

   Parsed parsed = parse(body);
   ASSERT_FALSE(parsed.error);
   EXPECT_TRUE(parsed.hasFile);
   EXPECT_EQ("hello.txt", parsed.fileName);
   EXPECT_EQ("", parsed.payload);

   EXPECT_EQ("", firstSplitMismatch(body));
}

TEST(SessionFilesUploadTest, ReportsNoFileChosenAsAnEmptyName)
{
   // a browser sends an empty filename when the file input was left empty
   Parsed parsed = parse(formBody({ filePart("", ""), targetDirectoryPart() }));
   ASSERT_FALSE(parsed.error);
   EXPECT_TRUE(parsed.hasFile);
   EXPECT_EQ("", parsed.fileName);
}

TEST(SessionFilesUploadTest, StreamsLargeFilesAcrossChunks)
{
   // mirror the upload handler: chunks of at least 1 MiB, then a final one
   // too short to hold the whole targetDirectory trailer
   std::string payload = randomBytes(3 * 1024 * 1024 + 12345);
   std::string body = browserBody(payload);
   std::size_t mib = 1024 * 1024;

   std::vector<std::vector<std::size_t>> chunkings = {
      { mib + 4321, 2 * mib + 9000, 3 * mib + 9000 },
      { mib, 2 * mib, 3 * mib, body.size() - 7 },
      { mib + 77, body.size() - 100, body.size() - 1 }
   };

   for (const std::vector<std::size_t>& splits : chunkings)
   {
      Parsed parsed = parseChunks(body, splits);
      ASSERT_FALSE(parsed.error);
      EXPECT_EQ(kTargetDirectory, parsed.targetDirectory);
      EXPECT_TRUE(parsed.payload == payload);
   }
}

TEST(SessionFilesUploadTest, AcceptsAnyEndingAfterTheCloseDelimiter)
{
   std::vector<std::string> endings = {
      "",
      "\r\n",
      "\r\nepilogue\r\n",
      "\r\n--" + kBoundary + "\r\nepilogue that looks like a part\r\n"
   };

   for (const std::string& ending : endings)
   {
      std::string body = formBody({ filePart(kPayload), targetDirectoryPart() }, ending);

      Parsed parsed = parse(body);
      ASSERT_FALSE(parsed.error);
      EXPECT_EQ(kTargetDirectory, parsed.targetDirectory);
      EXPECT_EQ(kPayload, parsed.payload);

      EXPECT_EQ("", firstSplitMismatch(body));
   }
}

TEST(SessionFilesUploadTest, IgnoresThePreamble)
{
   std::string body = "preamble\r\n\r\nwith blank lines\r\n" + browserBody();

   Parsed parsed = parse(body);
   ASSERT_FALSE(parsed.error);
   EXPECT_EQ(kPayload, parsed.payload);

   EXPECT_EQ("", firstSplitMismatch(body));
}

TEST(SessionFilesUploadTest, AcceptsTransportPadding)
{
   std::string body = formBody({ filePart(kPayload), targetDirectoryPart() }, "\r\n", " \t ");

   Parsed parsed = parse(body);
   ASSERT_FALSE(parsed.error);
   EXPECT_EQ(kTargetDirectory, parsed.targetDirectory);
   EXPECT_EQ(kPayload, parsed.payload);

   EXPECT_EQ("", firstSplitMismatch(body));
}

TEST(SessionFilesUploadTest, IgnoresOtherFields)
{
   std::string extra = part("form-data; name=\"extra\"", "zzz");
   std::vector<std::string> bodies = {
      formBody({ extra, filePart(kPayload), targetDirectoryPart() }),
      formBody({ filePart(kPayload), extra, targetDirectoryPart() }),
      formBody({ filePart(kPayload), targetDirectoryPart(), extra })
   };

   for (const std::string& body : bodies)
   {
      Parsed parsed = parse(body);
      ASSERT_FALSE(parsed.error);
      EXPECT_EQ(kTargetDirectory, parsed.targetDirectory);
      EXPECT_EQ(kPayload, parsed.payload);
   }
}

TEST(SessionFilesUploadTest, KeepsTheFirstTargetDirectory)
{
   Parsed parsed = parse(formBody({
      targetDirectoryPart("/first"),
      filePart(kPayload),
      targetDirectoryPart("/second")
   }));

   ASSERT_FALSE(parsed.error);
   EXPECT_EQ("/first", parsed.targetDirectory);
}

TEST(SessionFilesUploadTest, RejectsASecondFile)
{
   Parsed parsed = parse(formBody({ filePart("one"), filePart("two"), targetDirectoryPart() }));
   EXPECT_TRUE(parsed.error);
}

TEST(SessionFilesUploadTest, ParsesDispositionParametersPerRfc7578)
{
   std::vector<std::string> dispositions = {
      "Form-Data; Name=\"File\"; FileName=\"hello.txt\"",
      "form-data;name=\"File\";filename=\"hello.txt\"",
      "form-data; filename=\"hello.txt\"; name=\"File\"",
      "form-data;  name=File;  filename=hello.txt"
   };

   for (const std::string& disposition : dispositions)
   {
      Parsed parsed = parse(formBody({ part(disposition, kPayload), targetDirectoryPart() }));
      ASSERT_FALSE(parsed.error) << disposition;
      EXPECT_EQ("hello.txt", parsed.fileName) << disposition;
      EXPECT_EQ(kPayload, parsed.payload) << disposition;
   }

   // header names are case-insensitive too
   std::string lowercase = "content-disposition: form-data; name=\"targetDirectory\"\r\n\r\n" + kTargetDirectory;
   Parsed parsed = parse(formBody({ filePart(kPayload), lowercase }));
   ASSERT_FALSE(parsed.error);
   EXPECT_EQ(kTargetDirectory, parsed.targetDirectory);
}

TEST(SessionFilesUploadTest, UnescapesQuotedFileNames)
{
   Parsed parsed = parse(formBody({ filePart(kPayload, "a \\\"b\\\".txt"), targetDirectoryPart() }));
   ASSERT_FALSE(parsed.error);
   EXPECT_EQ("a \"b\".txt", parsed.fileName);
}

TEST(SessionFilesUploadTest, KeepsQuotesInTheTargetDirectory)
{
   // browsers escape quotes in file names, but send field values as-is
   std::string directory = "/home/user/x\"y";
   Parsed parsed = parse(formBody({ filePart(kPayload), targetDirectoryPart(directory) }));
   ASSERT_FALSE(parsed.error);
   EXPECT_EQ(directory, parsed.targetDirectory);
}

TEST(SessionFilesUploadTest, DropsDirectoriesFromFileNames)
{
   Parsed parsed = parse(formBody({ filePart(kPayload, "sub/dir/hello.txt"), targetDirectoryPart() }));
   ASSERT_FALSE(parsed.error);
   EXPECT_EQ("hello.txt", parsed.fileName);

   parsed = parse(formBody({ filePart(kPayload, "../../hello.txt"), targetDirectoryPart() }));
   ASSERT_FALSE(parsed.error);
   EXPECT_EQ("hello.txt", parsed.fileName);

   parsed = parse(formBody({ filePart(kPayload, "sub/"), targetDirectoryPart() }));
   ASSERT_FALSE(parsed.error);
   EXPECT_EQ("", parsed.fileName);
}

TEST(SessionFilesUploadTest, RejectsPartsThatAreNotNamedFormData)
{
   std::vector<std::string> parts = {
      "Content-Type: text/plain\r\n\r\nno disposition",
      "\r\nno headers at all",
      part("attachment; name=\"File\"; filename=\"hello.txt\"", kPayload),
      part("form-data; filename=\"hello.txt\"", kPayload)
   };

   for (const std::string& badPart : parts)
      EXPECT_TRUE(parse(formBody({ badPart, targetDirectoryPart() })).error) << badPart;
}

TEST(SessionFilesUploadTest, RejectsTransferEncodingsItCannotDecode)
{
   std::string base64 = part("form-data; name=\"File\"; filename=\"hello.txt\"",
                             "aGVsbG8=",
                             "Content-Transfer-Encoding: base64\r\n");
   EXPECT_TRUE(parse(formBody({ base64, targetDirectoryPart() })).error);

   std::string binary = part("form-data; name=\"File\"; filename=\"hello.txt\"",
                             kPayload,
                             "Content-Transfer-Encoding: Binary\r\n");
   Parsed parsed = parse(formBody({ binary, targetDirectoryPart() }));
   ASSERT_FALSE(parsed.error);
   EXPECT_EQ(kPayload, parsed.payload);

   // an ignored field is never decoded, so its encoding doesn't matter
   std::string ignored = part("form-data; name=\"extra\"",
                              "enp6",
                              "Content-Transfer-Encoding: base64\r\n");
   EXPECT_FALSE(parse(formBody({ filePart(kPayload), ignored, targetDirectoryPart() })).error);
}

TEST(SessionFilesUploadTest, RejectsABodyWithoutTheCloseDelimiter)
{
   std::string body = browserBody();
   std::size_t closeEnd = body.size() - 2;

   for (std::size_t size = 0; size < closeEnd; size++)
      EXPECT_TRUE(parse(body.substr(0, size)).error) << "truncated to " << size;

   EXPECT_FALSE(parse(body.substr(0, closeEnd)).error);
}

TEST(SessionFilesUploadTest, RejectsAMalformedDelimiterLine)
{
   std::string prefix = "--" + kBoundary;
   std::string rest = "\r\n" + filePart(kPayload) + "\r\n--" + kBoundary + "--\r\n";

   EXPECT_TRUE(parse(prefix + "x" + rest).error);
   EXPECT_TRUE(parse(prefix + " x" + rest).error);
   EXPECT_TRUE(parse(prefix + "-x" + rest).error);
}

TEST(SessionFilesUploadTest, LimitsHeaderAndFieldSizes)
{
   std::string longHeader = "X-Padding: " + std::string(70 * 1024, 'x') + "\r\n";
   std::string paddedPart = part("form-data; name=\"File\"; filename=\"hello.txt\"", kPayload, longHeader);
   EXPECT_TRUE(parse(formBody({ paddedPart, targetDirectoryPart() })).error);

   std::string longDirectory = "/" + std::string(70 * 1024, 'd');
   EXPECT_TRUE(parse(formBody({ filePart(kPayload), targetDirectoryPart(longDirectory) })).error);
}

TEST(SessionFilesUploadTest, StopsWhenTheSinkFails)
{
   int calls = 0;
   UploadFormParser parser(kBoundary, [&](const char*, std::size_t)
   {
      ++calls;
      return core::systemError(boost::system::errc::io_error, ERROR_LOCATION);
   });

   EXPECT_TRUE(parser.consume(browserBody(), true));
   EXPECT_EQ(1, calls);
}

TEST(SessionFilesUploadTest, FindsTheBoundaryInTheContentType)
{
   std::vector<std::pair<std::string, std::string>> cases = {
      { "multipart/form-data; boundary=abc", "abc" },
      { "multipart/form-data; boundary=\"abc def\"", "abc def" },
      { "multipart/form-data; boundary=abc; charset=utf-8", "abc" },
      { "Multipart/Form-Data; charset=utf-8; BOUNDARY=abc", "abc" },
      { "multipart/form-data; boundary=" + std::string(70, 'b'), std::string(70, 'b') }
   };

   for (const auto& testCase : cases)
   {
      std::string boundary;
      ASSERT_FALSE(multipartBoundary(testCase.first, &boundary)) << testCase.first;
      EXPECT_EQ(testCase.second, boundary) << testCase.first;
   }

   std::vector<std::string> invalid = {
      "multipart/form-data",
      "multipart/form-data; boundary=",
      "multipart/form-data; boundary=\"\"",
      "multipart/form-data; boundary=" + std::string(71, 'b'),
      "text/plain; boundary=abc"
   };

   for (const std::string& contentType : invalid)
   {
      std::string boundary;
      EXPECT_TRUE(multipartBoundary(contentType, &boundary)) << contentType;
   }
}

} // namespace tests
} // namespace session
} // namespace rstudio
