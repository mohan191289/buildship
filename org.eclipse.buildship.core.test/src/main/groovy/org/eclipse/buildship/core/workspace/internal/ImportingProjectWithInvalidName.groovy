/*
 * Copyright (c) 2015 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Etienne Studer & Donát Csikós (Gradle Inc.) - initial API and implementation and initial documentation
 */

package org.eclipse.buildship.core.workspace.internal

import spock.lang.Issue

import org.eclipse.buildship.core.Logger
import org.eclipse.buildship.core.notification.UserNotification
import org.eclipse.buildship.core.test.fixtures.ProjectSynchronizationSpecification


@Issue('https://bugs.eclipse.org/bugs/show_bug.cgi?id=490648')
class ImportingProjectWithInvalidName extends ProjectSynchronizationSpecification {

    def "Can import project with custom name containing invalid characters"() {
        setup:
        def location = dir('app') {
            file 'build.gradle', ''
            file 'settings.gradle', "rootProject.name = '${gradleProjectName}'"
        }

        when:
        importAndWait(location)

        then:
        findProject(eclipseProjectName)

        where:
        gradleProjectName << org.eclipse.core.internal.resources.OS.INVALID_RESOURCE_CHARACTERS.collect { "${it}project${it}" }
        eclipseProjectName << ['_project_'] * org.eclipse.core.internal.resources.OS.INVALID_RESOURCE_CHARACTERS.size()
    }

    def "Illegal character replacement might lead to clashing project names"() {
        setup:
        def notification = Mock(UserNotification)
        registerService(UserNotification, notification)

        def location = dir('app') {
            file 'build.gradle', ''
            file 'settings.gradle', """include 'a'
include 'b'
project(':a').name = '/project'
project(':b').name = '_project'
"""
            dir('a')
            dir('b')
        }

        when:
        importAndWait(location)

        then:
        1 * notification.errorOccurred(*_)


    }
}
