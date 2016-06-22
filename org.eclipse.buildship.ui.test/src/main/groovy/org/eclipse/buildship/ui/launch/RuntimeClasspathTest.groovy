package org.eclipse.buildship.ui.launch

import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.debug.core.DebugPlugin
import org.eclipse.debug.core.ILaunch
import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.core.ILaunchConfigurationType
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy
import org.eclipse.debug.core.ILaunchManager
import org.eclipse.debug.core.IStreamListener
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants

import org.eclipse.buildship.ui.test.fixtures.ProjectSynchronizationSpecification


class RuntimeClasspathTest extends ProjectSynchronizationSpecification {

    // This is a UI test simply because launching Java applications doesn't work in headless mode

    def "project classpath has entries only from local Gradle classpath container"() {
        setup:
        File location = sampleProject()
        importAndWait(location)
        waitForCompilation(location)

        when:
        ILaunchConfiguration config = createJavaLaunchConfig('c.Main', 'c')
        ILaunch launch = config.launch("run", new NullProgressMonitor())
        StringBuilder stringBuilder = new StringBuilder()
        IStreamListener listener = { text, monitor -> stringBuilder.append(text) } as IStreamListener
        launch.processes[0].streamsProxy.outputStreamMonitor.addListener(listener)
        waitUntilLaunchTerminated(launch)

        then:
        String result = stringBuilder.toString()
        result.contains('log4j-1.2.17')
        !result.contains('log4j-1.2.16')
    }

    private def sampleProject() {
        dir('sample-project') {
            file 'build.gradle',  '''
            subprojects {
                apply plugin: 'java'

                repositories {
                    mavenCentral()
                }
            }

            project(':a') {
                dependencies {
                    compile 'log4j:log4j:1.2.17'
                }
            }

            project(':b') {
                dependencies {
                    compile 'log4j:log4j:1.2.16'
                }
            }

            project(':c') {
                dependencies {
                    compile project(':a')
                    compile project(':b')
                    testCompile 'junit:junit:4.12'
                }
            }
            '''
            file('settings.gradle') << '''
                rootProject.name= 'sample-project'

                include "a"
                include "b"
                include "c"
            '''

        dir('a')
        dir('b')
        dir('c/src/main/java/c') {
            file('Main.java') << '''
                package c;
                import java.net.*;

                public class Main {
                    public static void main(String[] args) {
                        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
                        for (URL url: ((URLClassLoader)classLoader).getURLs()) {
                            System.out.println(url.getFile());
                        }
                    }
                }
            '''
            }
        }
    }

    private ILaunchConfiguration createJavaLaunchConfig(String mainClass, String projectName) {
        ILaunchManager launchManager = DebugPlugin.default.launchManager
        ILaunchConfigurationType type = launchManager.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION)
        ILaunchConfigurationWorkingCopy config = type.newInstance(null, 'sample-launch')
        config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, mainClass)
        config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName)
        config
    }

    private void waitForCompilation(File location) {
        long timeout = System.currentTimeMillis() + 1000000
        while (!new File(location, 'c/bin/c/Main.class').exists()) {
            if (System.currentTimeMillis() >= timeout) {
                throw new RuntimeException('Timeout while waiting for the compliation to finish')
            }
            Thread.sleep(200)
        }
    }

    private void waitUntilLaunchTerminated(ILaunch launch) {
        long timeout = System.currentTimeMillis() + 1000000
        while (!launch.isTerminated()) {
            if (System.currentTimeMillis() >= timeout) {
                throw new RuntimeException('Timeout while waiting for the launch to finish')
            }
            Thread.sleep(200)
        }
    }

}
