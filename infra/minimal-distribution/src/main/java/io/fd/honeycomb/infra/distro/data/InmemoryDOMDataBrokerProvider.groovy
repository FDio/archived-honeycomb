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

package io.fd.honeycomb.infra.distro.data

import com.google.inject.Inject
import com.google.inject.name.Named
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.fd.honeycomb.infra.distro.ProviderTrait
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitDeadlockException
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker
import org.opendaylight.controller.md.sal.dom.broker.impl.SerializedDOMDataBroker
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore
import org.opendaylight.yangtools.util.concurrent.DeadlockDetectingListeningExecutorService
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors

@Slf4j
@ToString
class InmemoryDOMDataBrokerProvider extends ProviderTrait<DOMDataBroker> {

    public static final String CONFIG = "config"
    public static final String OPERATIONAL = "operational"

    @Inject
    @Named(InmemoryDOMDataBrokerProvider.CONFIG)
    InMemoryDOMDataStore cfgDataStore

    @Inject
    @Named(InmemoryDOMDataBrokerProvider.OPERATIONAL)
    InMemoryDOMDataStore operDataStore

    @Override
    def create() {
        // This Databroker is dedicated for netconf metadata, not expected to be under heavy load
        def listenableFutureExecutor = SpecialExecutors.newBlockingBoundedCachedThreadPool(1, 100, "commits")
        def commitExecutor = SpecialExecutors.newBoundedSingleThreadExecutor(100, "WriteTxCommit")
        // TODO try to provide more lightweight implementation of DataBroker, maybe a single executor would be enough

        def map = [:]
        map.put(LogicalDatastoreType.CONFIGURATION, cfgDataStore)
        map.put(LogicalDatastoreType.OPERATIONAL, operDataStore)

        new SerializedDOMDataBroker(map,
                new DeadlockDetectingListeningExecutorService(commitExecutor,
                        TransactionCommitDeadlockException.DEADLOCK_EXCEPTION_SUPPLIER,
                        listenableFutureExecutor))
    }
}
