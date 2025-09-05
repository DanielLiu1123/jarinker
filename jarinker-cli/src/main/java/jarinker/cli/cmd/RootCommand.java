package jarinker.cli.cmd;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * Root command for Jarinker CLI.
 * Do not change version property, it will be replaced by gradle.properties:2
 *
 * @author Freeman
 */
@Command(
        name = "jarinker",
        mixinStandardHelpOptions = true,
        version = "0.1.0",
        description = "A tool for shrinking JAR files by removing unused classes.")
public class RootCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        // Show --help if no subcommand/options are provided
        spec.commandLine().usage(System.out);
    }
}
