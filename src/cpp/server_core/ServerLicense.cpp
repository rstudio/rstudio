/*
 * ServerLicense.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 */

#include <server_core/ServerLicense.hpp>

#include <shared_core/Error.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server_core {
namespace license {

bool isProfessionalEdition()
{
   return false;
}

Error initialize()
{
   return Success();
}

} // namespace license
} // namespace server
} // namespace rstudio
