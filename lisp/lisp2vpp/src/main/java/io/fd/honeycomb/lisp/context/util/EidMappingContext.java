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

package io.fd.honeycomb.lisp.context.util;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Optional;
import io.fd.honeycomb.lisp.translate.util.EidConverter;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.util.RWUtils;
import java.util.stream.Collector;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.Contexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.EidMappingContextKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.MappingKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.EidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.MappingId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * Utility class allowing {@link MappingId} to {@link Eid} mapping
 */
public class EidMappingContext {

    private static final Collector<Mapping, ?, Mapping> SINGLE_ITEM_COLLECTOR = RWUtils.singleItemCollector();

    private final KeyedInstanceIdentifier<org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.EidMappingContext, EidMappingContextKey>
            namingContextIid;

    /**
     * Create new naming context
     *
     * @param instanceName name of this context instance. Will be used as list item identifier within context data tree
     */
    public EidMappingContext(@Nonnull final String instanceName) {
        namingContextIid = InstanceIdentifier.create(Contexts.class).child(
                org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.EidMappingContext.class,
                new EidMappingContextKey(instanceName));
    }

    /**
     * Retrieve name for mapping stored provided mappingContext instance.
     *
     * @param eid            eid of a mapped item
     * @param mappingContext mapping context providing context data for current transaction
     * @return name mapped to provided index
     */
    @Nonnull
    public synchronized MappingId getId(
            @Nonnull final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.local.mappings.local.mapping.Eid eid,
            @Nonnull final MappingContext mappingContext) {

        final Optional<Mappings> read = mappingContext.read(namingContextIid.child(Mappings.class));

        //dont create artificial name as naming context, to not create refference to some artificial(in vpp non-existing)eid
        checkArgument(read.isPresent(), "No mapping stored for eid: %s", eid);

        return read.get().getMapping().stream()
                .filter(mapping -> EidConverter.compareEids(mapping.getEid(), eid))
                .collect(SINGLE_ITEM_COLLECTOR).getId();
    }

    /**
     * Retrieve name for mapping stored provided mappingContext instance.
     *
     * @param eid            eid of a mapped item
     * @param mappingContext mapping context providing context data for current transaction
     * @return name mapped to provided index
     */
    @Nonnull
    public synchronized MappingId getId(
            @Nonnull final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.Eid eid,
            @Nonnull final MappingContext mappingContext) {

        final Optional<Mappings> read = mappingContext.read(namingContextIid.child(Mappings.class));

        //dont create artificial name as naming context, to not create refference to some artificial(in vpp non-existing)eid
        checkArgument(read.isPresent(), "No mapping stored for eid: %s", eid);

        //this kind of comparing is needed ,because yang generated containers does not override equals,unless they are defined as types
        //in this hierarchy the first that define proper equals is Ipv4Address/Ipv6Address/MacAddress
        //
        // From official javadoc
        //  The equals method for class Object implements the most discriminating possible equivalence relation on objects;
        //  that is, for any non-null reference values x and y, this method returns true if and only if x and y refer to the same object
        //  (x == y has the value true).
        return read.get().getMapping().stream()
                .filter(mapping -> EidConverter.compareEids(mapping.getEid(), eid))
                .collect(SINGLE_ITEM_COLLECTOR).getId();
    }

    /**
     * Check whether mapping is present for index.
     *
     * @param eid            eid of a mapped item
     * @param mappingContext mapping context providing context data for current transaction
     * @return true if present, false otherwise
     */
    public synchronized boolean containsId(
            @Nonnull final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.local.mappings.local.mapping.Eid eid,
            @Nonnull final MappingContext mappingContext) {
        final Optional<Mappings> read = mappingContext.read(namingContextIid.child(Mappings.class));

        //dont create artificial name as naming context, to not create refference to some artificial(in vpp non-existing)eid
        checkArgument(read.isPresent(), "No mapping stored for eid: %s", eid);

        return read.isPresent()
                ? read.get().getMapping().stream().anyMatch(mapping -> EidConverter.compareEids(mapping.getEid(), eid))
                : false;
    }

