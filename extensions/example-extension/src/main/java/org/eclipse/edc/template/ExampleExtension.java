/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.template;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.io.IOException;

import static org.eclipse.edc.template.ExampleExtension.NAME;

/**
 * Example extension that demonstrates the extensibility concept
 */
@Extension(NAME)
public class ExampleExtension implements ServiceExtension {
    /**
     * The name of the extension that is visible in the {@link org.eclipse.edc.spi.monitor.Monitor}
     */
    public static final String NAME = " Super-Cool Project Example Extension";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        //todo: implement your extension initialization code here
        try (var banner = getClass().getClassLoader().getResourceAsStream("banner.txt")) {
            if (banner != null) {
                System.out.println(new String(banner.readAllBytes()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {
        //todo: implement here logic that starts your extension code
    }

    @Override
    public void shutdown() {
        //todo: implement cleanup logic here
    }

    @Override
    public void prepare() {
        //todo: implement your pre-start logic here
    }
}
