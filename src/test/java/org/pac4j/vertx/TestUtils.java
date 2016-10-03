/*
  Copyright 2015 - 2015 pac4j organization

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.pac4j.vertx;

import java.util.Objects;

/**
 * @author Jeremy Prime
 */
public class TestUtils {
    public static boolean isEqual(final Object a, final Object b) {
        if (Objects.isNull(a) && Objects.nonNull(b)) return false;
        if (Objects.nonNull(a) && Objects.isNull(b)) return false;
        if (Objects.isNull(a) && Objects.isNull(b)) return true;
        return a.equals(b);
    }
}
