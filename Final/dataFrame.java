import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class dataFrame {

    private static List<String> generateRandomItems(int numItems) {
        List<String> items = Arrays.asList("A", "B", "C", "D", "E");
        Collections.shuffle(items);
        return items.subList(0, numItems);
    }
    //Input:...
    //Output:...
    //Function:...

    private static Map<String, Integer> generateRandomWeights() {
        Random random = new Random();
        Map<String, Integer> weights = new HashMap<>();
        List<String> items = Arrays.asList("A", "B", "C", "D", "E");
        for (String item : items) {
            weights.put(item, random.nextInt(7) + 1);
        }
        return weights;
    }

    private static void writeCsv(String fileName, List<String> headers, List<?>... columns) {
        try (FileWriter csvWriter = new FileWriter(fileName)) {
            // Write headers
            csvWriter.append(String.join(",", headers));
            csvWriter.append("\n");

            // Write data rows
            for (int i = 0; i < columns[0].size(); i++) {
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

    public static void main(String[] args) {
        // Define the number of transactions and items
        int numTransactions = 10;
        int numItems = 5;

        // Create lists to store data
        List<Integer> transactionIds = new ArrayList<>();
        List<List<String>> items = new ArrayList<>();

        // Generate the transactions
        for (int i = 1; i <= numTransactions; i++) {
            int numItemsInTransaction = new Random().nextInt(numItems) + 1;
            List<String> transactionItems = generateRandomItems(numItemsInTransaction);
            transactionIds.add(i);
            items.add(transactionItems);
        }

        // Generate random weights for items
        Map<String, Integer> itemWeights = generateRandomWeights();

        // Create lists for item and weight columns
        List<String> itemColumn = new ArrayList<>(itemWeights.keySet());
        List<Integer> weightColumn = new ArrayList<>(itemWeights.values());

        // Create CSV files for data
        writeCsv("dataSets.csv", Arrays.asList("TID", "Items"), transactionIds, items);
        writeCsv("itemsWeights.csv", Arrays.asList("Items", "Weights"), itemColumn, weightColumn);

        // Read the transaction data from dataSets.csv
        List<String[]> dataSet = readCsv("/dataSets.csv");

        // Read the item weights data from itemsWeights.csv
        List<String[]> itemWeightsData = readCsv("/itemsWeights.csv");

        // Print the contents of dataSets
        System.out.println("\ndataSets:");
        for (String[] row : dataSet) {
            System.out.println(row[0] + ", " + row[1]);
        }

        // Print the contents of itemsWeights
        System.out.println("\nitemsWeights:");
        for (String[] row : itemWeightsData) {
            System.out.println(row[0] + ", " + row[1]);
        }

        ///////////////////////////////////////////////////////////////////////////////////////////

        // Convert item weights to a dictionary
        Map<String, Integer> convertedIW = new HashMap<>();
        for (int i = 1; i < itemWeightsData.size(); i++) {
            String[] row = itemWeightsData.get(i);
            convertedIW.put(row[0], Integer.parseInt(row[1]));
        }

        // Calculate transaction weighted (tw) and create the DataFrame for tw
        List<Map<String, Object>> df_tw = new ArrayList<>();
        for (int i = 1; i < dataSet.size(); i++) {
            String[] row = dataSet.get(i);
            String[] items_in_transaction = row[1].substring(1, row[1].length() - 1).split(", ");
            double tw = Arrays.stream(items_in_transaction)
                    .mapToDouble(item -> {
                        Integer weight = convertedIW.get(item);
                        return weight != null ? weight : 0.0;
                    })
                    .average()
                    .orElse(0.0);

            Map<String, Object> newRow = new HashMap<>();
            newRow.put("TID", row[0]);
            newRow.put("tw", tw);
            df_tw.add(newRow);
        }

        // Calculate TTW (Total Transaction Weights)
        double TTW = df_tw.stream().mapToDouble(row -> (double) row.get("tw")).sum();

        // Calculate weighted support (ws) and create the DataFrame for ws
        Set<String> itemsInItemWeights = convertedIW.keySet();
        List<Map<String, Object>> df_ws = new ArrayList<>();
        for (String item : itemsInItemWeights) {
            double total_tw_with_item = df_tw.stream()
                    .filter(row -> {
                        String tid = row.get("TID").toString();
                        // Find the corresponding row in dataSet by matching TID
                        Optional<String[]> dataSetRowOptional = dataSet.stream()
                                .filter(dataSetRow -> dataSetRow[0].equals(tid))
                                .findFirst();

                        // Check if the item is present in the items list of the dataSet row
                        return dataSetRowOptional.isPresent() &&
                                Arrays.asList(dataSetRowOptional.get()[1].substring(1, dataSetRowOptional.get()[1].length() - 1).split(", "))
                                        .contains(item);
                    })
                    .mapToDouble(row -> (double) row.get("tw"))
                    .sum();
            double ws = total_tw_with_item / TTW;
            Map<String, Object> newRow = new HashMap<>();
            newRow.put("Items", item);
            newRow.put("ws", ws);
            df_ws.add(newRow);
        }

        // Sort the DataFrame by 'ws' in descending order
        df_ws.sort(Comparator.<Map<String, Object>, Double>comparing(row -> (double) row.get("ws")).reversed());

        // Display the DataFrames
        System.out.println("\nTable example for tw:");
        for (Map<String, Object> row : df_tw) {
            System.out.println(row);
        }

        System.out.println("\nTTW:");
        System.out.println(TTW);

        System.out.println("\nTable example for ws:");
        for (Map<String, Object> row : df_ws) {
            System.out.println(row);
        }
    }
}