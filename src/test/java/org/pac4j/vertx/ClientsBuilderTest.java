package org.pac4j.vertx;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.junit.Test;
import org.pac4j.core.client.Clients;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * @author jez
 */
public class ClientsBuilderTest {

  @Test
  public void testClientsBuilder() throws IOException {
    final URL url = ClassLoader.getSystemClassLoader().getResource("clients.json");
    byte[] bytes = Files.readAllBytes(Paths.get(url.getPath()));
    Clients clients = ClientsBuilder.buildClients(new JsonObject((Map<String, Object>) Json.decodeValue(new String(
      bytes, "UTF-8"), Map.class)));
    System.out.println(clients);
  }


}