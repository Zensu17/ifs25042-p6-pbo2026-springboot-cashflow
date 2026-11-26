package org.delcom.ifs25042springbootcashflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DotEnvTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsValidEntriesAndSkipsCommentsAndExistingProps() throws IOException {
        Path missing = tempDir.resolve("missing.env");
        DotEnv.load(missing);

        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, """
                # comment line
                
                INVALID_LINE
                =no_key
                DOTENV_TEST_PLAIN=hello
                DOTENV_TEST_DOUBLE="quoted value"
                DOTENV_TEST_SINGLE='single value'
                DOTENV_TEST_SHORT=x
                DOTENV_TEST_MISMATCH="abc'
                DOTENV_TEST_MISMATCH2='abc"
                PATH=should_not_override_env
                """);

        System.setProperty("DOTENV_TEST_EXISTING", "initial");
        try {
            DotEnv.load();
            DotEnv.load(envFile);
            DotEnv.applyLine("DOTENV_TEST_EXISTING=changed");

            assertThat(System.getProperty("DOTENV_TEST_PLAIN")).isEqualTo("hello");
            assertThat(System.getProperty("DOTENV_TEST_DOUBLE")).isEqualTo("quoted value");
            assertThat(System.getProperty("DOTENV_TEST_SINGLE")).isEqualTo("single value");
            assertThat(System.getProperty("DOTENV_TEST_SHORT")).isEqualTo("x");
            assertThat(System.getProperty("DOTENV_TEST_MISMATCH")).isEqualTo("\"abc'");
            assertThat(System.getProperty("DOTENV_TEST_MISMATCH2")).isEqualTo("'abc\"");
            assertThat(System.getProperty("DOTENV_TEST_EXISTING")).isEqualTo("initial");
            assertThat(System.getProperty("PATH")).isNull();
        } finally {
            System.clearProperty("DOTENV_TEST_PLAIN");
            System.clearProperty("DOTENV_TEST_DOUBLE");
            System.clearProperty("DOTENV_TEST_SINGLE");
            System.clearProperty("DOTENV_TEST_SHORT");
            System.clearProperty("DOTENV_TEST_MISMATCH");
            System.clearProperty("DOTENV_TEST_MISMATCH2");
            System.clearProperty("DOTENV_TEST_EXISTING");
        }
    }
}
