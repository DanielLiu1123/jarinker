package jarinker.cli.cmd;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * Root command for Jarinker CLI.
 *
 * @author Freeman
 */
@Command(
        name = "jarinker",
        description = "A JAR shrinker that analyzes dependencies and removes unused classes",
        version = "0.0.1",
        mixinStandardHelpOptions = true)
public class RootCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        // Show help when no subcommand is specified
        spec.commandLine().usage(System.out);
    }
}
