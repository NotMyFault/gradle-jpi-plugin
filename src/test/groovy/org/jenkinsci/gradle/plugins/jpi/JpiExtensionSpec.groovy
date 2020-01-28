package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class JpiExtensionSpec extends Specification {
    Project project = Mock(Project)
    JpiExtension jpiExtension = new JpiExtension(project)

    def 'short name is project name when not set'() {
        when:
        project.name >> 'awesome'

        then:
        jpiExtension.shortName == 'awesome'
    }

    def 'short name is project name without -plugin suffix when not set'() {
        when:
        project.name >> 'test-plugin'

        then:
        jpiExtension.shortName == 'test'
    }

    def 'short name is used when set'() {
        when:
        jpiExtension.shortName = 'acme'

        then:
        jpiExtension.shortName == 'acme'
    }

    def 'display name is project name when not set'() {
        when:
        project.name >> 'awesome'

        then:
        jpiExtension.displayName == 'awesome'
    }

    def 'display name is project name without -plugin suffix when not set'() {
        when:
        project.name >> 'test-plugin'

        then:
        jpiExtension.displayName == 'test'
    }

    def 'display name is short name when not set'() {
        when:
        jpiExtension.shortName = 'acme'

        then:
        jpiExtension.displayName == 'acme'
    }

    def 'display name is used when set'() {
        when:
        jpiExtension.displayName = 'foo'

        then:
        jpiExtension.displayName == 'foo'
    }

    def 'file extension defaults to hpi if not set'(String value) {
        when:
        jpiExtension.fileExtension = value

        then:
        jpiExtension.fileExtension == 'hpi'

        where:
        value << [null, '']
    }

    def 'file extension is used when set'() {
        when:
        jpiExtension.fileExtension = 'jpi'

        then:
        jpiExtension.fileExtension == 'jpi'
    }

    def 'localizer output directory defaults to generated-src/localizer if not set'(Object value) {
        when:
        Project project = ProjectBuilder.builder().build()
        JpiExtension jpiExtension = new JpiExtension(project)
        jpiExtension.localizerOutputDir = value

        then:
        jpiExtension.localizerOutputDir == new File(project.buildDir, 'generated-src/localizer')

        where:
        value << [null, '']
    }

    def 'localizer output directory is used when set'() {
        when:
        Project project = ProjectBuilder.builder().build()
        JpiExtension jpiExtension = new JpiExtension(project)
        jpiExtension.localizerOutputDir = 'foo'

        then:
        jpiExtension.localizerOutputDir == new File(project.rootDir, 'foo')
    }

    def 'work directory defaults to work if not set'() {
        when:
        Project project = ProjectBuilder.builder().build()
        JpiExtension jpiExtension = new JpiExtension(project)
        jpiExtension.workDir = null

        then:
        jpiExtension.workDir == new File(project.projectDir, 'work')
    }

    def 'work directory defaults to work in child project the extension is applied to if not set'() {
        when:
        Project parent = ProjectBuilder.builder().build()
        Project project = ProjectBuilder.builder().withParent(parent).build()
        JpiExtension jpiExtension = new JpiExtension(project)
        jpiExtension.workDir = null

        then:
        jpiExtension.workDir == new File(project.projectDir, 'work')
    }

    def 'work directory is used when set'() {
        when:
        Project project = ProjectBuilder.builder().build()
        JpiExtension jpiExtension = new JpiExtension(project)
        File dir = new File('/tmp/foo')
        jpiExtension.workDir = dir

        then:
        jpiExtension.workDir == dir
    }

    def 'repo URL defaults to repo.jenkins-ci.org if not set'(String value) {
        when:
        jpiExtension.repoUrl = value

        then:
        jpiExtension.repoUrl == 'https://repo.jenkins-ci.org/releases'

        where:
        value << [null, '']
    }

    def 'repo URL is used when set'() {
        when:
        jpiExtension.repoUrl = 'https://maven.example.org/'

        then:
        jpiExtension.repoUrl == 'https://maven.example.org/'
    }

    def 'repo URL is overridden by project property'() {
        setup:
        System.properties['jpi.repoUrl'] = 'https://acme.org/'

        when:
        jpiExtension.repoUrl = 'any'

        then:
        jpiExtension.repoUrl == 'https://acme.org/'

        cleanup:
        System.properties.remove('jpi.repoUrl')
    }

    def 'snapshot repo URL defaults to repo.jenkins-ci.org if not set'(String value) {
        when:
        jpiExtension.snapshotRepoUrl = value

        then:
        jpiExtension.snapshotRepoUrl == 'https://repo.jenkins-ci.org/snapshots'

        where:
        value << [null, '']
    }

    def 'snapshot repo URL is used when set'() {
        when:
        jpiExtension.snapshotRepoUrl = 'https://maven.example.org/'

        then:
        jpiExtension.snapshotRepoUrl == 'https://maven.example.org/'
    }

    def 'snapshot repo URL is overridden by System property'() {
        setup:
        System.properties['jpi.snapshotRepoUrl'] = 'https://acme.org/'

        when:
        jpiExtension.snapshotRepoUrl = 'any'

        then:
        jpiExtension.snapshotRepoUrl == 'https://acme.org/'

        cleanup:
        System.properties.remove('jpi.snapshotRepoUrl')
    }

    def 'core versions earlier than 1.420 are not supported'(String version) {
        when:
        jpiExtension.coreVersion = version

        then:
        thrown(GradleException)

        where:
        version << ['1.419.99', '1.390', '1.1']
    }

    def 'jenkinsCore dependencies'() {
        setup:
        Project project = ProjectBuilder.builder().build()

        when:
        project.with {
            apply plugin: 'jpi'
            jenkinsPlugin {
                coreVersion = '1.509.3'
            }
        }

        then:
        def dependencies = collectDependencies(project, 'compileOnly')
        'org.jenkins-ci.main:jenkins-core:1.509.3' in dependencies
        'javax.servlet:servlet-api:2.4' in dependencies
        'findbugs:annotations:1.0.0' in dependencies
    }

    def 'jenkinsTest dependencies before 1.505'() {
        setup:
        Project project = ProjectBuilder.builder().build()

        when:
        project.with {
            apply plugin: 'jpi'
            jenkinsPlugin {
                coreVersion = '1.504'
            }
        }

        then:
        def dependenciesCompileTime = collectDependencies(project, 'testImplementation')
        def dependenciesRuntime = collectDependencies(project, 'testRuntimeOnly')
        'org.jenkins-ci.main:jenkins-test-harness:1.504' in dependenciesCompileTime
        'org.jenkins-ci.main:ui-samples-plugin:1.504' in dependenciesCompileTime
        'org.jenkins-ci.main:jenkins-war:1.504' in dependenciesRuntime
        'junit:junit-dep:4.10' in dependenciesCompileTime
    }

    def 'jenkinsTest dependencies before 1.533'() {
        setup:
        Project project = ProjectBuilder.builder().build()

        when:
        project.with {
            apply plugin: 'jpi'
            jenkinsPlugin {
                coreVersion = '1.532'
            }
        }

        then:
        def dependenciesCompileTime = collectDependencies(project, 'testImplementation')
        def dependenciesRuntime = collectDependencies(project, 'testRuntimeOnly')
        'org.jenkins-ci.main:jenkins-test-harness:1.532' in dependenciesCompileTime
        'org.jenkins-ci.main:ui-samples-plugin:1.532' in dependenciesCompileTime
        'org.jenkins-ci.main:jenkins-war:1.532' in dependenciesRuntime
    }

    def 'jenkinsTest dependencies for 1.533 or later'() {
        setup:
        Project project = ProjectBuilder.builder().build()

        when:
        project.with {
            apply plugin: 'jpi'
            jenkinsPlugin {
                coreVersion = '1.533'
            }
        }

        then:
        def dependenciesCompileTime = collectDependencies(project, 'testImplementation')
        def dependenciesRuntime = collectDependencies(project, 'testRuntimeOnly')
        'org.jenkins-ci.main:jenkins-test-harness:1.533' in dependenciesCompileTime
        'org.jenkins-ci.main:ui-samples-plugin:2.0' in dependenciesCompileTime
        'org.jenkins-ci.main:jenkins-war:1.533' in dependenciesRuntime
    }

    def 'jenkinsTest dependencies for 1.645 or later'() {
        setup:
        Project project = ProjectBuilder.builder().build()

        when:
        project.with {
            apply plugin: 'jpi'
            jenkinsPlugin {
                coreVersion = '1.645'
            }
        }

        then:
        def dependenciesCompileTime = collectDependencies(project, 'testImplementation')
        def dependenciesRuntime = collectDependencies(project, 'testRuntimeOnly')
        'org.jenkins-ci.main:jenkins-test-harness:2.0' in dependenciesCompileTime
        'org.jenkins-ci.main:ui-samples-plugin:2.0' in dependenciesCompileTime
        'org.jenkins-ci.main:jenkins-war:1.645' in dependenciesRuntime
    }

    def 'jenkinsTest dependencies for 2.64 or later'() {
        setup:
        Project project = ProjectBuilder.builder().build()

        when:
        project.with {
            apply plugin: 'jpi'
            jenkinsPlugin {
                coreVersion = '2.64'
            }
        }

        then:
        def dependenciesCompileTime = collectDependencies(project, 'testImplementation')
        'org.jenkins-ci.main:jenkins-test-harness:2.60' in dependenciesCompileTime
        'org.jenkins-ci.main:ui-samples-plugin:2.0' in dependenciesCompileTime
    }

    private static collectDependencies(Project project, String configuration) {
        project.configurations.getByName(configuration).dependencies.collect {
            "${it.group}:${it.name}:${it.version}".toString()
        }
    }
}
