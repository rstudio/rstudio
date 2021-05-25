/*
 * user.ts
 *
 * Copyright (C) 2021 by RStudio, PBC
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

import os from "os";
import { FilePath } from "./file-path";

/**
 * Class which represents a system user.
 */
export class User {
  /**
   * Gets the user home path.
   */
  static getUserHomePath(): FilePath {
    return new FilePath(os.homedir());
  }

  // #ifndef _WIN32

  //    /**
  //     * @brief Constructor.
  //     *
  //     * Creates a user object which is either empty or represents all users.
  //     *
  //     * @param in_isEmpty    True to create an empty user; False to create a user which represents all users.
  //     *                      Default: false.
  //     */
  //     explicit User(bool in_isEmpty = false);

  //    /**
  //     * @brief Copy constructor.
  //     *
  //     * @param in_other   The user to copy.
  //     */
  //    User(const User& in_other);

  //    /**
  //     * @brief Move constructor.
  //     *
  //     * @param in_other   The user to move into this User.
  //     */
  //    User(User&& in_other) noexcept = default;

  //    /**
  //     * @brief Gets the current user.
  //     *
  //     * @param out_currentUser    The user this process is currently executing on behalf of. This object will be the empty
  //     *                           user if this function returns an error.
  //     *
  //     * @return Success if the user could be retrieved; Error otherwise.
  //     */
  //    static Error getCurrentUser(User& out_currentUser);

  //    /**
  //     * @brief Gets a user from its username.
  //     *
  //     * @param in_username    The name of the user to create.
  //     * @param out_user       The created user.
  //     *
  //     * @return Success if the user could be retrieved; Error otherwise.
  //     */
  //    static Error getUserFromIdentifier(const std::string& in_username, User& out_user);

  //    /**
  //     * @brief Gets a user from its user ID.
  //     *
  //     * @param in_userId      The ID of the user to create.
  //     * @param out_user       The created user.
  //     *
  //     * @return Success if the user could be retrieved; Error otherwise.
  //     */
  //    static Error getUserFromIdentifier(UidType in_userId, User& out_user);

  //    /**
  //     * @brief Overloaded assignment operator.
  //     *
  //     * @param in_other   The user to copy to this one.
  //     *
  //     * @return This user.
  //     */
  //    User& operator=(const User& in_other);

  //    /**
  //     * @brief Overloaded move operator.
  //     *
  //     * @param in_other   The user to move to this one.
  //     *
  //     * @return This user.
  //     */
  //    User& operator=(User&& in_other) noexcept = default;

  //    /**
  //     * @brief Equality operator.
  //     *
  //     * @param in_other      The user to compare with this user.
  //     *
  //     * @return True if this user and in_other have the same user ID; false otherwise.
  //     */
  //    bool operator==(const User& in_other) const;

  //    /**
  //     * @brief Inequality operator.
  //     *
  //     * @param in_other      The user to compare with this user.
  //     *
  //     * @return False if this user and in_other have the same user ID; true otherwise.
  //     */
  //    bool operator!=(const User& in_other) const;

  //    /**
  //     * @brief Checks whether the user represented by this object exists.
  //     *
  //     * If this is an empty user, or is a user object which represents all users, this method will return false as it does
  //     * not represent a user which exists on the system.
  //     *
  //     * @return True if this user exists; false otherwise.
  //     */
  //    bool exists() const;

  //    /**
  //     * @brief Returns whether this object represents all users or not. See the default constructor for more details.
  //     *
  //     * @return True if this object represents all users; false otherwise.
  //     */
  //    bool isAllUsers() const;

  //    /**
  //     * @brief Checks whether this user is empty or not.
  //     *
  //     * @return True if this is user is empty; False otherwise.
  //     */
  //    bool isEmpty() const;

  //    /**
  //     * @brief Gets the user home path, if it exists.
  //     *
  //     * @return The user's home path, if it exists; empty path otherwise.
  //     */
  //    const FilePath& getHomePath() const;

  //    /**
  //     * @brief Gets the ID of this user's primary group.
  //     *
  //     * @return The ID of this user's primary group.
  //     */
  //    GidType getGroupId() const;

  //    /**
  //     * @brief Returns the login shell of this user.
  //     *
  //     * @return The login shell of this user.
  //     */
  //    const std::string& getShell() const;

  //    /**
  //     * @brief Gets the ID of this user.
  //     *
  //     * @return The ID of this user.
  //     */
  //    UidType getUserId() const;

  //    /**
  //     * @brief Returns the name of this user.
  //     *
  //     * @return The name of this user ("*" for all users).
  //     */
  //    const std::string& getUsername() const;

  // private:
  //    // The private implementation of User.
  //    PRIVATE_IMPL(m_impl);

  // #else
  //    // No construction on windows.
  //    User() = delete;

  // #endif
}
