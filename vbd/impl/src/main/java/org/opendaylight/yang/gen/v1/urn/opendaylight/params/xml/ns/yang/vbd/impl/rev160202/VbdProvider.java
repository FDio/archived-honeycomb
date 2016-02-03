/**
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbd.impl.rev160202;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;

/**
 * Created by joe on 2.2.2016.
 */
public class VbdProvider implements BindingAwareProvider {

    @Override
    public void onSessionInitiated(BindingAwareBroker.ProviderContext session) {
        // Intentional NOOP
    }
}
