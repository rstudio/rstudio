/*
 * Tutorial.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 */

#ifndef SESSION_PRESENTATION_TUTORIAL_HPP
#define SESSION_PRESENTATION_TUTORIAL_HPP

#include <string>
#include <vector>

#include <core/FilePath.hpp>

namespace rstudio {
namespace session {
namespace modules { 
namespace presentation {

struct Tutorial
{
   Tutorial() : caption("Tutorial") {}
   core::FilePath slidesPath;
   std::string caption;
   std::vector<std::string> depends;
   std::vector<std::string> installFiles;
   std::vector<std::string> installReadonlyFiles;
};

core::Error initializeTutorial();

} // namespace presentation
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_PRESENTATION_TUTORIAL_HPP
