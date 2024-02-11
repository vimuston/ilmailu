package ilmailu;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class IlmailuTest {
    private static final Logger logger = LoggerFactory.getLogger(IlmailuTest.class);

    private final Map<String, Boolean> optionsEliminated = new HashMap<>();

    private static final Map<String, String> optionsTexts = new HashMap<>();

    @BeforeAll
    public static void beforeAll() {
        optionsTexts.put("A", "(Tiheyskorkeus on) Pienempi kuin painekorkeus ja suunnilleen sama kuin tosikorkeus.");
        optionsTexts.put("B", "(Tiheyskorkeus on) Suurempi kuin painekorkeus.");
        optionsTexts.put("C", "(Tiheyskorkeus on) Pienempi kuin tosikorkeus.");
        optionsTexts.put("D", "(Tiheyskorkeus on) Suurempi kuin tosikorkeus ja pienempi kuin painekorkeus.");
    }

    @BeforeEach()
    public void beforeEach() {
        optionsEliminated.put("A", false);
        optionsEliminated.put("B", false);
        optionsEliminated.put("C", false);
        optionsEliminated.put("D", false);
    }

    @Test
    public void testDensityAltitude() {

        int altitudeBegin = 0;
        int altitudeEnd = 35000;

        int qnhBegin = 998;
        int qnhEnd = 1083;

        int temperatureBegin = -30;
        int temperatureEnd = 40;

        logger.info("-------------------------------------------------------------------------------");
        logger.info("77 Jos ulkoilman lämpötila (OAT) on tietyllä korkeudella standardia matalampi, tiheyskorkeus on: ");
        logger.info("-------------------------------------------------------------------------------");

        logAssumptions();

        OUTER:
        for (int indicatedAltitude = altitudeBegin; indicatedAltitude <= altitudeEnd; indicatedAltitude += 100) {
            for (int qnh = qnhBegin; qnh <= qnhEnd; qnh++) {
                for (int temperature = temperatureBegin; temperature <= temperatureEnd; temperature++) {

                    BigDecimal pressureAltitude = Utils.pressureAltitudeFromIndicatedAltitude(indicatedAltitude, qnh);
                    BigDecimal densityAltitude = Utils.densityAltitudeFeet(pressureAltitude, temperature);
                    BigDecimal trueAltitude = Utils.trueAltitudeFromIndicatedAltitude(indicatedAltitude, temperature);

                    if (!Utils.isColderThanIsa(pressureAltitude, BigDecimal.valueOf(temperature))) {
                        continue;
                    }


                    // [A] (Tiheyskorkeus on) Pienempi kuin painekorkeus ja suunnilleen sama kuin tosikorkeus.
                    if (!isOptionEliminated("A") && densityAltitude.compareTo(pressureAltitude) > 0) {
                        logger.info("[A] (Tiheyskorkeus on) Pienempi kuin painekorkeus ja suunnilleen sama kuin tosikorkeus.");
                        logger.info("==> Ei päde näissä olosuhteissa");
                        logAltitudeInformation(BigDecimal.valueOf(qnh), BigDecimal.valueOf(indicatedAltitude), BigDecimal.valueOf(temperature), pressureAltitude, densityAltitude, trueAltitude);
                        eliminateOption("A");
                    }


                    // [B] (Tiheyskorkeus on) Suurempi kuin painekorkeus.
                    if (!isOptionEliminated("B") && densityAltitude.compareTo(pressureAltitude) < 0) {
                        logger.info("[B] (Tiheyskorkeus on) Suurempi kuin painekorkeus.");
                        logger.info("==> Ei päde näissä olosuhteissa");
                        logAltitudeInformation(BigDecimal.valueOf(qnh), BigDecimal.valueOf(indicatedAltitude), BigDecimal.valueOf(temperature), pressureAltitude, densityAltitude, trueAltitude);
                        eliminateOption("B");
                    }

                    // [C] (Tiheyskorkeus on) Pienempi kuin tosikorkeus.
                    if (!isOptionEliminated("C") && densityAltitude.compareTo(trueAltitude) > 0) {
                        logger.info("[C] (Tiheyskorkeus on) Pienempi kuin tosikorkeus.");
                        logger.info("==> Ei päde näissä olosuhteissa");
                        logAltitudeInformation(BigDecimal.valueOf(qnh), BigDecimal.valueOf(indicatedAltitude), BigDecimal.valueOf(temperature), pressureAltitude, densityAltitude, trueAltitude);

                        eliminateOption("C");
                    }


                    // [D] (Tiheyskorkeus on) Suurempi kuin tosikorkeus ja pienempi kuin painekorkeus.
                    if (!isOptionEliminated("D") && (densityAltitude.compareTo(trueAltitude) < 0 || densityAltitude.compareTo(pressureAltitude) > 0)) {
                        logger.info("[D] (Tiheyskorkeus on) Suurempi kuin tosikorkeus ja pienempi kuin painekorkeus.");
                        logger.info("==> Ei päde näissä olosuhteissa");
                        logAltitudeInformation(BigDecimal.valueOf(qnh), BigDecimal.valueOf(indicatedAltitude), BigDecimal.valueOf(temperature), pressureAltitude, densityAltitude, trueAltitude);

                        eliminateOption("D");
                    }

                    if (isAllOptionsElminated()) {
                        break OUTER;
                    }


                }
            }
        }
    }


    @Test
    public void testOptionsEliminated() {
        assertFalse(isAllOptionsElminated());
        optionsEliminated.put("A", true);
        assertFalse(isAllOptionsElminated());
        optionsEliminated.put("B", true);
        assertFalse(isAllOptionsElminated());
        optionsEliminated.put("C", true);
        assertFalse(isAllOptionsElminated());
        optionsEliminated.put("D", true);
        assertTrue(isAllOptionsElminated());
    }

    @Test
    public void testIsaDeviation() {
        assertEquals(new BigDecimal("0.00"), Utils.isaDeviation(new BigDecimal("0"), new BigDecimal("15")));
        assertEquals(new BigDecimal("1.98"), Utils.isaDeviation(new BigDecimal("1000"), new BigDecimal("15")));
        assertEquals(new BigDecimal("0.00"), Utils.isaDeviation(new BigDecimal("1000"), new BigDecimal("13.02")));
        assertEquals(new BigDecimal("0.00"), Utils.isaDeviation(new BigDecimal("2000"), new BigDecimal("11.04")));
    }

    @Test
    public void testColderThanIsa() {
        assertTrue(Utils.isColderThanIsa(new BigDecimal(0), BigDecimal.valueOf(14)));
        assertFalse(Utils.isColderThanIsa(new BigDecimal(0), BigDecimal.valueOf(15)));
        assertFalse(Utils.isColderThanIsa(new BigDecimal(0), BigDecimal.valueOf(16)));

        assertTrue(Utils.isColderThanIsa(new BigDecimal(1000), new BigDecimal("13.01")));
        assertFalse(Utils.isColderThanIsa(new BigDecimal(1000), new BigDecimal("13.02")));
        assertFalse(Utils.isColderThanIsa(new BigDecimal(1000), new BigDecimal("13.03")));
    }


    private boolean isAllOptionsElminated() {
        return optionsEliminated.values().stream().filter(option -> !option).findFirst().orElse(true);
    }

    private void logAssumptions() {
        logger.info("");
        logger.info("Lyhenteet");
        logger.info("---------");
        logger.info("OAT ........... Ulkolämpötila, outside air temperature");
        logger.info("ISA ........... ISA lämpötila tietyllä korkeudella");
        logger.info("ISADeviation .. OAT-ISA");
        logger.info("QNE ........... Paine-asetus 1013,25 hPa");
        logger.info("QNH ........... Paikallinen paine-asetus (hPa)");
        logger.info("");
        logger.info("Oletukset");
        logger.info("---------");
        logger.info("Tosikorkeus ..... Indikoitu korkeus + ((OAT-ISA) * Indikoitu korkeus / 273)");
        logger.info(" ==> Lähde https://ppla.education/navcomp/Calculation_of_True_Altitude_from_Indicated_Altitude_and_Temperature/");
        logger.info("Painekorkeus .... Korkeus paineasetuksella 1013,25 hPA");
        logger.info(" ==> Lähde: Kurssimateriaalissa");
        logger.info("Tiheyskorkeus ... Painekorkeus + ((OAT-ISA) * 120)");
        logger.info(" ==> Lähde: Kurssimateriaalissa");
        logger.info("Indikoidun korkeuden voi muuttaa painekorkeudeksi kaavalla: Indikoitu korkeus + (QNE-QNH) * 27 ");
        logger.info("");

    }

    private boolean isOptionEliminated(String option) {
        return optionsEliminated.get(option);
    }

    private void eliminateOption(String option) {
        optionsEliminated.put(option, true);
    }

    private void logAltitudeInformation(BigDecimal qnh, BigDecimal indicatedAltitude, BigDecimal temperature, BigDecimal pressureAltitude, BigDecimal densityAltitude, BigDecimal trueAltitude) {
        String altitudeInformation = "\n" +
                "QNH: .................. " + qnh + " hPa\n" +
                "Indikoitu korkeus: .... " + indicatedAltitude + " ft\n" +
                "Lämpötila: ............ " + temperature + " °C\n" +
                "Painekorkeus: ......... " + pressureAltitude + " ft\n" +
                "Tiheyskorkeus: ........ " + densityAltitude + " ft\n" +
                "Tosikorkeus: .......... " + trueAltitude + " ft\n" +
                "\n";

        logger.info(altitudeInformation);
    }


}

