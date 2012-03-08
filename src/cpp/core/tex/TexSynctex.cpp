/*
 * TexSynctex.cpp
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

#include <core/tex/TexSynctex.hpp>

#include <core/Error.hpp>

#include "synctex/synctex_parser_utils.h"

namespace core {
namespace tex {

namespace {

void test()
{
   _synctex_malloc(1);
}

} // anonymous namespace
   
} // namespace tex
} // namespace core 



