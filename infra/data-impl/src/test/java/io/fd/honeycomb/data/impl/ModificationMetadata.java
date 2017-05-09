/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.honeycomb.data.impl;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

abstract class ModificationMetadata {

    private static final String YANG_NAMESPACE = "urn:opendaylight:params:xml:ns:yang:test:diff";
    final QName TOP_CONTAINER_QNAME = QName.create(YANG_NAMESPACE, "2015-01-05", "top-container");
    final QName STRING_LEAF_QNAME = QName.create(TOP_CONTAINER_QNAME, "string");
    final QName NAME_LEAF_QNAME = QName.create(TOP_CONTAINER_QNAME, "name");
    final QName TEXT_LEAF_QNAME = QName.create(TOP_CONTAINER_QNAME, "text");
    final QName NESTED_LIST_QNAME = QName.create(TOP_CONTAINER_QNAME, "nested-list");
    final QName DEEP_LIST_QNAME = QName.create(TOP_CONTAINER_QNAME, "deep-list");
    final QName EMPTY_QNAME = QName.create(TOP_CONTAINER_QNAME, "empty");

    final QName FOR_LEAF_LIST_QNAME = QName.create(TOP_CONTAINER_QNAME, "for-leaf-list");
    final QName NESTED_LEAF_LIST_QNAME = QName.create(TOP_CONTAINER_QNAME, "nested-leaf-list");

    final QName WITH_CHOICE_CONTAINER_QNAME = QName.create(YANG_NAMESPACE, "2015-01-05", "with-choice");
    final QName CHOICE_QNAME = QName.create(WITH_CHOICE_CONTAINER_QNAME, "choice");
    final QName IN_CASE1_LEAF_QNAME = QName.create(WITH_CHOICE_CONTAINER_QNAME, "in-case1");

    final QName PRESENCE_CONTAINER_QNAME = QName.create(TOP_CONTAINER_QNAME, "presence");

    final QName NESTED_CONTAINER_QNAME = QName.create(TOP_CONTAINER_QNAME, "nested-container");
    final QName NESTED_LIST_IN_CONTAINER_QNAME = QName.create(NESTED_CONTAINER_QNAME, "nested-list-in-container");
    final QName IN_CONTAINER_NAME_LEAF_QNAME = QName.create(NESTED_LIST_IN_CONTAINER_QNAME, "name");
    final QName NESTED_CONTAINER_VAL = QName.create(NESTED_CONTAINER_QNAME, "nested-container-val");
    final QName NESTED_CONTAINER_LEAF_LIST = QName.create(NESTED_CONTAINER_QNAME, "nested-container-leaf-list");

    final QName NESTED_CHOICE = QName.create(NESTED_CONTAINER_QNAME,"nested-choice");
    final QName NESTED_CASE=QName.create(NESTED_CHOICE,"nested-case");
    final QName UNDER_NESTED_CASE=QName.create(NESTED_CASE,"under-nested-case");

    final QName AUG_LEAF = QName.create(NESTED_CONTAINER_QNAME,"under-aug-leaf");
    final QName AUG_LEAFLIST = QName.create(NESTED_CONTAINER_QNAME,"under-aug-leaflist");
    final QName AUG_CONTAINER = QName.create(NESTED_CONTAINER_QNAME,"under-aug-container");
    final QName AUG_CONTAINER_LEAF = QName.create(NESTED_CONTAINER_QNAME,"under-aug-cont-leaf");
    final QName AUG_LIST = QName.create(NESTED_CONTAINER_QNAME,"under-aug-list");
    final QName AUG_LIST_KEY = QName.create(NESTED_CONTAINER_QNAME,"under-aug-list-key");

    final QName NESTED_AUG_CONTAINER = QName.create(AUG_CONTAINER, "nested-under-aug-container");
    final QName NESTED_AUG_CONTAINER_LEAF = QName.create(AUG_CONTAINER, "nested-under-aug-container-leaf");
    final QName NESTED_AUG_LEAF = QName.create(AUG_CONTAINER, "nested-under-aug-leaf");
    final QName NESTED_AUG_LIST = QName.create(AUG_CONTAINER, "nested-under-aug-list");
    final QName NESTED_AUG_LIST_KEY = QName.create(AUG_CONTAINER, "nested-under-aug-list-key");
    final QName NESTED_AUG_LEAF_LIST = QName.create(AUG_CONTAINER, "nested-under-aug-leaf-list");

    final YangInstanceIdentifier TOP_CONTAINER_ID = YangInstanceIdentifier.of(TOP_CONTAINER_QNAME);
    final YangInstanceIdentifier NESTED_LIST_ID = TOP_CONTAINER_ID.node(new YangInstanceIdentifier.NodeIdentifier(NESTED_LIST_QNAME));
}
