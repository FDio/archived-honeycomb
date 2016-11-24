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

package io.fd.honeycomb.translate.util.read.cache;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TypeAwareIdentifierCacheKeyFactoryTest {

    private DataObjectParentKey parentKey;
    private DataObjectChildKey childKey;
    private InstanceIdentifier<DataObjectChild> identifierBothKeyed;
    private InstanceIdentifier<DataObjectChild> identifierOneMissing;
    private InstanceIdentifier<DataObjectChild> identifierNoneKeyed;
    private TypeAwareIdentifierCacheKeyFactory simpleKeyFactory;
    private TypeAwareIdentifierCacheKeyFactory complexKeyFactory;

    @Before
    public void init() {
        parentKey = new DataObjectParentKey();
        childKey = new DataObjectChildKey();
        identifierBothKeyed = InstanceIdentifier.create(SuperDataObject.class).child(DataObjectParent.class, parentKey)
                .child(DataObjectChild.class, childKey);
        identifierOneMissing = InstanceIdentifier.create(DataObjectChild.class);
        identifierNoneKeyed = InstanceIdentifier.create(SuperDataObject.class).child(DataObjectParent.class)
                .child(DataObjectChild.class);

        complexKeyFactory =
                new TypeAwareIdentifierCacheKeyFactory(String.class, ImmutableSet.of(DataObjectParent.class));
        simpleKeyFactory = new TypeAwareIdentifierCacheKeyFactory(String.class);
    }

    @Test
    public void createKeyBothKeyedComplex() {
        final String key = complexKeyFactory.createKey(identifierBothKeyed);

        /**
         * Should pass because key constructed in this case should look like :
         * additional_scope_type[additional_scope_type_key]|cached_type
         * */
        verifyComplexKey(key);
    }

    /**
     * Should fail because provided identifier does'nt contain all requested key parts
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeyOneMissingComplex() {
        complexKeyFactory.createKey(identifierOneMissing);
    }

    /**
     * Should fail because request paths are not keyed
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeyNoneKeyedComplex() {
        complexKeyFactory.createKey(identifierNoneKeyed);
    }

    @Test
    public void createKeyBothKeyedSimple() {
        final String key = simpleKeyFactory.createKey(identifierBothKeyed);

        /**
         * Should pass because key constructed in this case should look like : cached_type
         * */
        verifySimpleKey(key);
    }

    @Test
    public void createKeyOneMissingSimple() {
        final String key = simpleKeyFactory.createKey(identifierOneMissing);
        /**
         * Should pass because key constructed in this case should look like : cached_type
         * */
        verifySimpleKey(key);
    }

    /**
     * Should fail because request paths are not keyed
     */
    @Test
    public void createKeyNoneKeyedSimple() {
        final String key = simpleKeyFactory.createKey(identifierNoneKeyed);
        /**
         * Should pass because key constructed in this case should look like : cached_type
         * */
        verifySimpleKey(key);
    }

    private void verifyComplexKey(final String key) {
        assertTrue(key.contains(String.class.getTypeName()));
        assertTrue(key.contains(DataObjectParent.class.getTypeName()));
        assertTrue(key.contains(parentKey.toString()));
        assertTrue(key.contains(DataObjectChild.class.getTypeName()));
        assertFalse(key.contains(childKey.toString()));
        assertFalse(key.contains(SuperDataObject.class.getTypeName()));
    }

    private void verifySimpleKey(final String key) {
        assertTrue(key.contains(String.class.getTypeName()));
        assertFalse(key.contains(DataObjectParent.class.getTypeName()));
        assertFalse(key.contains(parentKey.toString()));
        assertTrue(key.contains(DataObjectChild.class.getTypeName()));
        assertFalse(key.contains(childKey.toString()));
        assertFalse(key.contains(SuperDataObject.class.getTypeName()));
    }

    private interface SuperDataObject extends DataObject {
    }

    private interface DataObjectParent extends DataObject, ChildOf<SuperDataObject>, Identifiable<DataObjectParentKey> {
    }

    private interface DataObjectChild extends DataObject, ChildOf<DataObjectParent>, Identifiable<DataObjectChildKey> {
    }

    private class DataObjectParentKey implements Identifier<DataObjectParent> {
    }

    private class DataObjectChildKey implements Identifier<DataObjectChild> {
    }
}