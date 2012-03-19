/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.whirr.service.c4tgz.predicates;

import static org.apache.whirr.service.c4tgz.C4TgzConstants.MODULE_SOURCE_SUBKEY;
import static org.apache.whirr.service.c4tgz.C4TgzConstants.C4TGZ;
import static org.apache.whirr.service.c4tgz.C4TgzConstants.C4TGZ_ROLE_PREFIX;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

/**
 * 
 */
public class C4TgzPredicates {
  public static Predicate<String> isFirstC4TgzRoleIn(final Iterable<String> roles) {
    return new Predicate<String>() {

      @Override
      public boolean apply(String arg0) {
        return Iterables.get(Iterables.filter(roles, Predicates.containsPattern("^" + C4TGZ_ROLE_PREFIX + arg0)),
              0).equals(C4TGZ_ROLE_PREFIX + arg0);
      }

      @Override
      public String toString() {
        return "isFirstC4TgzRoleIn(" + roles + ")";

      }
    };

  }

  public static Predicate<CharSequence> isC4TgzRole() {
    return Predicates.containsPattern("^" + C4TGZ_ROLE_PREFIX);
  }

  public static Predicate<String> isLastC4TgzRoleIn(final Iterable<String> roles) {
    return new Predicate<String>() {

      @Override
      public boolean apply(String arg0) {
        return Iterables.getLast(
              Iterables.filter(roles, Predicates.containsPattern("^" + C4TGZ_ROLE_PREFIX + arg0))).equals(
              C4TGZ_ROLE_PREFIX + arg0);
      }

      @Override
      public String toString() {
        return "isLastC4TgzRoleIn(" + roles + ")";
      }
    };
  }

  public static Predicate<String> isModuleSubKey(final String module) {
    return new Predicate<String>() {

      @Override
      public boolean apply(String arg0) {
        return arg0.startsWith(C4TGZ + "." + module + "." + MODULE_SOURCE_SUBKEY);
      }

      @Override
      public String toString() {
        return "isModuleSubKey(" + module + ")";

      }
    };
  }
}