    /**
     * Check whether mapping is present for index.
     *
     * @param eid            eid of a mapped item
     * @param mappingContext mapping context providing context data for current transaction
     * @return true if present, false otherwise
     */
    public synchronized boolean containsId(
            @Nonnull final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.Eid eid,
            @Nonnull final MappingContext mappingContext) {
        final Optional<Mappings> read = mappingContext.read(namingContextIid.child(Mappings.class));

        checkArgument(read.isPresent(), "No mapping stored for eid: %s", eid);

        return read.isPresent()
                ? read.get().getMapping().stream().anyMatch(mapping -> EidConverter.compareEids(mapping.getEid(), eid))
                : false;
    }


    /**
     * Add mapping to current context
     *
     * @param index          index of a mapped item
     * @param eid            eid data
     * @param mappingContext mapping context providing context data for current transaction
     */
    public synchronized void addEid(
            @Nonnull final MappingId index,
            @Nonnull final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.local.mappings.local.mapping.Eid eid,
            final MappingContext mappingContext) {

        final KeyedInstanceIdentifier<Mapping, MappingKey> mappingIid = getMappingIid(index);
        //this copy is needed (type of eid in mapping is different that in local mapping,they only have same ancestor)
        mappingContext.put(mappingIid, new MappingBuilder().setId(index).setEid(copyEid(eid)).build());
    }

    /**
     * Add mapping to current context
     *
     * @param index          index of a mapped item
     * @param eid            eid data
     * @param mappingContext mapping context providing context data for current transaction
     */
    public synchronized void addEid(
            @Nonnull final MappingId index,
            @Nonnull final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.Eid eid,
            final MappingContext mappingContext) {

        final KeyedInstanceIdentifier<Mapping, MappingKey> mappingIid = getMappingIid(index);
        mappingContext.put(mappingIid, new MappingBuilder().setId(index).setEid(copyEid(eid)).build());
    }

    private KeyedInstanceIdentifier<Mapping, MappingKey> getMappingIid(final MappingId index) {
        return namingContextIid.child(Mappings.class).child(Mapping.class, new MappingKey(index));
    }

    private Eid copyEid(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.local.mappings.local.mapping.Eid eid) {
        return new EidBuilder().setAddress(eid.getAddress()).setAddressType(eid.getAddressType())
                .setVirtualNetworkId(eid.getVirtualNetworkId()).build();
    }

    private Eid copyEid(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.Eid eid) {
        return new EidBuilder().setAddress(eid.getAddress()).setAddressType(eid.getAddressType())
                .setVirtualNetworkId(eid.getVirtualNetworkId()).build();
    }

    /**
     * Remove mapping from current context
     *
     * @param index          identificator of a mapped item
     * @param mappingContext mapping context providing context data for current transaction
     */
    public synchronized void removeEid(@Nonnull final MappingId index, final MappingContext mappingContext) {
        mappingContext.delete(getMappingIid(index));
    }

    /**
     * Returns index value associated with the given name.
     *
     * @param index          index whitch should value sits on
     * @param mappingContext mapping context providing context data for current transaction
     * @return integer index value matching supplied name
     * @throws IllegalArgumentException if name was not found
     */
    public synchronized Eid getEid(@Nonnull final MappingId index, final MappingContext mappingContext) {
        final Optional<Mapping> read = mappingContext.read(getMappingIid(index));
        checkArgument(read.isPresent(), "No mapping stored for index: %s", index);
        return mappingContext.read(getMappingIid(index)).get().getEid();
    }

    /**
     * Check whether mapping is present for name.
     *
     * @param index          index of a mapped item
     * @param mappingContext mapping context providing context data for current transaction
     * @return true if present, false otherwise
     */
    public synchronized boolean containsEid(@Nonnull final MappingId index,
                                            @Nonnull final MappingContext mappingContext) {
        return mappingContext.read(getMappingIid(index)).isPresent();
    }
}