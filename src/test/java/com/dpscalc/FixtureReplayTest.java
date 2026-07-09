package com.dpscalc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@RunWith(Parameterized.class)
public class FixtureReplayTest {

    private final String fixtureId;
    private final String fixtureName;
    private final JsonObject fixture;

    public FixtureReplayTest(String id, String name, JsonObject fixture) {
        this.fixtureId = id;
        this.fixtureName = name;
        this.fixture = fixture;
    }

    @Parameterized.Parameters(name = "{0}: {1}")
    public static Collection<Object[]> loadFixtures() {
        List<Object[]> testCases = new ArrayList<>();

        try (InputStream is = FixtureReplayTest.class.getResourceAsStream("/fixtures.json")) {
            if (is == null) {
                System.err.println("Warning: fixtures.json not found. Run generate-fixtures.ts first.");
                return Collections.emptyList();
            }

            Gson gson = new Gson();
            JsonObject root = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
            JsonArray fixtures = root.getAsJsonArray("fixtures");

            for (JsonElement elem : fixtures) {
                JsonObject fixture = elem.getAsJsonObject();
                String id = fixture.get("id").getAsString();
                String name = fixture.get("name").getAsString();
                testCases.add(new Object[]{id, name, fixture});
            }
        } catch (Exception e) {
            System.err.println("Error loading fixtures: " + e.getMessage());
        }

        return testCases;
    }

    @Test
    public void testFixture() {
        FixtureReplayAssertions.assertFixture(fixture, String.format("[%s] %s", fixtureId, fixtureName));
    }

}
