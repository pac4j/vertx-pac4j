package org.pac4j.vertx;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.vertx.java.core.json.JsonObject;

@SuppressWarnings("rawtypes")
public class ClientsBuilder {

    public static Clients buildClients(JsonObject conf) {
        String callbackUrl = conf.getString("callbackUrl");
        List<Client> clients = new ArrayList<>();
        for (String name : conf.getObject("clients").getFieldNames()) {
            clients.add((Client) buildObjectTree(conf.getObject("clients").getObject(name)));
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
                    if (value instanceof String) {
                        Method m = findInheritedMethod(c, getSetterName(field), new Class[] { String.class }, false);
                        m.invoke(object, value);
                    } else if (value instanceof JsonObject) {
                        Object objectValue = buildObjectTree((JsonObject) value);
                        Method m = findInheritedMethod(c, getSetterName(field), new Class[] { objectValue.getClass() },
                                false);
                        m.invoke(object, objectValue);
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
            throw new NoSuchMethodException("No class");
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
            while ((j < formalParams.length) && (formalParams[j].isAssignableFrom(actualParams[j]))) {
                j++;
            }
        }

        if (j != formalParams.length) {
            return false;
        }

        return true;
    }

    private static String getSetterName(String field) {
        return "set" + field.substring(0, 1).toUpperCase() + field.substring(1);
    }
}
