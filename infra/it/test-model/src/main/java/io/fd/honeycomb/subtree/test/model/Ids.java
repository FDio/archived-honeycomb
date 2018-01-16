/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
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

package io.fd.honeycomb.subtree.test.model;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.subtree.test.rev180116.C1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.subtree.test.rev180116.c1.C2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.subtree.test.rev180116.c1.C3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.subtree.test.rev180116.c1.C4;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Instance identifiers referencing all complex nodes within honeycomb's subtree test model.
 */
public interface Ids {
    InstanceIdentifier<C1> C1_ID = InstanceIdentifier.create(C1.class);
    InstanceIdentifier<C2> C2_ID = C1_ID.child(C2.class);
    InstanceIdentifier<C3> C3_ID = C1_ID.child(C3.class);
    InstanceIdentifier<C4> C4_ID = C1_ID.child(C4.class);
}
