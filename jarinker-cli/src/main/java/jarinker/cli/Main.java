package jarinker.cli;

import static io.goodforgod.graalvm.hint.annotation.ReflectionHint.AccessType.ALL_DECLARED;
import static io.goodforgod.graalvm.hint.annotation.ReflectionHint.AccessType.ALL_PUBLIC;

import io.goodforgod.graalvm.hint.annotation.ReflectionHint;
import jarinker.cli.cmd.RootCommand;
import jarinker.cli.cmd.analyze.AnalyzeCommand;
import jarinker.cli.cmd.shrink.ShrinkCommand;
import picocli.AutoComplete;
import picocli.CommandLine;

/**
 * @author Freeman
 */
@ReflectionHint(
        types = AutoComplete.GenerateCompletion.class,
        value = {ALL_DECLARED, ALL_PUBLIC
        }) // for GenerateCompletion, picocli only generate graalvm reflection config for "your own" commands
public class Main {

    public static void main(String[] args) {

        var rootCmd = new CommandLine(new RootCommand())
                .addSubcommand("completion", new AutoComplete.GenerateCompletion())
                .addSubcommand(new AnalyzeCommand())
                .addSubcommand(new ShrinkCommand());

        System.exit(rootCmd.execute(args));
    }
}
