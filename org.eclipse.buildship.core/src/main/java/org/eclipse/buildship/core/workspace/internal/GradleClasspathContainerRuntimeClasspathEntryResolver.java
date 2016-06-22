package org.eclipse.buildship.core.workspace.internal;

import java.util.List;

import org.gradle.internal.impldep.com.google.common.collect.Lists;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathEntryResolver;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.buildship.core.CorePlugin;
import org.eclipse.buildship.core.workspace.GradleClasspathContainer;

/**
 * {@link IRuntimeClasspathEntryResolver} implementation to resolve Gradle classpath container
 * entries.
 */
public class GradleClasspathContainerRuntimeClasspathEntryResolver implements IRuntimeClasspathEntryResolver {

    @Override
    public IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry(IRuntimeClasspathEntry entry, ILaunchConfiguration configuration) throws CoreException {
        if (entry == null || entry.getJavaProject() == null) {
            return new IRuntimeClasspathEntry[0];
        }
        return resolveRuntimeClasspathEntry(entry, entry.getJavaProject());
    }

    @Override
    public IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry(IRuntimeClasspathEntry entry, IJavaProject project) throws CoreException {
        if (entry.getType() != IRuntimeClasspathEntry.CONTAINER || !entry.getPath().equals(GradleClasspathContainer.CONTAINER_PATH)) {
            return new IRuntimeClasspathEntry[0];
        }

        IJavaProject javaProject = entry.getJavaProject();
        IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(), javaProject);

        List<IRuntimeClasspathEntry> result = Lists.newArrayList();
        for (final IClasspathEntry cpe : container.getClasspathEntries()) {
            if (cpe.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                result.add(JavaRuntime.newArchiveRuntimeClasspathEntry(cpe.getPath()));
            } else if (cpe.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                Optional<IProject> candidate = CorePlugin.workspaceOperations().findProject(accessibleJavaProject(cpe.getPath()));
                if (candidate.isPresent()) {
                    IJavaProject dependencyProject = JavaCore.create(candidate.get());
                    result.add(JavaRuntime.newProjectRuntimeClasspathEntry(dependencyProject));
                }
            }
        }

        return result.toArray(new IRuntimeClasspathEntry[result.size()]);
    }

    private static Predicate<IProject> accessibleJavaProject(final IPath projectPath) {
        return new Predicate<IProject>() {

            @Override
            public boolean apply(IProject project) {
                try {
                    return project.isAccessible() && project.getFullPath().equals(projectPath) && project.hasNature(JavaCore.NATURE_ID);
                } catch (CoreException e) {
                    return false;
                }
            }
        };
    }

    @Override
    public IVMInstall resolveVMInstall(IClasspathEntry entry) throws CoreException {
        return null;
    }

}
