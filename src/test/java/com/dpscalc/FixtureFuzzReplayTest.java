package com.dpscalc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Assume;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FixtureFuzzReplayTest {
    private static final String COUNT_PROPERTY = "fixtureFuzz.count";
    private static final String SEED_PROPERTY = "fixtureFuzz.seed";
    private static final String INDEX_PROPERTY = "fixtureFuzz.index";
    private static final String SURFACE_PROPERTY = "fixtureFuzz.surface";
    private static final String DEFAULT_SURFACE = "baseline";
    private static final long DEFAULT_SEED = 0x5eed2026L;
    private static final long GENERATOR_TIMEOUT_MINUTES = 15;

    @Test
    public void fuzzGeneratedFixtures() throws Exception {
        int count = readIntProperty(COUNT_PROPERTY, 0);
        Assume.assumeTrue("Set -PfixtureFuzzCount=<n> or -DfixtureFuzz.count=<n> to run fixture fuzz replay", count > 0);

        long seed = readLongProperty(SEED_PROPERTY, DEFAULT_SEED);
        Integer startIndex = readOptionalIntProperty(INDEX_PROPERTY);
        String surface = readStringProperty(SURFACE_PROPERTY, DEFAULT_SURFACE);
        Path output = Files.createTempFile("osrs-dps-fixture-fuzz-", ".json");

        try {
            runGenerator(seed, count, startIndex, surface, output);
            JsonObject root = readJson(output);
            assertEquals("Fuzzer generated unexpected surface", surface, root.getAsJsonObject("generator").get("surface").getAsString());
            JsonArray fixtures = root.getAsJsonArray("fixtures");
            assertEquals("Fuzzer generated unexpected fixture count", count, fixtures.size());

            for (int offset = 0; offset < fixtures.size(); offset++) {
                JsonObject fixture = fixtures.get(offset).getAsJsonObject();
                int globalIndex = startIndex == null ? offset : startIndex + offset;
                String context = String.format(
                    "surface=%s seed=%d count=%d index=%d reproduce='%s'",
                    surface,
                    seed,
                    count,
                    globalIndex,
                    reproductionCommand(surface, seed, globalIndex)
                );
                FixtureReplayAssertions.assertFixture(fixture, context);
            }
        } finally {
            Files.deleteIfExists(output);
        }
    }

    private static void runGenerator(long seed, int count, Integer startIndex, String surface, Path output) throws IOException, InterruptedException {
        Path repoRoot = findRepoRoot();
        List<String> command = new ArrayList<>();
        command.add("yarn");
        command.add("sync-fuzz-fixtures");
        command.add("--surface");
        command.add(surface);
        command.add("--seed");
        command.add(Long.toString(seed));
        command.add("--count");
        command.add(Integer.toString(count));
        command.add("--output");
        command.add(output.toString());
        if (startIndex != null) {
            command.add("--index");
            command.add(Integer.toString(startIndex));
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(repoRoot.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        boolean finished = process.waitFor(GENERATOR_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        String processOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!finished) {
            process.destroyForcibly();
            fail("Fixture fuzz generator timed out: " + String.join(" ", command));
        }
        if (process.exitValue() != 0) {
            fail(String.format("Fixture fuzz generator failed with exit %d: %s%n%s", process.exitValue(), String.join(" ", command), processOutput));
        }
    }

    private static JsonObject readJson(Path output) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(output), StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, JsonObject.class);
        }
    }

    private static Path findRepoRoot() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
            if (Files.isRegularFile(candidate.resolve("package.json")) && Files.isRegularFile(candidate.resolve("sync-tests/generate-fixtures.ts"))) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to locate repository root from " + current);
    }

    private static String reproductionCommand(String surface, long seed, int index) {
        return String.format(
            "JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew fixtureFuzzTest -PfixtureFuzzSurface=%s -PfixtureFuzzSeed=%d -PfixtureFuzzCount=1 -PfixtureFuzzIndex=%d",
            surface,
            seed,
            index
        );
    }

    private static int readIntProperty(String name, int defaultValue) {
        String value = System.getProperty(name);
        return value == null || value.isEmpty() ? defaultValue : Integer.decode(value);
    }

    private static long readLongProperty(String name, long defaultValue) {
        String value = System.getProperty(name);
        return value == null || value.isEmpty() ? defaultValue : Long.decode(value);
    }

    private static Integer readOptionalIntProperty(String name) {
        String value = System.getProperty(name);
        return value == null || value.isEmpty() ? null : Integer.decode(value);
    }

    private static String readStringProperty(String name, String defaultValue) {
        String value = System.getProperty(name);
        return value == null || value.isEmpty() ? defaultValue : value;
    }
}
