/*
 * ServerLicense.hpp
 *
 * Copyright (C) 2022 by RStudio, PBC
 *
 */

#ifndef SERVER_LICENSE_HPP
#define SERVER_LICENSE_HPP

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace server_core {
namespace license {

bool isProfessionalEdition();

core::Error initialize();

} // namespace license
} // namespace server
} // namespace rstudio

#endif // SERVER_LICENSE_HPP

