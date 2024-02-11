package ilmailu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    /**
     * Constant for ISA (International Standard Atmosphere) Barometric Pressure at Sea Level in hPa
     */
    private static final BigDecimal ISA_BAROMETRIC_PRESSURE_SEA_LEVEL = new BigDecimal("1013.25");

    /**
     * Constant for ISA (International Standard Atmosphere) Temperature at Sea Level in Celsius
     */
    private static final BigDecimal ISA_SEA_LEVEL_TEMPERATURE = new BigDecimal("15.00");

    /**
     * Constant for ISA (International Standard Atmosphere) Temperature lapse rate in Celsius degrees per 1000 feet
     */
    private static final BigDecimal ISA_LAPSE_RATE_CELSIUS_PER_1000_FEET = new BigDecimal("-1.98");

    /**
     * Constant for density altitude multiplier
     */
    private static final BigDecimal DENSITY_ALTITUDE_CONSTANT = new BigDecimal("120");

    /**
     * Constant for true altitude divisor
     */
    private static final BigDecimal TRUE_ALTITUDE_CONSTANT = new BigDecimal("273");

    /**
     * Constant for ISA (International Standard Atmosphere) altitude lapse rate, defined in feet per hPa
     */
    private static final BigDecimal ISA_PRESSURE_LAPSE_RATE_FEET_PER_HPA = new BigDecimal("27");

    /**
     * For a given pressure altitude and outside temperature, calculate the density altitude
     *
     * @param pressureAltitudeFeet         Pressure altitude in feet above ISA MSL (1013,25 hPa)
     * @param outsideAirTemperatureCelsius Outside air temperature in Celsius
     * @return Density altitude in feet
     */
    public static BigDecimal densityAltitudeFeet(BigDecimal pressureAltitudeFeet, BigDecimal outsideAirTemperatureCelsius) {
        BigDecimal isaDeviation = isaDeviation(pressureAltitudeFeet, outsideAirTemperatureCelsius);
        logger.debug("...for density altitude, ISA deviation is {}", isaDeviation);
        return pressureAltitudeFeet.add(DENSITY_ALTITUDE_CONSTANT.multiply(isaDeviation));
    }

    /**
     * For a given altitude and outside air temperature, calculate how much the temperature deviates from ISA temperature at the same altitude
     *
     * @param altitude              Altitude in feet
     * @param outsideAirTemperature Outside air temperature in Celsius
     * @return Deviation from ISA temperature. Negative if outside are is colder than ISA, positive if warmer, zero if same.
     */
    public static BigDecimal isaDeviation(BigDecimal altitude, BigDecimal outsideAirTemperature) {
        return outsideAirTemperature.subtract(expectedIsaTemperature(altitude)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Checks if a given temperature at a given altitude is colder than the ISA temperature at the same altitude
     *
     * @param altitude              Altitude in feet
     * @param outsideAirTemperature Outside air temperature in Celsius
     * @return true if temperature is colder, false if same or warmer
     */
    public static boolean isColderThanIsa(BigDecimal altitude, BigDecimal outsideAirTemperature) {
        return isaDeviation(altitude, outsideAirTemperature).signum() == -1;
    }

    /**
     * Calculate the expected ISA temperature at a given altitude using the ISA defined lapse rate of 1.98C/1000ft
     *
     * @param altitude Altitude in feet
     * @return Expected temperature in Celsius
     */
    public static BigDecimal expectedIsaTemperature(BigDecimal altitude) {
        BigDecimal lapse = altitude.divide(new BigDecimal("1000.00"), 2, RoundingMode.HALF_UP).multiply(ISA_LAPSE_RATE_CELSIUS_PER_1000_FEET);
        return ISA_SEA_LEVEL_TEMPERATURE.add(lapse);
    }

    /**
     * Calculates true altitude from indicated altitude using the following formula
     * True Altitude = Indicated Altitude + ( ISA Deviation × Indicated Altitude / 273 )
     *
     * @param indicatedAltitude     Indicated altitude in feet
     * @param outsideAirTemperature Outside air temperature in Celsius
     * @return True altitude in feet
     */
    public static BigDecimal trueAltitudeFromIndicatedAltitude(BigDecimal indicatedAltitude, BigDecimal outsideAirTemperature) {
        BigDecimal temperatureDeviation = isaDeviation(indicatedAltitude, outsideAirTemperature);
        BigDecimal altitudeDeviation = temperatureDeviation.multiply(indicatedAltitude).divide(TRUE_ALTITUDE_CONSTANT, 2, RoundingMode.HALF_UP);
        return indicatedAltitude.add(altitudeDeviation);
    }

    /**
     * Calculates the pressure altitude in feet from a given indicated altitude. Note that if barometric pressure is QNE (1013,25 hPa) this returns
     * the value of indicatedAltitude.
     *
     * @param indicatedAltitude  Indicated altitude in feet
     * @param barometricPressure Current local barometric pressure (e.g QNH setting below Transition Altitude and QNE setting above it)
     * @return Pressure altitude in feet above ISA MSL (1013,25 hPA)
     */
    public static BigDecimal pressureAltitudeFromIndicatedAltitude(BigDecimal indicatedAltitude, BigDecimal barometricPressure) {
        BigDecimal pressureDifference = ISA_BAROMETRIC_PRESSURE_SEA_LEVEL.subtract(barometricPressure);
        BigDecimal altitudeDifference = pressureDifference.multiply(ISA_PRESSURE_LAPSE_RATE_FEET_PER_HPA);
        return indicatedAltitude.add(altitudeDifference);
    }

    public static BigDecimal pressureAltitudeFromIndicatedAltitude(int indicatedAltitude, int qnh) {
        return pressureAltitudeFromIndicatedAltitude(new BigDecimal(indicatedAltitude), new BigDecimal(qnh));
    }

    public static BigDecimal densityAltitudeFeet(BigDecimal pressureAltitude, int temperature) {
        return densityAltitudeFeet(pressureAltitude, new BigDecimal(temperature)).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal trueAltitudeFromIndicatedAltitude(int indicatedAltitude, int outsideAirTemperature) {
        return trueAltitudeFromIndicatedAltitude(new BigDecimal(indicatedAltitude), new BigDecimal(outsideAirTemperature));
    }
}
