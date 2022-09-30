package org.apache.hadoop.conf;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ConfigurationGenerator extends Generator<Configuration> {

    private static String PARAM_EQUAL_MARK = "=";
    private static String PARAM_VALUE_SPLITOR = ";";

    /* Current Fuzzing Test Class Name; Set with -Dclass=XXX */
    private static String clzName = null;
    /* Currently Fuzzing Test Method Name; Set with -Dmethod=XXX */
    private static String methodName = null;

    /* File path that stores all parameter constraints (e.g., valid values) */
    private static String constraintFile = null;
    /* Mapping that keeps all parameter valid values supported (from the constraint file) */
    private static Map<String, List<String>> paramConstraintMapping = null;

    /* Mapping that let generator know which configuration parameter to fuzz */
    private static Map<String, String> curTestMapping = null;


    /**
     * Constructor for Configuration Generator
     */
    public ConfigurationGenerator() throws IOException {
        super(Configuration.class);
        clzName = System.getProperty("class");
        methodName = System.getProperty("method");
        constraintFile = System.getProperty("constraint.file", "constraint");

        /* Use TreeMap to sort the configuration set to prevent ordering inconsistency;
           Initialize the mapping with all default configuration set for the first round */
        curTestMapping = ConfigurationTracker.getInitConfigMap();
        paramConstraintMapping = parseParamConstraint();
    }

    /**
     * This method is invoked to generate a Configuration object
     * @param random
     * @param generationStatus
     * @return
     */
    @Override
    public Configuration generate(SourceOfRandomness random, GenerationStatus generationStatus) {
        //Initialize a Configuration object
        Configuration conf = new Configuration();
        if (clzName == null || methodName == null) {
            throw new RuntimeException("Must specify test class name and test method name!");
        }
        curTestMapping = ConfigurationTracker.getAndFreshConfigMap();
        if (curTestMapping == null) {
            throw new RuntimeException("Unable to get configuration mapping for current test: " + clzName + "#" +
                    methodName);
        }
        // System.out.println("Current generate value for: " + clzName + "#" + methodName);
        // Here should be a for loop to set all the configuration parameter that used in the set;
        // For now we use TestIdentityProviders#testPluggableIdentityProvider as an example, which
        // only sets one param: CommonConfigurationKeys.IPC_IDENTITY_PROVIDER_KEY

        // conf.set(CommonConfigurationKeys.IPC_IDENTITY_PROVIDER_KEY, random.nextBytes(100).toString());
        // conf.setInt("fs.ftp.host.port", random.nextInt())

        // TODO: monitor config dynamically
        // curTestMapping is a sorted TreeMap
        for (Map.Entry<String, String> entry : curTestMapping.entrySet()) {
            if (!isNullOrEmpty(entry.getValue())) {
                //System.out.println("No." + i++ + "Looping " + entry.getKey());
                try {
                    String randomValue = randomValue(entry.getKey(), entry.getValue(), random);
                    conf.set(entry.getKey(), randomValue);
                } catch (Exception e) {
                    System.out.println(" Configuration Name: " + entry.getKey() + " value " + entry.getValue() + " Exception:");
                    e.printStackTrace();
                    continue;
                }
            }
        }
	    return conf;
    }

    /**
     * Return a random value based on the type of @param value
     * @param value
     * @return
     */
    // TODO: Types can be cached the type rather than doing it for mutiple times.
    // Pass the type instead of the value and check the type

    private static String randomValue(String name, String value, SourceOfRandomness random) {
        // TODO: Next to find a way to randomly generate string that we don't know
        // Some parameter may only be able to fit into such values
        if (paramHasConstraints(name)) {
            return randomValueFromConstraint(name, random);
        }
        if (isBoolean(value)) {
            return String.valueOf(random.nextBoolean());
        } else if (isInteger(value)) {
            return String.valueOf(random.nextInt());
        } else if (isFloat(value)) {
            return String.valueOf(random.nextFloat());
        } 
        // for now we only fuzz numeric and boolean configuration parameters.
        String returnStr = String.valueOf(random.nextBytes(10));
        //System.out.println("Generating random String for " + name + " : " + returnStr);
        return returnStr;
        //return value;
    }

    private static String randomValueFromConstraint(String name, SourceOfRandomness random) {
        return random.choose(paramConstraintMapping.get(name));
    }

    private static boolean paramHasConstraints(String name) {
        return paramConstraintMapping.containsKey(name);
    }

    private static Map<String, List<String>> parseParamConstraint() throws IOException {
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        File file = Paths.get(constraintFile).toFile();
        if (!file.exists() || !file.isFile()){
            throw new IOException("Unable to read file: " + file.getPath());
        }
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        int index;
        while ((line = br.readLine()) != null) {
            index = line.indexOf(PARAM_EQUAL_MARK);
            if (index != -1) {
                String name = line.substring(0, index - 1).trim();
                /* Only continue parsing when this parameter is used by current fuzzing test */
                if (curTestMapping.containsKey(name)) {
                    String[] values = line.substring(index + 1).split(PARAM_VALUE_SPLITOR);
                    List<String> valueList = new ArrayList(Arrays.asList(values));
                    result.put(name, valueList);
                }
            }
        }
        return result;
    }


    /** Helper Functions */
    private static boolean isInteger(String value) {
        try {
            int i = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private static boolean isBoolean(String value) {
        String trimStr = value.toLowerCase().trim();
        if (trimStr.equals("true") || trimStr.equals("false")) {
            return true;
        }
        return false;
    }

    private static boolean isFloat(String value) {
        try {
            float f = Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private static boolean isNullOrEmpty(String value) {
        return value.equals("null") || value == null || value.equals("");
    }

    /** For Internal test */
    public static void printListMap(Map<String, List<String>> map) {
        System.out.println("In printing!!");
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String name = entry.getKey();
            String value = "";
            for (String s : entry.getValue()) {
                value = value + ";" + s;
            }
            System.out.println(name + "=" + value);
        }
    }

    public static void printMap(Map<String, String> map) {
        System.out.println("In printing!!");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            System.out.println(name + "=" + value);
        }
        System.out.println("Number of map size = " + map.size());
    }


    public static void main(String[] args) {
        Path file = Paths.get("/home/swang516/xlab/test_file");
        try {
            ConfigurationGenerator cg = new ConfigurationGenerator();
            printMap(curTestMapping);
            // curTestMapping = readFileToMapping(file);
            // // for (Map.Entry<String, String> entry : curTestMapping.entrySet()) {
            // //     System.out.println(entry.getKey() + "=" + entry.getValue());
            // // }

            // for (Map.Entry<String, String> entry : curTestMapping.entrySet()) {
            //     if (!isNullOrEmpty(entry.getValue())) {
            //         System.out.println(entry.getKey() + "= old: " + entry.getValue() + " new: " + randomValue(entry.getKey(), entry.getValue(), random));
            //     }
            // }
        } catch(Exception e) {
           e.printStackTrace();
        }
    }
}
