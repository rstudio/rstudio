/*
 * RClientState.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#ifndef R_SESSION_CLIENT_STATE_HPP
#define R_SESSION_CLIENT_STATE_HPP

#include <string>

#include <boost/utility.hpp>

#include <shared_core/FilePath.hpp>
#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace core {
   class Error;
} 
}

namespace rstudio {
namespace r {
namespace session {

// singleton
class ClientState;
ClientState& clientState();

enum ClientStateCommitType
{
   ClientStateCommitAll,
   ClientStateCommitPersistentOnly,
};
   
class ClientState : boost::noncopyable
{
private:   
   ClientState();
   friend ClientState& clientState();
     
public:
   
   void clear();
   
   void putTemporary(const std::string& scope, 
                     const std::string& name,
                     const core::json::Value& value);
   
   void putTemporary(const core::json::Object& temporaryState);
   
   void putPersistent(const std::string& scope, 
                      const std::string& name,
                      const core::json::Value& value);
   
   void putPersistent(const core::json::Object& persistentState);

   void putProjectPersistent(const std::string& scope,
                             const std::string& name,
                             const core::json::Value& value);

   void putProjectPersistent(const core::json::Object& projectPersistentState);
   core::json::Value getProjectPersistent(std::string scope,
                                          std::string name);

   core::Error commit(ClientStateCommitType commitType,
                      const core::FilePath& stateDir,
                      const core::FilePath& projectStateDir);
   
   core::Error restore(const core::FilePath& stateDir,
                       const core::FilePath& projectStateDir);
   
   void currentState(core::json::Object* pCurrentState) const;
   
private:
   void restoreGlobalState(const core::FilePath& stateFile);
   void restoreProjectState(const core::FilePath& stateFile);

private:
   core::json::Object temporaryState_;
   core::json::Object persistentState_;
   core::json::Object projectPersistentState_;
};

} // namespace session
} // namespace r
} // namespace rstudio

#endif // R_SESSION_CLIENT_STATE_HPP

