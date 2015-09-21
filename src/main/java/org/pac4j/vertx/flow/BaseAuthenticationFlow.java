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
package org.pac4j.vertx.flow;

import io.vertx.core.Vertx;
import org.pac4j.core.client.Client;
import org.pac4j.vertx.handler.HttpActionHandler;
import org.pac4j.vertx.handler.impl.DefaultHttpActionHandler;

/**
 * @author jez
 */
public abstract class BaseAuthenticationFlow<T extends Client> implements AuthenticationFlow<T> {

  protected final HttpActionHandler httpActionHandler;
  protected final Vertx vertx;
  protected final T client;

  public BaseAuthenticationFlow(Vertx vertx, T client) {
    this.httpActionHandler = new DefaultHttpActionHandler();
    this.vertx = vertx;
    this.client = client;
  }
}
