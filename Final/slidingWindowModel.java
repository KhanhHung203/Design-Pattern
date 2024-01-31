import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class slidingWindowModel {

    // Function to read the transaction data from a CSV file
    public static List<Map<String, String>> readTransactionData(String filename) {
        List<Map<String, String>> data = new ArrayList<>();
        try (Scanner scanner = new Scanner(new FileReader(filename))) {
            String[] headers = scanner.nextLine().split(",");
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                // Use regex to separate the columns
                Pattern pattern = Pattern.compile("(.*?),\\s*(.*)");
                Matcher matcher = pattern.matcher(line);

                if (matcher.matches()) {
                    String column1 = matcher.group(1);
                    String column2 = matcher.group(2);

                    Map<String, String> row = new HashMap<>();
                    row.put(headers[0], column1);
                    row.put(headers[1], column2);
                    data.add(row);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    // Function to read the item weights data from a CSV file
    public static Map<String, Integer> readItemWeights(String filename) {
        Map<String, Integer> itemWeights = new HashMap<>();
        try (Scanner scanner = new Scanner(new FileReader(filename))) {
            String[] headers = scanner.nextLine().split(",");
            while (scanner.hasNextLine()) {
                String[] values = scanner.nextLine().split(",");
                itemWeights.put(values[0], Integer.parseInt(values[1]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return itemWeights;
    }

    // Function to calculate transaction weighted (tw)
    public static List<Map<String, String>> calculateTw(List<Map<String, String>> data, Map<String, Integer> itemWeights) {
        return data.stream()
                .map(row -> {
                    String itemsHeader = "Items";
                    String itemsString = row.get(itemsHeader).replaceAll("\\[|\\]", "");
    
                    List<String> itemsInTransaction = Arrays.asList(itemsString.split(", "));
                    double tw = itemsInTransaction.stream()
                            .mapToDouble(item -> itemWeights.getOrDefault(item, 0))
                            .average()
                            .orElse(0.0);
                    row.put("tw", Double.toString(tw));
                    return row;
                })
                .collect(Collectors.toList());
    }

    // Function to calculate weighted support (ws) for each transaction in a window
    public static List<Double> calculateWsForWindow(List<Map<String, String>> window) {
        double windowTtw = window.stream()
                .mapToDouble(row -> Double.parseDouble(row.get("tw")))
                .sum();
    
        return window.stream()
                .map(row -> {
                    String itemsString = row.get("Items").replaceAll("\\[|\\]", "");
    
                    Set<String> itemsInTransaction = new HashSet<>(Arrays.asList(itemsString.split(", ")));
                    double tw = window.stream()
                            .filter(transaction -> {
                                // Fix the header "Items" by removing "[" and "]" characters
                                String transactionItemsString = transaction.get("Items").replaceAll("\\[|\\]", "");
                                Set<String> transactionItems = new HashSet<>(Arrays.asList(transactionItemsString.split(", ")));
                                return transactionItems.containsAll(itemsInTransaction);
                            })
                            .mapToDouble(transaction -> Double.parseDouble(transaction.get("tw")))
                            .sum();
                    return tw / windowTtw;
                })
                .collect(Collectors.toList());
    }

    // Function to implement sliding window model
    public static void slidingWindow(List<Map<String, String>> data, int windowSize, double minWs) {
        for (int i = 0; i <= data.size() - windowSize; i++) {
            List<Map<String, String>> window = data.subList(i, i + windowSize);
            int windowNumber = i + 1;

            System.out.println("Window " + windowNumber + ":");
            window.forEach(System.out::println);

            List<Double> wsList = calculateWsForWindow(window);

            for (int j = 0; j < window.size(); j++) {
                if (wsList.get(j) >= minWs) {
                    System.out.println("Transaction " + (i + j + 1) + " in Window " + windowNumber + " is a FWP with WS: " + wsList.get(j));
                }
            }
        }
    }

    // Main function
    public static void main(String[] args) {
        int windowSize = 5;
        double minWs = 0.3;

        // Read the transaction data and item weights data from CSV files
        List<Map<String, String>> df = readTransactionData("dataSets.csv");
        Map<String, Integer> itemWeights = readItemWeights("itemsWeights.csv");

        // Calculate transaction weighted (tw)
        df = calculateTw(df, itemWeights);

        // Implement sliding window model to find FWPs
        slidingWindow(df, windowSize, minWs);
    }
}
