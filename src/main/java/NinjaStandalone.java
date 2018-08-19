import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.JavaExecSpec;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaModuleDependency;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Olaleye Afolabi <olaleyeone@gmail.com>
 */
public class NinjaStandalone extends DefaultTask {

    private String pathToMainClass = "com/javalee/webrunner/MainClass.class";

    public NinjaStandalone(String pathToMainClass) {
        this.pathToMainClass = pathToMainClass;
    }

    public NinjaStandalone() {
    }

    @TaskAction
    public void startJetty() {

        Project project = getProject();

        project.javaexec(new Action<JavaExecSpec>() {

            @Override
            public void execute(JavaExecSpec javaExecSpec) {
                ProjectConnection connection = null;
                Set<String> classpath = new HashSet<>();


                try {

                    GradleConnector gradleConnector = GradleConnector.newConnector();
                    gradleConnector.useInstallation(project.getGradle().getGradleHomeDir());
                    connection = gradleConnector.forProjectDirectory(project.getProjectDir()).connect();

                    IdeaProject ideaProject = connection.model(IdeaProject.class).get();

                    IdeaModule module = ideaProject.getModules().stream()
                            .filter(it -> it.getGradleProject().getPath().equals(project.getPath()))
                            .findFirst().get();

                    Path mainClassFile = module.getGradleProject().getBuildDirectory()
                            .toPath()
                            .resolve(Paths.get("classes", "java", "main"))
                            .resolve(Paths.get(".", pathToMainClass.split("/")));

                    Files.createDirectories(mainClassFile.getParent());
                    Files.deleteIfExists(mainClassFile);
                    try (InputStream is = getClass().getResourceAsStream("/" + pathToMainClass)) {
                        Files.copy(is, mainClassFile);
                    }

                    classpath.add(project.getProperties().get("webAppDir").toString());
                    classpath.addAll(getClasspath(module).stream().map(path -> path.toString()).collect(Collectors.toList()));

                    dependencies(module, IdeaModuleDependency.class).forEach(it -> {
                        if (it.getScope().getScope().toLowerCase().matches("runtime|compile")) {
                            classpath.addAll(getClasspath(it.getDependencyModule()).stream().map(path -> path.toString()).collect(Collectors.toList()));
                        }
                    });
                    dependencies(module, IdeaSingleEntryLibraryDependency.class).forEach(it -> {
                        if (it.getScope().getScope().toLowerCase().matches("runtime|compile")) {
                            classpath.add(it.getFile().getAbsolutePath());
                        }
                    });

                    Configuration standalone = project.getConfigurations().getByName(NinjaPlugin.STANDALONE);
                    standalone.getDependencies().forEach(dep -> {
                        classpath.addAll(standalone.files(dep)
                                .stream().map(it -> it.getAbsolutePath())
                                .collect(Collectors.toList()));
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } finally {
                    if (connection != null) {
                        connection.close();
                    }
                }
                javaExecSpec.setClasspath(project.files(classpath));

                javaExecSpec.setMain(pathToMainClass.replaceAll("/", ".")
                        .replace(".class", ""));
            }
        });
    }

    public <T> Stream<T> dependencies(IdeaModule module, final Class<T> type) {
        return module.getDependencies().stream()
                .filter(t -> type.isInstance(t))
                .map(t -> (T) t);
    }

    public List<Path> getClasspath(IdeaModule ideaModule) {
        List<Path> paths = new ArrayList<>();
        Path path = ideaModule.getGradleProject().getBuildDirectory().toPath();
        ideaModule.getContentRoots().getAll().forEach(contentRoot -> {
            contentRoot.getResourceDirectories().forEach(it -> paths.add(it.getDirectory().toPath()));
        });
        paths.add(path.resolve(Paths.get("classes", "java", "main")));
        return paths;
    }
}
