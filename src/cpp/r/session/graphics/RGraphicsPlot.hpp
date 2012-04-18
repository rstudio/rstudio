/*
 * RGraphicsPlot.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef R_SESSION_GRAPHICS_PLOT_HPP
#define R_SESSION_GRAPHICS_PLOT_HPP

#include <string>

#include <boost/utility.hpp>

#include <core/FilePath.hpp>

#include <core/json/Json.hpp>

#include <r/RSexp.hpp>

#include "RGraphicsTypes.hpp"
#include "RGraphicsPlotManipulator.hpp"

namespace core {
   class Error;
   class FilePath;
}

namespace r {
namespace session {
namespace graphics {
   
// plot storage data structure
class Plot : boost::noncopyable
{
public:
   Plot(const GraphicsDeviceFunctions& graphicsDevice,
        const core::FilePath& baseDirPath,
        SEXP manipulatorSEXP);
   
   Plot(const GraphicsDeviceFunctions& graphicsDevice,
        const core::FilePath& baseDirPath, 
        const std::string& storageUuid,
        const DisplaySize& renderedSize);
   
   std::string storageUuid() const;  
   bool hasValidStorage() const;
   const DisplaySize& renderedSize() const { return renderedSize_; }

   bool hasManipulator() const;
   SEXP manipulatorSEXP() const;
   void manipulatorAsJson(core::json::Value* pValue) const;
   void saveManipulator() const;
   
   void invalidate();
   
   core::Error renderFromDisplay();
   core::Error renderFromDisplaySnapshot(SEXP snapshot);
   std::string imageFilename() const;
   
   core::Error renderToDisplay();
   
   core::Error removeFiles();

   void purgeInMemoryResources();
   
private:
   bool hasStorage() const;

   core::FilePath snapshotFilePath() const ;
   core::FilePath snapshotFilePath(const std::string& storageUuid) const;
   core::FilePath imageFilePath(const std::string& storageUuid) const;

   bool hasManipulatorFile() const;
   core::FilePath manipulatorFilePath(const std::string& storageUuid) const;
   void loadManipulatorIfNecessary() const;
   void saveManipulator(const std::string& storageUuid) const;

private:
   GraphicsDeviceFunctions graphicsDevice_;
   core::FilePath baseDirPath_;
   std::string storageUuid_ ;
   DisplaySize renderedSize_ ;
   bool needsUpdate_;

   // manipulator and protection scope for it
   mutable PlotManipulator manipulator_;
};

} // namespace graphics
} // namespace session
} // namespace r


#endif // R_SESSION_GRAPHICS_PLOT_HPP 

