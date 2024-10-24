package jarinker.cli.cmd;

import jarinker.cli.Main;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * @author Freeman
 * @since 2024/10/15
 */
@Command(name = "jarinker", mixinStandardHelpOptions = true, versionProvider = RootCommand.VersionProvider.class)
public class RootCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        // Show --help if no subcommand/options are provided
        spec.commandLine().usage(System.out);
    }

    static final class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[] {"Jarinker " + Main.VERSION};
        }
    }
}
