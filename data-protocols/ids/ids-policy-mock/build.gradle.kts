/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

val infoModelVersion: String by project
val rsApi: String by project

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    implementation(project(":data-protocols:ids:ids-spi"))
}


publishing {
    publications {
        create<MavenPublication>("ids-policy-mock") {
            artifactId = "ids-policy-mock"
            from(components["java"])
        }
    }
}
