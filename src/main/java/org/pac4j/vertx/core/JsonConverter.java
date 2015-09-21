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
package org.pac4j.vertx.core;

/**
 * Interface for encoding and decoding objects for the event bus. This is required for
 * session attributes and the user profile.
 *
 * @author Michael Remond
 * @since 1.1.0
 *
 */
public interface JsonConverter {

  /**
   * Encode the given object in a compatible form for the event bus.
   *
   * @param value the value to encode
   * @return the encoded object
   */
  Object encodeObject(Object value);

  /**
   * Decode the given object encoded with the encodeObject method.
   *
   * @param value the value to decode
   * @return the decoded object
   */
  Object decodeObject(Object value);
}