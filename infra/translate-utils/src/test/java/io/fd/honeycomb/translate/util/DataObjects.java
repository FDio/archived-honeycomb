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

package io.fd.honeycomb.translate.util;

import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DataObjects {
    public interface DataObject1 extends DataObject {
        InstanceIdentifier<DataObject1> IID = InstanceIdentifier.create(DataObject1.class);
    }

    public interface DataObject2 extends DataObject {
        InstanceIdentifier<DataObject2> IID = InstanceIdentifier.create(DataObject2.class);
    }

    public interface DataObject3 extends DataObject {
        InstanceIdentifier<DataObject3> IID = InstanceIdentifier.create(DataObject3.class);
        interface DataObject31 extends DataObject, ChildOf<DataObject3> {
            InstanceIdentifier<DataObject31> IID = DataObject3.IID.child(DataObject31.class);
        }
    }

    public interface DataObject4 extends DataObject {
        InstanceIdentifier<DataObject4> IID = InstanceIdentifier.create(DataObject4.class);
        interface DataObject41 extends DataObject, ChildOf<DataObject4> {
            InstanceIdentifier<DataObject41> IID = DataObject4.IID.child(DataObject41.class);
            interface DataObject411 extends DataObject, ChildOf<DataObject41> {
                InstanceIdentifier<DataObject411> IID = DataObject41.IID.child(DataObject411.class);
            }
        }

        interface DataObject42 extends DataObject, ChildOf<DataObject4> {
            InstanceIdentifier<DataObject42> IID = DataObject4.IID.child(DataObject42.class);
        }
    }

    public interface DataObjectK extends DataObject, Identifiable<DataObjectKey> {
        InstanceIdentifier<DataObjectK> IID = InstanceIdentifier.create(DataObjectK.class);

        interface DataObjectK1 extends DataObject, ChildOf<DataObjectK> {
            InstanceIdentifier<DataObjectK1> IID = DataObjectK.IID.child(DataObjectK1.class);
        }
    }

    public interface DataObjectKBuilder extends org.opendaylight.yangtools.concepts.Builder<DataObjectK>{
    }

    public interface DataObject1ChildK extends DataObject, ChildOf<DataObject1>, Identifiable<DataObject1ChildKey> {
        // needs to be defined like this to have paths totally equal after cutting path for internally keyed id inside infra
        InstanceIdentifier<DataObject1ChildK> IID =
                RWUtils.makeIidWildcarded(InstanceIdentifier.create(DataObject1.class).child(DataObject1ChildK.class));
        InstanceIdentifier<DataObject1ChildK> INTERNALLY_KEYED_IID = InstanceIdentifier.create(DataObject1.class)
                .child(DataObject1ChildK.class, new DataObject1ChildKey());

        public interface DataObject1ChildKNested extends DataObject, ChildOf<DataObject1ChildK> {
            InstanceIdentifier<DataObject1ChildK.DataObject1ChildKNested> IID = DataObject1ChildK.IID.child(DataObject1ChildK.DataObject1ChildKNested.class);
        }

        public interface DataObject1ChildKNested2 extends DataObject, ChildOf<DataObject1ChildK> {
            InstanceIdentifier<DataObject1ChildK.DataObject1ChildKNested2> IID = DataObject1ChildK.IID.child(DataObject1ChildK.DataObject1ChildKNested2.class);
        }
    }

    public static class DataObject1ChildKey implements Identifier<DataObject1ChildK> {
    }

    public static class DataObjectKey implements Identifier<DataObjectK> {
    }
}
