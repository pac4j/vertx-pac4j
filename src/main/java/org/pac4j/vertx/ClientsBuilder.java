/*
  Copyright 2014 - 2014 Michael Remond

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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.vertx.java.core.json.JsonObject;

/**
 * Builder returning a {@link Clients} instance based on the Json configuration. This class uses introspection
 * in order to create and configure the java objects.
 * 
 * @author Michael Remond
 * @since 1.0.0
 *
 */
@SuppressWarnings("rawtypes")
public class ClientsBuilder {

    private static Map<Class, Class> map = new HashMap<>();

    static {
        map.put(int.class, Integer.class);
        map.put(long.class, Long.class);
        map.put(boolean.class, Boolean.class);
    }

    public static Clients buildClients(JsonObject conf) {
        String callbackUrl = conf.getString("callbackUrl");
        if (callbackUrl == null) {
            callbackUrl = "http://localhost:8080/callback";
        }
        List<Client> clients = new ArrayList<>();
        JsonObject cs = conf.getObject("clients");
        if (cs != null) {
            for (String name : cs.getFieldNames()) {
                clients.add((Client) buildObjectTree(cs.getObject(name)));
            }
        }

        return new Clients(callbackUrl, clients);
    }

    private static Object buildObjectTree(JsonObject o) {
        Object object = null;
        try {
            String className = o.getString("class");
            Class<?> c = Class.forName(className);
            object = c.newInstance();
            JsonObject props = o.getObject("props");
            if (props != null) {
                for (String field : props.getFieldNames()) {
                    Object value = props.getValue(field);
                    if (value != null) {
                        if (value instanceof JsonObject) {
                            value = buildObjectTree((JsonObject) value);
                        }
                        Method m = findInheritedMethod(c, getSetterName(field), new Class[] { value.getClass() }, false);
                        m.invoke(object, value);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return object;
    }

    private static Method findInheritedMethod(Class clazz, String methodName, Class[] args, boolean strictArgs)
            throws NoSuchMethodException {
        if (clazz == null)
            throw new NoSuchMethodException("No class for method " + methodName + " and args " + args);
        if (methodName == null)
            throw new NoSuchMethodException("No method name");

        Method method = null;
        Method[] methods = clazz.getDeclaredMethods();
        for (int i = 0; i < methods.length && method == null; i++) {
            if (methods[i].getName().equals(methodName)
                    && checkParams(methods[i].getParameterTypes(), args, strictArgs))
                method = methods[i];
        }
        if (method != null) {
            return method;
        } else
            return findInheritedMethod(clazz.getSuperclass(), methodName, args, strictArgs);
    }

    @SuppressWarnings("unchecked")
    private static boolean checkParams(Class[] formalParams, Class[] actualParams, boolean strict) {
        if (formalParams == null && actualParams == null)
            return true;
        if (formalParams == null && actualParams != null)
            return false;
        if (formalParams != null && actualParams == null)
            return false;

        if (formalParams.length != actualParams.length)
            return false;

        if (formalParams.length == 0)
            return true;

        int j = 0;
        if (strict) {
            while (j < formalParams.length && formalParams[j].equals(actualParams[j]))
                j++;
        } else {
            while ((j < formalParams.length) && (getWrapperClass(formalParams[j]).isAssignableFrom(actualParams[j]))) {
                j++;
            }
        }

        if (j != formalParams.length) {
            return false;
        }

        return true;
    }

    private static Class getWrapperClass(Class clazz) {
        Class c = map.get(clazz);
        return (c == null) ? clazz : c;
    }

    private static String getSetterName(String field) {
        return "set" + field.substring(0, 1).toUpperCase() + field.substring(1);
    }
}
