#ifndef DB_ACTIVE_SESSION_STORAGE_HPP
#define DB_ACTIVE_SESSION_STORAGE_HPP

#include "core/Database.hpp"
#include "core/r_util/RActiveSessionStorage.hpp"
#include "shared_core/Error.hpp"

namespace rstudio
{
namespace server
{
namespace storage
{

    using namespace core;
    using namespace core::r_util;
    
    class DBActiveSessionStorage : public IActiveSessionStorage 
    {
    public:
        explicit DBActiveSessionStorage();
        explicit DBActiveSessionStorage(boost::shared_ptr<core::database::IConnection> connection);
        ~DBActiveSessionStorage() = default;
        Error readProperty(const std::string& id, const std::string& name, std::string* pValue) override;   
        Error readProperties(const std::string& id, const std::set<std::string>& names, std::map<std::string, std::string>* pValues) override;
        Error readProperties(const std::string& id, std::map<std::string, std::string>* pValues) override;
        Error writeProperty(const std::string& id, const std::string& name, const std::string& value) override;
        Error writeProperties(const std::string& id, const std::map<std::string, std::string>& properties) override;
    private:
        boost::shared_ptr<core::database::IConnection> connection;
    };

namespace errc
{
    enum errc_t {
        Success = 0,
        DBError = 1,
        SessionNotFound = 2,
        TooManySessionsReturned = 3,
    };
}
} // namespace storage
} // namespace server
} // namespace rstudio

#endif