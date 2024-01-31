import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class SWNNode {
    String name;
    double weight;
    int pre;
    int pos;
    List<SWNNode> childList;
    SWNNode parent;

    public SWNNode(String name, double weight, int pre, int pos, List<SWNNode> childList, SWNNode parent) {
        this.name = name;
        this.weight = weight;
        this.pre = pre;
        this.pos = pos;
        this.childList = (childList != null) ? childList : new ArrayList<>();
        this.parent = parent;
    }

    @Override
    public String toString() {
        return String.format("SWNNode(name=%s, weight=%.2f, pre=%d, pos=%d)", name, weight, pre, pos);
    }
}

class TailElement {
    private List<String> transaction;
    private SWNNode node;
    private double weight;

    public TailElement(List<String> transaction, SWNNode node, double weight) {
        this.transaction = transaction;
        this.node = node;
        this.weight = weight;
    }

    public List<String> getTransaction() {
        return transaction;
    }

    public SWNNode getNode() {
        return node;
    }

    public double getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return String.format("Tail(transaction=" + this.transaction + ", node=" + this.node + ", weight=" + this.weight);
    }
}

public class SWNTree {
    static int preCounter = 0;
    static int postCounter = 1;

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
                    String column2 = matcher.group(2).replaceAll("\\[|\\]", "");

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

    public static List<Map<String, String>> calculateTw(List<Map<String, String>> data, Map<String, Integer> itemWeights) {
        return data.stream()
                .map(row -> {
                    String itemsHeader = "Items";
                    String itemsString = row.get(itemsHeader);
    
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

    public static List<String> customSort(List<String> transaction, List<Map<String, String>> df, int ti) {
        List<String> sortingTransaction = new ArrayList<>(transaction);
        List<String> storeSortedT = new ArrayList<>();
    
        for (int i = 0; i < df.size(); i++) {
            if (!storeSortedT.isEmpty()) {
                sortingTransaction = new ArrayList<>(storeSortedT);
                storeSortedT.clear();
            }
            List<String> storeRemainingItems = new ArrayList<>();
            int check = 0;
    
            if (i == ti && i != df.size() - 1) {
                continue;
            }
            else {
                for (int j = 0; j < sortingTransaction.size(); j++) {
                    if (df.get(i).get("Items").contains(sortingTransaction.get(j)) && !storeSortedT.contains(sortingTransaction.get(j))) {
                        storeSortedT.add(sortingTransaction.get(j));
                    } else {
                        storeRemainingItems.add(sortingTransaction.get(j));
                    }
    
                    if (j == sortingTransaction.size() - 1) {
                        check = 1;
                    }
                }
                if (check == 1) {
                    storeRemainingItems.addAll(storeSortedT);
                    storeSortedT = new ArrayList<>(storeRemainingItems);
                }
            }
        }
    
        Collections.reverse(storeSortedT);
        sortingTransaction = new ArrayList<>(storeSortedT);
        return sortingTransaction;
    }

    public static void depthFirstSearch(SWNNode node) {
        if (node != null) {
            node.pre = preCounter++;
            for (SWNNode child : node.childList) {
                depthFirstSearch(child);
            }
            node.pos = postCounter++;
        }
    }

    public static void insertTree(List<String> transaction, SWNNode root, double tw, List<TailElement> tail) {
        SWNNode currentNode = root;
        for (String item : transaction) {
            boolean found = false;
            for (SWNNode child : currentNode.childList) {
                if (child.name.equals(item)) {
                    child.weight += tw;
                    currentNode = child;
                    found = true;
                    break;
                }
            }
            if (!found) {
                SWNNode newNode = new SWNNode(item, tw, 0, 0, new ArrayList<>(), currentNode);
                currentNode.childList.add(newNode);
                currentNode = newNode;
            }
        }
        tail.add(new TailElement(transaction, currentNode, tw));
    }

    public static void swnTreeConstruction(List<Map<String, String>> data, SWNNode root, List<TailElement> tail) {
        for (int i = 0; i < data.size(); i++) {
            List<String> sortedT = customSort(Arrays.asList(data.get(i).get("Items").split(", ")), data, i);
            insertTree(sortedT, root, Double.parseDouble(data.get(i).get("tw")), tail);
        }
        depthFirstSearch(root);
    }

    public static void printSWNTree(SWNNode node, int indent) {
        if (node != null) {
            if ("root".equals(node.name)) {
                for (int i = 0; i < indent; i++) {
                    System.out.print("  ");
                }
                System.out.println(node.name);
            } else {
                for (int i = 0; i < indent; i++) {
                    System.out.print("  ");
                }
                System.out.printf("%s (Weight: %.2f, Pre: %d, Post: %d)%n",
                        node.name, node.weight, node.pre, node.pos);
            }
            for (SWNNode child : node.childList) {
                printSWNTree(child, indent + 1);
            }
        }
    }

    public static void printTail(List<TailElement> tail) {
        System.out.println("TAIL:");
        for (int i = 0; i < tail.size(); i++) {
            List<String> transaction = tail.get(i).getTransaction();
            SWNNode node = tail.get(i).getNode();
            System.out.printf("TAIL [T%d]: The sorted transaction of T%d is {%s}%n",
                    i + 1, i + 1, String.join(" <-> ", transaction));
            System.out.printf("T%d: root", i + 1);
            while (node != null) {
                if (!"root".equals(node.name)) {
                    node = node.parent;
                } else {
                    break;
                }
            }
            for(String item : transaction) {
                if(node != null) {
                    SWNNode childNode = node.childList.stream()
                            .filter(child -> child.name.equals(item))
                            .findFirst()
                            .orElse(null);
                    if (childNode != null) {
                        System.out.printf(" <-> %s, %.2f", item, childNode.weight);
                        node = childNode;
                    } else {
                        System.out.printf(" <-> %s, 0.00", item);
                    }
                }
                else {
                    System.out.printf(" <-> %s, 0.00", item);
                }
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        int windowSize = 5;

        List<Map<String, String>> df = readTransactionData("dataSets.csv");
        Map<String, Integer> itemWeights = readItemWeights("itemsWeights.csv");
        df = calculateTw(df, itemWeights);

        int originalPreCounter = preCounter;
        int originalPostCounter = postCounter;

        for (int i = 0; i <= df.size() - windowSize; i++) {
            preCounter = originalPreCounter;
            postCounter = originalPostCounter;

            List<Map<String, String>> window = df.subList(i, i + windowSize);
            int windowNumber = i + 1;
        
            System.out.println("Window " + windowNumber + ":");
            window.forEach(System.out::println);
        
            SWNNode swnTree = new SWNNode("root", 0, 0, 0, new ArrayList<>(), null);
            List<TailElement> tail = new ArrayList<>();
        
            swnTreeConstruction(window, swnTree, tail);
        
            printSWNTree(swnTree, 0);

            printTail(tail);
        }
    }
}