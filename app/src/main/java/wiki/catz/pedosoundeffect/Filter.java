package wiki.catz.pedosoundeffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Filter {
    private static final Map<String, double[]> COEFFICIENTS_LOW_0_HZ = new HashMap<>();
    private static final Map<String, double[]> COEFFICIENTS_LOW_5_HZ = new HashMap<>();
    private static final Map<String, double[]> COEFFICIENTS_HIGH_1_HZ = new HashMap<>();

    static {
        COEFFICIENTS_LOW_0_HZ.put("alpha", new double[]{1, -1.979133761292768, 0.979521463540373});
        COEFFICIENTS_LOW_0_HZ.put("beta", new double[]{0.000086384997973502, 0.000172769995947004, 0.000086384997973502});

        COEFFICIENTS_LOW_5_HZ.put("alpha", new double[]{1, -1.80898117793047, 0.827224480562408});
        COEFFICIENTS_LOW_5_HZ.put("beta", new double[]{0.095465967120306, -0.172688631608676, 0.095465967120306});

        COEFFICIENTS_HIGH_1_HZ.put("alpha", new double[]{1, -1.905384612118461, 0.910092542787947});
        COEFFICIENTS_HIGH_1_HZ.put("beta", new double[]{0.953986986993339, -1.907503180919730, 0.953986986993339});
    }

    public static List<Double> low_0_hz(List<Double> data) {
        return filter(data, COEFFICIENTS_LOW_0_HZ);
    }

    public static List<Double> low_5_hz(List<Double> data) {
        return filter(data, COEFFICIENTS_LOW_5_HZ);
    }

    public static List<Double> high_1_hz(List<Double> data) {
        return filter(data, COEFFICIENTS_HIGH_1_HZ);
    }

    private static List<Double> filter(List<Double> data, Map<String, double[]> coefficients) {
        List<Double> filteredData = new ArrayList<>();
        filteredData.add(0.0);
        filteredData.add(0.0);
        for (int i = 2; i < data.size(); i++) {
            double filteredValue = Objects.requireNonNull(coefficients.get("alpha"))[0] *
                    (data.get(i) * Objects.requireNonNull(coefficients.get("beta"))[0] +
                            data.get(i - 1) * Objects.requireNonNull(coefficients.get("beta"))[1] +
                            data.get(i - 2) * Objects.requireNonNull(coefficients.get("beta"))[2] -
                            filteredData.get(i - 1) * Objects.requireNonNull(coefficients.get("alpha"))[1] -
                            filteredData.get(i - 2) * Objects.requireNonNull(coefficients.get("alpha"))[2]);
            filteredData.add(filteredValue);
        }
        return filteredData;
    }
}

