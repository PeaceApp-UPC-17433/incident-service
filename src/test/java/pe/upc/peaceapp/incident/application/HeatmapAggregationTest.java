package pe.upc.peaceapp.incident.application;

import org.junit.jupiter.api.Test;
import pe.upc.peaceapp.incident.domain.model.Severity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HeatmapAggregationTest {

    private static Object[] at(double lat, double lng) {
        return new Object[]{lat, lng};
    }

    @Test
    void placesEachCellAtTheCentroidOfItsRealIncidents() {
        var cells = IncidentQueryService.aggregate(List.of(at(-12.1001, -77.0001), at(-12.1003, -77.0003)), 0.01);

        assertThat(cells).hasSize(1);
        assertThat(cells.get(0).count()).isEqualTo(2);
        assertThat(cells.get(0).latitude()).isCloseTo(-12.1002, within(1e-9));
        assertThat(cells.get(0).longitude()).isCloseTo(-77.0002, within(1e-9));
    }

    @Test
    void keepsNearbyIncidentsInSeparateCellsWhenTheyFallInDifferentOnes() {
        var cells = IncidentQueryService.aggregate(List.of(at(-12.1001, -77.0001), at(-12.2001, -77.1001)), 0.01);

        assertThat(cells).hasSize(2);
        assertThat(cells).allSatisfy(c -> assertThat(c.count()).isEqualTo(1));
    }

    @Test
    void derivesRiskFromTheNumberOfIncidentsInTheCell() {
        var many = new java.util.ArrayList<Object[]>();
        for (int i = 0; i < 10; i++) many.add(at(-12.1 + i * 1e-5, -77.0));

        assertThat(IncidentQueryService.aggregate(many, 0.01).get(0).risk()).isEqualTo(Severity.HIGH);
        assertThat(IncidentQueryService.aggregate(java.util.Collections.singletonList(at(-12.1, -77.0)), 0.01).get(0).risk()).isEqualTo(Severity.LOW);
    }

    @Test
    void emptyInputGivesNoCells() {
        assertThat(IncidentQueryService.aggregate(List.of(), 0.01)).isEmpty();
    }
}
