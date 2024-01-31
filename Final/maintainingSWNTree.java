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
    List<String> transaction;
    SWNNode node;
    double weight;

    public TailElement(List<String> transaction, SWNNode node, double weight) {
        this.transaction = transaction;
        this.node = node;
        this.weight = weight;
    }

    @Override
    public String toString() {
        return String.format("Tail(transaction=" + this.transaction + ", node=" + this.node + ", weight=" + this.weight);
    }
}

public class maintainingSWNTree {
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

    private static void removeNodeFromTree(SWNNode root, TailElement targetNode) {
        Iterator<SWNNode> iterator = root.childList.iterator();
        while (iterator.hasNext()) {
            SWNNode child = iterator.next();
            if (child.pre == targetNode.node.pre && child.pos == targetNode.node.pos) {
                child.weight -= targetNode.weight;
                if (child.weight <= 0) {
                    iterator.remove();
                }
                return;
            }
            removeNodeFromTree(child, targetNode);
        }
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

    public static SWNNode MaintainingSWNTree(List<Map<String, String>> window, SWNNode root, Map<String, String> newT, List<TailElement> tail, int originalPreCounter, int originalPostCounter) {
        List<String> sortedT = customSort(Arrays.asList(newT.get("Items").split(", ")), window, window.size() - 1);
        insertTree(sortedT, root, Double.parseDouble(newT.get("tw")), tail);
        depthFirstSearch(root);

        TailElement l = tail.get(0);
        SWNNode N = l.node.parent;

        while (N != null && !N.name.equals("root")) {
            N.weight -= l.weight;

            if (N.weight <= 0) {
                SWNNode NPrime = N.parent;
                N.parent.childList.remove(N);
                N = NPrime;
            } else {
                N = N.parent;
            }
        }

        tail.remove(0);
        removeNodeFromTree(root, l);
        preCounter = originalPreCounter;
        postCounter = originalPostCounter;
        depthFirstSearch(root);

        return root;
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
            List<String> transaction = tail.get(i).transaction;
            SWNNode node = tail.get(i).node;
            double tw = tail.get(i).weight;
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

        List<Map<String, String>> window = df.subList(0, 0 + windowSize);
        int windowNumber = 1;

        System.out.println("Window " + windowNumber + ":");
        window.forEach(System.out::println);

        SWNNode swnTree = new SWNNode("root", 0, 0, 0, new ArrayList<>(), null);
        List<TailElement> tail = new ArrayList<>();

        swnTreeConstruction(window, swnTree, tail);

        printSWNTree(swnTree, 0);

        printTail(tail);

        for (int i = 0; i <= df.size() - windowSize; i++) {
            if(i + windowSize < df.size()) {
                preCounter = originalPreCounter;
                postCounter = originalPostCounter;

                window = df.subList(i, i + windowSize);
                windowNumber = i + 1;

                System.out.println("Window " + windowNumber + ":");
                window.forEach(System.out::println);

                Map<String, String> newTransaction = df.get(i + windowSize);
                System.out.println("New Transaction: " + newTransaction);

                swnTree = MaintainingSWNTree(window, swnTree, newTransaction, tail, originalPreCounter, originalPostCounter);
            
                printSWNTree(swnTree, 0);

                printTail(tail);
            }
        }
    }
}