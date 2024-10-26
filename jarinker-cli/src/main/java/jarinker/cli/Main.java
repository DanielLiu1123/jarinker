package jarinker.cli;

import static io.goodforgod.graalvm.hint.annotation.ReflectionHint.AccessType.ALL_DECLARED;
import static io.goodforgod.graalvm.hint.annotation.ReflectionHint.AccessType.ALL_PUBLIC;

import io.goodforgod.graalvm.hint.annotation.ReflectionHint;
import jarinker.cli.cmd.RootCommand;
import jarinker.cli.cmd.scan.ScanCommand;
import jarinker.cli.cmd.shrink.ShrinkCommand;
import picocli.AutoComplete;
import picocli.CommandLine;

/**
 * @author Freeman
 * @since 2024/10/22
 */
public class Main {

    public static void main(String[] args) {

        var rootCmd = new CommandLine(new RootCommand())
                .addSubcommand("completion", new AutoComplete.GenerateCompletion())
                .addSubcommand(new ShrinkCommand())
                .addSubcommand(new ScanCommand());

        System.exit(rootCmd.execute(args));
    }

    @ReflectionHint(
            types = AutoComplete.GenerateCompletion.class,
            value = {ALL_DECLARED, ALL_PUBLIC
            }) // for GenerateCompletion, picocli only generate graalvm reflection config for "your own" commands
    static class ReflectionHit {}
}
