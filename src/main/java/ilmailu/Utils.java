package ilmailu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    private static final BigDecimal ISA_BAROMETRIC_PRESSURE_SEA_LEVEL = new BigDecimal("1013.25");
    private static final BigDecimal ISA_SEA_LEVEL_TEMPERATURE = new BigDecimal("15.00");
    private static final BigDecimal ISA_LAPSE_RATE_CELSIUS_PER_1000_FEET = new BigDecimal("-1.98");
    private static final BigDecimal DENSITY_ALTITUDE_CONSTANT = new BigDecimal("120");
    private static final BigDecimal TRUE_ALTITUDE_CONSTANT = new BigDecimal("273");

    private static final BigDecimal ISA_PRESSURE_LAPSE_RATE_FEET_PER_HPA = new BigDecimal("27");

    public static BigDecimal densityAltitudeFeet(BigDecimal pressureAltitudeFeet, BigDecimal outsideAirTemperatureCelsius) {
        BigDecimal isaDeviation = isaDeviation(pressureAltitudeFeet, outsideAirTemperatureCelsius);
        logger.debug("...for density altitude, ISA deviation is {}", isaDeviation);
        return pressureAltitudeFeet.add(DENSITY_ALTITUDE_CONSTANT.multiply(isaDeviation));
    }

    public static BigDecimal isaDeviation(BigDecimal altitude, BigDecimal outsideAirTemperature) {
        return outsideAirTemperature.subtract(expectedIsaTemperature(altitude)).setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean isColderThanIsa(BigDecimal altitude, BigDecimal outsideAirTemperature) {
        return isaDeviation(altitude, outsideAirTemperature).signum() == -1;
    }

    public static BigDecimal expectedIsaTemperature(BigDecimal altitude) {
        BigDecimal lapse = altitude.divide(new BigDecimal("1000.00"), 2, RoundingMode.HALF_UP).multiply(ISA_LAPSE_RATE_CELSIUS_PER_1000_FEET);
        return ISA_SEA_LEVEL_TEMPERATURE.add(lapse);
    }

    public static BigDecimal trueAltitudeFromIndicatedAltitude(BigDecimal indicatedAltitude, BigDecimal outsideAirTemperature) {
        BigDecimal temperatureDeviation = isaDeviation(indicatedAltitude, outsideAirTemperature);
        BigDecimal altitudeDeviation = temperatureDeviation.multiply(indicatedAltitude).divide(TRUE_ALTITUDE_CONSTANT, 2, RoundingMode.HALF_UP);
        return indicatedAltitude.add(altitudeDeviation);
    }

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
