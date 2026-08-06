/*
 * SqlPreprocessor.cpp
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

#include <core/SqlPreprocessor.hpp>

#include <core/QueryBuilder.hpp>
#include <core/SqlIdentifier.hpp>

#include <sstream>
#include <utility>
#include <system_error>

#define SQLP_ERROR(msg) Error(msg, boost::system::errc::invalid_argument, ERROR_LOCATION)
#define SQLP_UNEXPECTED(msg) Unexpected(SQLP_ERROR(msg))

namespace rstudio {
namespace core {
namespace database {

namespace {

enum EmitState
{
   Normal,
   IfTrue,
   IfFalse,
   ElseTrue,
   ElseFalse,
};

using Tokens = std::vector<std::string>;
using TokensIter = Tokens::const_iterator;

class ExpressionParser
{
private:
   struct State
   {
      State() : value(false) {}
      State(bool value, TokensIter next) : value(value), next(next) {}
      State(const State&) = default;
      State(State&&) = default;
      State& operator=(const State&) = default;
      State& operator=(State&&) = default;

      bool value;
      TokensIter next;
   };

public:
   ExpressionParser(DatabaseConnection dbConnection)
   : dbConnection(dbConnection)
   {
      // initializers only
   }

   static Tokens tokenize(const std::string& expr)
   {
      Tokens tokens;
      int len = expr.size();
      std::string token;
      for (int i = 0; i < len; i++)
      {
         char ch = expr[i];
         if (ch == ' ' || ch == '\t' || ch == '!' || ch == ')' || ch == ',')
         {
            if (!token.empty())
               tokens.push_back(token);
            if (ch != ' ' && ch != '\t')
               tokens.push_back(std::string(1, ch));
            token.clear();
            continue;
         }
         if (ch >= 'A' && ch <= 'Z')
            ch = ch + ('a' - 'A');
         token += ch;
         if (token == "&&" || token == "||" || ch == '(')
         {
            tokens.push_back(token);
            token.clear();
         }
      }

      if (!token.empty())
         tokens.push_back(token);

      len = tokens.size();
      for (int i = 0; i < len; i++)
      {
         if (tokens[i] == "true")
            tokens[i] = "1";
         else if (tokens[i] == "false")
            tokens[i] = "0";
      }
      return tokens;
   }

   Result<bool> evaluate(const std::string& expr)
   {
      Tokens tokens = tokenize(expr);
      auto result = evaluate(tokens.begin(), tokens.end());
      if (!result)
         return Unexpected(result.error());
      if (result->next != tokens.end())
         return SQLP_UNEXPECTED("Unexpected tokens after expression: " + expr);
      return result->value;
   }

private:
   static Result<State> evaluate(DatabaseConnection dbConnection, const TokensIter& begin, const TokensIter& end)
   {
      ExpressionParser sql(dbConnection);
      return sql.evaluate(begin, end);
   }

   Result<State> evaluate(const TokensIter& begin, const TokensIter& end)
   {
      if (begin == end)
         return SQLP_UNEXPECTED("Incomplete expression");

      hasValue = false;
      for (auto iter = begin; iter < end; iter++)
      {
         std::string token = *iter;
         if (token == "&&")
            return doBinary(iter, end, doAnd);
         if (token == "||")
            return doBinary(iter, end, doOr);
         if (token == "!")
            return doUnary(iter, end, doInvert);

         Result<State> nextState;
         if (token == "0")
            nextState = setValue(token, State(false, iter));
         else if (token == "1")
            nextState = setValue(token, State(true, iter));
         else if (token == "(")
            nextState = doParenthesized(iter, end);
         else if (token[token.size() - 1] == '(')
            nextState = doFunction(iter, end);
         else
            return SQLP_UNEXPECTED("Unknown token " + token);

         if (!nextState)
            return Unexpected(nextState.error());
         iter = nextState->next;
      }
      return State(value, end);
   }

   static bool doInvert(bool v) { return !v; }
   static bool doAnd(bool l, bool r) { return l && r; }
   static bool doOr(bool l, bool r) { return l || r; }

   Result<State> doUnary(const TokensIter& iter, const TokensIter& end, bool(*func)(bool))
   {
      if (hasValue)
         return SQLP_UNEXPECTED("Unexpected " + *iter);
      auto result = evaluate(dbConnection, iter + 1, end);
      if (!result)
         return Unexpected(result.error());
      result->value = func(result->value);
      return result;
   }

   Result<State> doBinary(const TokensIter& iter, const TokensIter& end, bool(*func)(bool, bool))
   {
      if (!hasValue)
         return SQLP_UNEXPECTED("Unexpected " + *iter);
      auto result = evaluate(dbConnection, iter + 1, end);
      if (!result)
         return Unexpected(result.error());
      result->value = func(value, result->value);
      return result;
   }

   Result<State> setValue(const std::string& token, Result<State> state)
   {
      if (hasValue)
         return SQLP_UNEXPECTED("Unexpected " + token);
      if (!state)
         return state;
      value = state->value;
      hasValue = true;
      return State(value, state->next);
   }

   Result<State> doParenthesized(const TokensIter& begin, const TokensIter& end)
   {
      TokensIter iter = begin;
      while (++iter != end)
      {
         if (*iter == ")")
         {
            return setValue(*begin, evaluate(dbConnection, begin + 1, iter));
         }
      }
      return SQLP_UNEXPECTED("Unmatched parentheses");
   }

   Result<State> doFunction(const TokensIter& begin, const TokensIter& end)
   {
      TokensIter iter = begin;
      Tokens args;
      while (++iter != end)
      {
         std::string token = *iter;
         if (token == "," || token == ")")
            return SQLP_UNEXPECTED("Unexpected " + token);
         args.push_back(token);
         ++iter;
         if (iter == end || *iter == ")")
            break;
         if (*iter != ",")
            return SQLP_UNEXPECTED("Unexpected " + *iter);
      }
      if (iter == end)
         return SQLP_UNEXPECTED("Unmatched parentheses");

      std::string token = *begin;
      if (token == "exists(")
         return setValue(token, checkExists(args, iter));
      else if (token == "driver(")
         return setValue(token, checkDriver(args, iter));
      else
         return SQLP_UNEXPECTED("unknown preprocessor function: " + token);
   }

   Result<State> checkDriver(const Tokens& args, const TokensIter& end)
   {
      if (args.size() != 1)
         return SQLP_UNEXPECTED("Expected 1 parameter to driver()");
      return State(args[0] == dbConnection->driverName(), end);
   }

   Result<State> checkExists(const Tokens& args, const TokensIter& end)
   {
      std::string type;
      std::string rawName;
      if (args.size() == 1)
      {
         type = "table";
         rawName = args[0];
      }
      else if (args.size() == 2)
      {
         auto typeResult = SqlIdentifier::from(args[0]);
         if (!typeResult)
            return Unexpected(typeResult.error());
         type = *typeResult;
         rawName = args[1];
      }
      else
      {
         return SQLP_UNEXPECTED("Expected 1 or 2 parameters to exists()");
      }
      Result<SqlIdentifier> nameResult;
      std::string column;
      if (type == "column")
      {
         std::size_t dotPos = rawName.find('.');
         if (dotPos == std::string::npos)
            return SQLP_UNEXPECTED("Column expression expected in exists(column, ...)");
         auto columnResult = SqlIdentifier::from(rawName.substr(dotPos + 1));
         if (!columnResult)
            return Unexpected(columnResult.error());
         column = *columnResult;
         nameResult = SqlIdentifier::from(rawName.substr(0, dotPos));
      }
      else
      {
         nameResult = SqlIdentifier::from(rawName);
      }
      if (!nameResult)
         return Unexpected(nameResult.error());
      std::string name = *nameResult;
      SqlIdentifier schema;
      Rowset rows;
      bool isSqlite = (dbConnection->driver() == database::Driver::Sqlite);
      if (isSqlite && type == "column")
      {
         // intentionally loophole the identifier validation: this has already been validated
         schema = SqlIdentifier(("pragma_table_info('" + name + "')").c_str());
      }
      else if (isSqlite)
         schema = "sqlite_master";
      else if (type == "table" || type == "view")
         schema = "information_schema.tables";
      else if (type == "index")
         schema = "pg_catalog.pg_indexes";
      else if (type == "trigger")
         schema = "information_schema.triggers";
      else if (type == "column")
         schema = "information_schema.columns";
      else
         return SQLP_UNEXPECTED("Unknown object type " + type);
      SelectBuilder select(dbConnection, schema);
      if (isSqlite && type == "column")
         select.add("name").where("name", column);
      else if (isSqlite)
         select.add("name").where("name", name).where("type", type);
      else if (type == "table" || type == "view")
         select.add("table_name").where("table_name", name).where("table_type", (type == "table") ? "BASE TABLE" : "VIEW");
      else if (type == "index")
         select.add("indexname").where("indexname", name);
      else if (type == "trigger")
         select.add("trigger_name").where("trigger_name", name);
      else if (type == "column")
         select.add("column_name").where("table_name", name).where("column_name", column);
      Query query = select.build();
      Error error = dbConnection->execute(query, rows);
      if (error)
         return Unexpected(error);
      return State(rows.begin() != rows.end(), end);
   }

   DatabaseConnection dbConnection;
   bool hasValue;
   bool value;
};

} // anonymous namespace

Result<std::string> preprocessSchemaFile(DatabaseConnection dbConnection, const std::string& schema)
{
   std::ostringstream ss;

   ExpressionParser parser(dbConnection);
   std::vector<EmitState> ifStack({ Normal });
   EmitState emitting = Normal;

   std::size_t schemaLength = schema.size();
   std::size_t pos = 0;
   std::size_t tokenPos = std::string::npos;
   // Scan for preprocessor-style directives
   while (pos != std::string::npos && pos < schemaLength)
   {
      // Only recognize # at the beginning of a line
      if (schema[pos] == '#')
      {
         tokenPos = pos;
      }
      else
      {
         tokenPos = schema.find("\n#", pos);
         if (tokenPos != std::string::npos)
            tokenPos++;
      }
      if (tokenPos == std::string::npos)
         break;
      // Output text from before #
      if (emitting != IfFalse && emitting != ElseFalse)
         ss << schema.substr(pos, tokenPos - pos);
      // skip whitespace after #
      tokenPos = schema.find_first_not_of("\t ", tokenPos + 1);
      // Capture the line, ignoring whitespace before newline
      std::size_t nlPos = schema.find("\n", tokenPos);
      std::size_t lineEndPos = schema.find_last_not_of("\t\n\r ", nlPos);
      if (nlPos != std::string::npos)
         nlPos = nlPos + 1;
      if (lineEndPos < tokenPos)
         lineEndPos = tokenPos;
      if (lineEndPos == std::string::npos) // line contains only # and whitespace
      {
         pos = nlPos;
         continue;
      }
      std::string line = schema.substr(tokenPos, lineEndPos - tokenPos + 1);
      // Move the iterator to the end of the line (which might be the end of the string if npos)
      pos = nlPos;

      if (line == "endif")
      {
         ifStack.pop_back();
         if (ifStack.empty())
            return SQLP_UNEXPECTED("#endif without #if");
         emitting = ifStack.back();
      }
      else if (line == "else")
      {
         EmitState state = ifStack.back();
         ifStack.pop_back();
         if (ifStack.empty())
            return SQLP_UNEXPECTED("#else without #if");
         else if (state == ElseTrue || state == ElseFalse)
            return SQLP_UNEXPECTED("#else after #else");
         else if (state == IfTrue || ifStack.back() == IfFalse || ifStack.back() == ElseFalse)
            emitting = ElseFalse;
         else
            emitting = ElseTrue;
         ifStack.push_back(emitting);
      }
      else if (line.substr(0, 5) == "elif ")
      {
         EmitState state = ifStack.back();
         ifStack.pop_back();
         if (ifStack.empty())
            return SQLP_UNEXPECTED("#elif without #if");
         else if (state == ElseTrue || state == ElseFalse)
            return SQLP_UNEXPECTED("#elif after #else");
         else if (state == IfTrue || ifStack.back() == IfFalse || ifStack.back() == ElseFalse)
            emitting = IfFalse;
         else
         {
            auto result = parser.evaluate(line.substr(5));
            if (!result)
               return Unexpected(result.error());
            emitting = *result ? IfTrue : IfFalse;
         }
         ifStack.push_back(emitting);
      }
      else if (line.substr(0, 3) == "if ")
      {
         if (emitting == IfFalse || emitting == ElseFalse)
         {
            emitting = IfFalse;
         }
         else
         {
            auto result = parser.evaluate(line.substr(3));
            if (!result)
               return Unexpected(result.error());
            emitting = *result ? IfTrue : IfFalse;
         }
         ifStack.push_back(emitting);
      }
      else if (!line.empty())
      {
         return SQLP_UNEXPECTED("Unknown directive: #" + line);
      }
   }

   if (ifStack.size() != 1)
      return SQLP_UNEXPECTED("#if without #endif");

   // Output text from after last directive
   if (pos != std::string::npos)
      ss << schema.substr(pos);

   return ss.str();
}

} // namespace rstudio
} // namespace core
} // namespace database
