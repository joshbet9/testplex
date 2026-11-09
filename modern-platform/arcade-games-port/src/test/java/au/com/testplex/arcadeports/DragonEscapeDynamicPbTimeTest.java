package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DragonEscapeDynamicPbTimeTest {
    @Test
    void interpolatesTimeByContinuousPositionAlongRecordedRoute() {
        List<DragonEscapeDynamicPbTime.Point> route = List.of(
                new DragonEscapeDynamicPbTime.Point(0, 0, 0, 0),
                new DragonEscapeDynamicPbTime.Point(1_000, 10, 0, 0),
                new DragonEscapeDynamicPbTime.Point(3_000, 10, 0, 20));

        assertEquals(500, DragonEscapeDynamicPbTime.expectedMillis(route, 5, 0, 0));
        assertEquals(2_000, DragonEscapeDynamicPbTime.expectedMillis(route, 10, 0, 10));
        assertEquals(3_000, DragonEscapeDynamicPbTime.expectedMillis(route, 10, 0, 30));
    }

    @Test
    void missingRouteHasNoExpectedTime() {
        assertEquals(-1, DragonEscapeDynamicPbTime.expectedMillis(List.of(), 0, 0, 0));
    }
}
