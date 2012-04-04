/*
 * Markdown.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/markdown/Markdown.hpp>

#include <iostream>

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include "sundown/markdown.h"

namespace core {
namespace markdown {

namespace {

void printVersion()
{
   int major, minor, revision;
   ::sd_version(&major, &minor, &revision);

   std::cerr << major << "." << minor << "."<< revision << std::endl;
}

} // anonymous namespace

Error markdownToHTML(const FilePath& markdownFile, const FilePath& outputFile)
{

   printVersion();

   return Success();
}

} // namespace markdown
} // namespace core
   



