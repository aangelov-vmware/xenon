/*
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, without warranties or
 * conditions of any kind, EITHER EXPRESS OR IMPLIED.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.vmware.xenon.common;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vmware.xenon.common.ServiceDocumentDescription.PropertyDescription;

public class ReflectionUtils {

    private static final ConcurrentHashMap<Class<?>, Map<String, Field>> DECLARED_FIELDS_CACHE = new ConcurrentHashMap<>();

    public static <T> T instantiate(Class<T> clazz) {
        try {
            Constructor<T> ctor = clazz.getDeclaredConstructor();
            if (!ctor.isAccessible()) {
                ctor.setAccessible(true);
            }
            return ctor.newInstance();
        } catch (Throwable e) {
            Utils.logWarning("Reflection error: %s", Utils.toString(e));
        }
        return null;
    }

    public static Object getPropertyValue(PropertyDescription pd, Object instance) {
        try {
            return pd.accessor.get(instance);
        } catch (Throwable e) {
            Utils.logWarning("Reflection error: %s", Utils.toString(e));
        }
        return null;
    }

    public static void setPropertyValue(PropertyDescription pd, Object instance, Object value) {
        try {
            pd.accessor.set(instance, value);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if fieldName is present and accessible in service type
     */
    public static boolean hasField(Class<? extends Service> type, String fieldName) {
        try {
            type.getField(fieldName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retrieve field and make it accessible.
     */
    public static Field getField(Class<?> clazz, String name) {

        Map<String, Field> fieldMap = DECLARED_FIELDS_CACHE.computeIfAbsent(clazz, key ->
                        Arrays.stream(key.getDeclaredFields())
                                .collect(toMap(Field::getName, identity()))
        );

        Field field = fieldMap.get(name);
        if (field == null) {
            return null;
        }

        if (!field.isAccessible()) {
            synchronized (field) {
                field.setAccessible(true);
            }
        }
        return field;
    }

}
