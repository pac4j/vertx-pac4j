package org.pac4j.vertx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.pac4j.core.client.Clients;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Json;

@Ignore
public class ClientsBuilderTest {

    @Test
    public void testClientsBuilder() throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("src/test/resources/clients.json"));
        Clients clients = ClientsBuilder.buildClients(new JsonObject((Map<String, Object>) Json.decodeValue(new String(
                bytes, "UTF-8"), Map.class)));
        System.out.println(clients);
    }

}
