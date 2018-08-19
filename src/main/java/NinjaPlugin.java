import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.WarPlugin;

/**
 * @author Olaleye Afolabi <olaleyeone@gmail.com>
 */
public class NinjaPlugin implements Plugin<Project> {

    public static final String STANDALONE = "standalone";
    public static final String NINJA_RUN_TASK_NAME = "ninjaRun";

    @Override
    public void apply(Project project) {

        project.getPluginManager().apply(WarPlugin.class);

        project.getTasks().create(NINJA_RUN_TASK_NAME, NinjaStandalone.class, (task) -> {
            task.dependsOn("clean", "compileJava");

            project.getConfigurations().create(STANDALONE);
        });
    }
}
