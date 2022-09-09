package org.jenkinsci.gradle.plugins.jpi.legacy;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.util.GradleVersion;
import org.jenkinsci.gradle.plugins.jpi.internal.JpiExtensionBridge;

public class LegacyWorkaroundsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        // workarounds for JENKINS-26331
        TaskContainer tasks = project.getTasks();
        tasks.named("test").configure(new Action<Task>() {
            @Override
            public void execute(Task t) {
                t.doFirst(new Action<Task>() {
                    @Override
                    public void execute(Task first) {
                        JpiExtensionBridge ext = project.getExtensions().getByType(JpiExtensionBridge.class);
                        Provider<String> jenkinsVersion = ext.getJenkinsCoreVersion();
                        if (isBetween(jenkinsVersion.get(), "1.545", "1.592")) {
                            project.file("target").mkdirs();
                        }
                    }
                });
            }
        });
        tasks.named("clean", Delete.class).configure(new Action<Delete>() {
            @Override
            public void execute(Delete t) {
                t.doFirst(new Action<Task>() {
                    @Override
                    public void execute(Task first) {
                        JpiExtensionBridge ext = project.getExtensions().getByType(JpiExtensionBridge.class);
                        Provider<String> jenkinsVersion = ext.getJenkinsCoreVersion();
                        if (isOlderThan(jenkinsVersion.get(), "1.598")) {
                            t.delete("target");
                        }
                    }
                });
            }
        });
    }

    private static boolean isBetween(String subject, String lowerBoundInclusive, String upperExclusive) {
        GradleVersion current = GradleVersion.version(subject);
        GradleVersion lower = GradleVersion.version(lowerBoundInclusive);
        return current.compareTo(lower) >= 0 && isOlderThan(subject, upperExclusive);
    }

    private static boolean isOlderThan(String subject, String upperExclusive) {
        GradleVersion current = GradleVersion.version(subject);
        GradleVersion upper = GradleVersion.version(upperExclusive);
        return current.compareTo(upper) < 0;
    }
}
