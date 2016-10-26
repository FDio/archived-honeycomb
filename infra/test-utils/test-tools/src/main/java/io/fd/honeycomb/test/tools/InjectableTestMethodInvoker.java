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

package io.fd.honeycomb.test.tools;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Invoker that allows injecting of method parameters
 */
class InjectableTestMethodInvoker extends Statement {

    private static final Logger LOG = LoggerFactory.getLogger(InjectableTestMethodInvoker.class);

    private final FrameworkMethod testMethod;
    private final Object target;
    private final Object[] invocationParams;


    InjectableTestMethodInvoker(final FrameworkMethod testMethod, final Object target,
                                final Object[] invocationParams) {
        this.testMethod = testMethod;
        this.target = target;
        this.invocationParams = invocationParams;
    }

    @Override
    public void evaluate() throws Throwable {
        LOG.debug("Invoking @Test[{}] on target[{}] with params {}", testMethod.getName(), target, invocationParams);
        testMethod.invokeExplosively(target, invocationParams);
    }
}
