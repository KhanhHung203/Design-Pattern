import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class dataFrameForBenmark {

    private static List<String> generateItemNames(int index) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        int baseLen = alphabet.length();
        List<String> itemNames = new ArrayList<>();
        StringBuilder item = new StringBuilder();
        while (index >= 0) {
            item.insert(0, alphabet.charAt(index % baseLen));
            index /= baseLen;
            index -= 1;
        }
        itemNames.add(item.toString());
        return itemNames;
    }

    private static Map<String, Double> generateItemWeights(List<String> itemNames) {
        Random random = new Random();
        Map<String, Double> itemWeights = new HashMap<>();
        for (String itemName : itemNames) {
            itemWeights.put(itemName, Math.round((0.1 + random.nextDouble() * 0.9) * 100.0) / 100.0);
        }
        return itemWeights;
    }

    public static List<List<String>> generateTransactions(List<String> itemNames, int numTransactions, int numItems, int avgLength) {
        List<List<String>> transactions = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < numTransactions; i++) {
            int numItemsInTransaction = (int) Math.round(random.nextDouble() * (2 * avgLength - 2) + 1);
            numItemsInTransaction = Math.max(1, Math.min(numItemsInTransaction, numItems));
            List<String> transactionItems = new ArrayList<>(itemNames);
            Collections.shuffle(transactionItems);
            transactionItems = transactionItems.subList(0, numItemsInTransaction);
            transactions.add(transactionItems);
        }
        return transactions;
    }

    private static void writeCsv(String fileName, List<String> headers, List<?>... columns) {
        try (FileWriter csvWriter = new FileWriter(fileName)) {
            // Write headers
            csvWriter.append(String.join(",", headers));
            csvWriter.append("\n");
    
            // Determine the minimum size of columns
            int minSize = Integer.MAX_VALUE;
            for (List<?> column : columns) {
                minSize = Math.min(minSize, column.size());
            }
    
            // Write data rows up to the minimum size
            for (int i = 0; i < minSize; i++) {
                List<String> row = new ArrayList<>();
                for (List<?> column : columns) {
                    row.add(column.get(i).toString());
                }
                csvWriter.append(String.join(",", row));
                csvWriter.append("\n");
            }
    
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String[]> readCsv(String fileName) {
        List<String[]> data = new ArrayList<>();

        try (Scanner scanner = new Scanner(dataFrame.class.getResourceAsStream(fileName))) {
            // Read the data rows
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                // Use regex to separate the columns
                Pattern pattern = Pattern.compile("(.*?),\\s*(.*)");
                Matcher matcher = pattern.matcher(line);

                if (matcher.matches()) {
                    String column1 = matcher.group(1);
                    String column2 = matcher.group(2);

                    // Create a new row with the columns
                    String[] row = new String[]{column1, column2};
                    data.add(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return data;
    }

    private static List<Integer> range(int start, int end) {
        List<Integer> result = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            result.add(i);
        }
        return result;
    }

    public static void main(String[] args) {
        // Define the number of transactions and items for each dataset
        Map<String, Map<String, Integer>> datasets = new HashMap<>();
        datasets.put("Custom", new HashMap<String, Integer>() {{
            put("numTransactions", 1000);
            put("numItems", 27);
            put("avgLength", 15);
        }});

        // Create CSV files for each dataset
        for (Map.Entry<String, Map<String, Integer>> entry : datasets.entrySet()) {
            String dataset = entry.getKey();
            Map<String, Integer> params = entry.getValue();

            int numTransactions = params.get("numTransactions");
            int numItems = params.get("numItems");
            int avgLength = params.get("avgLength");

            List<String> itemNames = new ArrayList<>();
            for (int i = 0; i < numItems; i++) {
                itemNames.addAll(generateItemNames(i));
            }
            Map<String, Double> itemWeights = generateItemWeights(itemNames);
            List<List<String>> transactions = generateTransactions(itemNames, numTransactions, numItems, avgLength);

            // Create lists for item and weight columns
            List<String> itemColumn = new ArrayList<>(itemWeights.keySet());
            List<Double> weightColumn = new ArrayList<>(itemWeights.values());

            // Generate CSV files for data
            try {
                writeCsv(dataset + "_data.csv", Arrays.asList("TID", "Items"), range(1, numTransactions + 1), transactions);
            } catch (Exception e) {
                e.printStackTrace();
            }
            writeCsv(dataset + "_weights.csv", Arrays.asList("Items", "Weights"), itemColumn, weightColumn);

            // Print information about the dataset
            double density = (double) avgLength / numItems;
            String datasetType = density >= 0.1 ? "dense" : "sparse";

            System.out.println("\nDataset: " + dataset);
            System.out.println("Number of transactions: " + numTransactions);
            System.out.println("Number of items: " + numItems);
            System.out.println("Average length: " + avgLength);
            System.out.println("Density: " + density);
            System.out.println("Type: " + datasetType);
        }

        // Read the transaction data from the generated CSV files
        for (Map.Entry<String, Map<String, Integer>> entry : datasets.entrySet()) {
            String dataset = entry.getKey();
            String filename = dataset + "_data.csv";
            String weightsFilename = dataset + "_weights.csv";

            List<String[]> df1 = readCsv(filename);
            List<String[]> df2 = readCsv(weightsFilename);

            // Convert item weights to a dictionary
            Map<String, Double> itemWeights = new HashMap<>();
            for (int i = 1; i < df2.size(); i++) {
                String[] row = df2.get(i);
                itemWeights.put(row[0], Double.parseDouble(row[1]));
            }

            // Calculate transaction weighted (tw)
            // Split the items in the 'Items' column and calculate the tw for each transaction
            List<Map<String, Object>> df1_tw = new ArrayList<>();
            for (int i = 1; i < df1.size(); i++) {
                String[] row = df1.get(i);
                String[] itemsInTransaction = row[1].split(", ");
                double tw = Arrays.stream(itemsInTransaction)
                        .mapToDouble(item -> itemWeights.getOrDefault(item, 0.0))
                        .average()
                        .orElse(0.0);
                Map<String, Object> newRow = new HashMap<>();
                newRow.put("TID", row[0]);
                newRow.put("tw", tw);
                df1_tw.add(newRow);
            }

            // Create the DataFrame for tw
            List<Map<String, Object>> df_tw = new ArrayList<>();
            for (Map<String, Object> row : df1_tw) {
                Map<String, Object> newRow = new HashMap<>();
                newRow.put("TID", row.get("TID"));
                newRow.put("tw", row.get("tw"));
                df_tw.add(newRow);
            }

            // Calculate TTW (Total Transaction Weights)
            double TTW = df1_tw.stream().mapToDouble(row -> (double) row.get("tw")).sum();

            // Calculate weighted support (ws)
            Set<String> items = itemWeights.keySet();
            List<Map<String, Object>> df_ws = new ArrayList<>();
            for (String item : items) {
                double totalTwWithItem = df1_tw.stream()
                    .filter(row -> {
                        String tid = row.get("TID").toString();
                        Optional<String[]> dataSetRowOptional = df1.stream()
                                .filter(dataSetRow -> dataSetRow[0].equals(tid))
                                .findFirst();
                        return dataSetRowOptional.isPresent() && dataSetRowOptional.get().length > 1 &&
                        Arrays.asList(dataSetRowOptional.get()[1].substring(1, dataSetRowOptional.get()[1].length() - 1).split(", "))
                                .contains(item);
                    })
                    .mapToDouble(row -> (double) row.get("tw"))
                    .sum();
                double ws = totalTwWithItem / TTW;
                Map<String, Object> newRow = new HashMap<>();
                newRow.put("Items", item);
                newRow.put("ws", ws);
                df_ws.add(newRow);
            }

            // Sort the DataFrame by 'ws' in descending order
            df_ws.sort(Comparator.<Map<String, Object>, Double>comparing(row -> (double) row.get("ws")).reversed());

            // Display the DataFrames
            System.out.println("\nTable example for tw (" + dataset + "):");
            for (Map<String, Object> row : df_tw) {
                System.out.println(row);
            }

            System.out.println("\nTTW (" + dataset + "):");
            System.out.println(TTW);

            System.out.println("\nTable example for ws (" + dataset + "):");
            for (Map<String, Object> row : df_ws) {
                System.out.println(row);
            }
        }
    }
}
