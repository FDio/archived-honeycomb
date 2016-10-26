/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.test.tools.annotations;

import static io.fd.honeycomb.test.tools.annotations.InjectTestData.NO_ID;

import com.google.common.base.Optional;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.data.rev150105.$YangModuleInfoImpl;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Common logic for @InjectTestData
 */
public interface InjectablesProcessor {

    default List<Field> injectableFields(final Class<?> testClass) {
        return FieldUtils.getFieldsListWithAnnotation(testClass, InjectTestData.class);
    }

    default boolean isInjectable(final Parameter parameter) {
        return parameter.isAnnotationPresent(InjectTestData.class);
    }

    default String resourcePath(final Field field) {
        return field.getAnnotation(InjectTestData.class).resourcePath();
    }

    default String resourcePath(final Parameter parameter) {
        return parameter.getAnnotation(InjectTestData.class).resourcePath();
    }

    default Optional<String> instanceIdentifier(final Field field) {
        final String identifier = field.getAnnotation(InjectTestData.class).id();
        // == used instead of equals to ensure constant was used
        if (NO_ID.equals(identifier)) {
            return Optional.absent();
        } else {
            return Optional.of(identifier);
        }
    }

    default Optional<String> instanceIdentifier(final Parameter parameter) {
        final String identifier = parameter.getAnnotation(InjectTestData.class).id();
        // == used instead of equals to ensure constant was used
        if (NO_ID.equals(identifier)) {
            return Optional.absent();
        } else {
            return Optional.of(identifier);
        }
    }

    default void injectField(final Field field, final Object testInstance, final DataObject data) {
        field.setAccessible(true);
        try {
            FieldUtils.writeField(field, testInstance, data);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to access field " + field);
        }
    }

    default YangInstanceIdentifier getRootInstanceIdentifier(final Class type) {
        try {
            return YangInstanceIdentifier.of(QName.class.cast(type.getField("QNAME").get(null)));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Constant QNAME not accessible for type" + type);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Class " + type + " does not have QName defined");
        }
    }

    default ModuleInfoBackedContext provideSchemaContextFor(final Set<? extends YangModuleInfo> modules) {
        final ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();
        moduleInfoBackedContext.addModuleInfos(modules);
        return moduleInfoBackedContext;
    }
}
