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

package org.apache.whirr.service.c4tgz;

import java.util.regex.Pattern;


/**
 * 
 */
public class C4TgzConstants {
  public static final String C4TGZ = "c4tgz";
  public static final String C4TGZ_ROLE_PREFIX = C4TGZ + ":";
  public static final String MODULE_SOURCE_SUBKEY = "module";

  public static final String MODULES_DIR = "/var/tmp/c4cs";

  /** matches anything like:   c4tgz.foobar.module  ... where foobar is our module   */
  public static final Pattern MODULE_KEY_PATTERN = Pattern.compile("^" + C4TGZ + "\\.([^.]+)\\."
        + MODULE_SOURCE_SUBKEY + "$");
}
