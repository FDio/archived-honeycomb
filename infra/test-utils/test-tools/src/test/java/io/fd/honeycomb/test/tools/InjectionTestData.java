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

package io.fd.honeycomb.test.tools;

final class InjectionTestData {

    static final String CONTAINER_UNDER_LIST_DATA_PATH = "/hc-data:simple-container" +
            "/hc-data:simple-list[hc-data:name='nameUnderSimpleList']" +
            "/hc-data:cont-under-list";

    static final String SIMPLE_LIST_DATA_PATH = "/hc-data:simple-container" +
            "/hc-data:simple-list[hc-data:name='nameUnderSimpleList']";

    static final String NESTED_LIST_DATA_PATH = "/hc-data:simple-container" +
            "/hc-data:simple-list[hc-data:name='nameUnderSimpleList']" +
            "/hc-data:nested-list[hc-data:nested-name='nameUnderNestedList']";

    static final String ROOT_LIST_DATA_PATH = "/hc-data:root-list[hc-data:root-name='rootName']";
    static final String AUGMENT_LIST_DATA_PATH = "/hc-data:simple-container" +
            "/hc-data:augmented-container" +
            "/hc-data:list-in-augment[hc-data:key-in-augment='keyInAugment']";

    static final String CONTAINER_IN_ROOT_RESOURCE = "/simpleContainerEmpty.json";
    static final String CONTAINER_UNDER_LIST_RESOURCE = "/containerInList.json";
    static final String SIMPLES_LIST_RESOURCE = "/simpleListEntry.json";
    static final String NESTED_LIST_RESOURCE = "/nestedListEntry.json";
    static final String ROOT_LIST_RESOURCE = "/rootListEntry.json";
    static final String AUGMENT_LIST_RESOURCE = "/augmentListEntry.json";

    private InjectionTestData() {
    }
}
