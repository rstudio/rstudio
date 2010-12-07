#ifndef JASON_SPIRIT_ESCAPE_STRING_HPP
#define JASON_SPIRIT_ESCAPE_STRING_HPP

// NOTE: json_spirit was not properly escaping mbcs strings coming from 
// R. As a result we defined this hook which allows us to provide the
// string escaping externally (see Json.cpp)

namespace json_spirit {

std::string write_escaped_string(const std::string& str);
   
#ifndef BOOST_NO_STD_WSTRING
   
std::wstring write_escaped_string(const std::wstring& str);
   
#endif

}   
   
#endif // JASON_SPIRIT_ESCAPE_STRING_HPP
