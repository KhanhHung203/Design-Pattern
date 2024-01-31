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

class WLNode {
    int pre;
    int pos;
    double weight;

    public WLNode(int pre, int pos, double weight) {
        this.pre = pre;
        this.pos = pos;
        this.weight = weight;
    }

    @Override
    public String toString() {
        return String.format("WLNode(pre=%d, pos=%d, weight=%.2f)", pre, pos, weight);
    }
}

public class miningFWPsFromBenmarkDataset {
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
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static Map<String, Double> readItemWeights(String filename) {
        Map<String, Double> itemWeights = new HashMap<>();
        try (Scanner scanner = new Scanner(new FileReader(filename))) {
            scanner.nextLine().split(",");
            while (scanner.hasNextLine()) {
                String[] values = scanner.nextLine().split(",");
                itemWeights.put(values[0], Double.parseDouble(values[1]));
            }
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return itemWeights;
    }

    public static List<Map<String, String>> calculateTw(List<Map<String, String>> data, Map<String, Double> itemWeights) {
        return data.stream()
                .map(row -> {
                    String itemsHeader = "Items";
                    String itemsString = row.get(itemsHeader);
    
                    List<String> itemsInTransaction = Arrays.asList(itemsString.split(", "));
                    double tw = itemsInTransaction.stream()
                            .mapToDouble(item -> itemWeights.getOrDefault(item, 0.0))
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

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static List<List<String>> sortItemSet(List<List<String>> I_next, List<Map<List<String>, List<WLNode>>> new_WL) {
        if (I_next.size() > 1) {
            for (int i = 0; i < I_next.size() - 1; i++) {
                int finalI = i;
                for (int j = i + 1; j < I_next.size(); j++) {
                    int finalJ = j;

                    int pre_i = new_WL.stream()
                                      .filter(entry -> entry.containsKey(I_next.get(finalI)))
                                      .map(entry -> entry.get(I_next.get(finalI)).get(0).pre)
                                      .findFirst()
                                      .orElseThrow(NoSuchElementException::new);
                    int pre_j = new_WL.stream()
                                      .filter(entry -> entry.containsKey(I_next.get(finalJ)))
                                      .map(entry -> entry.get(I_next.get(finalJ)).get(0).pre)
                                      .findFirst()
                                      .orElseThrow(NoSuchElementException::new);

                    if (pre_i > pre_j) {
                        // Swap elements in I_next
                        List<String> temp = I_next.get(finalI);
                        I_next.set(finalI, I_next.get(finalJ));
                        I_next.set(finalJ, temp);
                    }
                }
            }
        }
        return I_next;
    }

    public static List<Map<List<String>,List<WLNode>>> wlIntersection(Map<List<String>,List<WLNode>> WL1, Map<List<String>,List<WLNode>> WL2, List<Map<String, String>> window, double windowTTW, double minWS) {
        List<Map<List<String>,List<WLNode>>> WL3 = new ArrayList<>();
        List<WLNode> WL3_support = new ArrayList<>();
        int k = -1, i = 0, j = 0;
        int m = 0, n = 0;
        List<WLNode> WNL1 = new ArrayList<>();
        List<WLNode> WNL2 = new ArrayList<>();
        List<String> i1 = new ArrayList<>();
        List<String> i2 = new ArrayList<>();

        for (Map.Entry<List<String>, List<WLNode>> entry : WL1.entrySet()) {
            i1 = entry.getKey();
            WNL1 = entry.getValue();
            m = WNL1.size();
        }

        for (Map.Entry<List<String>, List<WLNode>> entry : WL2.entrySet()) {
            i2 = entry.getKey();
            WNL2 = entry.getValue();
            n = WNL2.size();
        }

        double s = calculateWs(window, windowTTW, i1) + calculateWs(window, windowTTW, i2);

        while (i < m && j < n) {
            boolean check = false;
            if (WNL2.get(j).pre < WNL1.get(i).pre) {
                if (WNL2.get(j).pos > WNL1.get(i).pos) {
                    if (k >= 0) {
                        if (WNL2.get(j).pre == WL3_support.get(k).pre && WNL2.get(j).pos == WL3_support.get(k).pos) {
                            WL3_support.get(k).weight += WNL1.get(i).weight;
                            check = true;
                        }
                    }
                    if (check == false) {
                        k += 1;
                        WL3_support.add(new WLNode(WNL2.get(j).pre, WNL2.get(j).pos, WNL1.get(i).weight));
                    }
                    i += 1;
                } else {
                    s -= WNL2.get(j).weight;
                    j += 1;
                }
            } else {
                s -= WNL1.get(i).weight;
                i += 1;
            }
            if (s < minWS) {
                return new ArrayList<>();
            }
        }

        if (!WL3_support.isEmpty()) {
            Set<String> itemSet = new HashSet<>();
            itemSet.addAll(i1);
            itemSet.addAll(i2);
            List<String> uniqueItemSet = new ArrayList<>(itemSet);
            WL3.add(Collections.singletonMap(uniqueItemSet, WL3_support));
        }
        return WL3;
    }

    public static void findFWPs(List<List<String>> I_s, double min_ws, List<Map<List<String>,List<WLNode>>> WN_list, List<List<String>> FWPs, List<Map<String, String>> window, List<TailElement> tail) {
        double windowTTW = tail.stream().mapToDouble(node -> (int) node.weight).sum();
        List<Map<List<String>,List<WLNode>>> new_WL = new ArrayList<>();

        for (int i = I_s.size() - 1; i > 0; i--) {
            final int finalI = i;

            List<List<String>> I_next = new ArrayList<>();
            for (int j = i - 1; j >= 0; j--) {
                final int finalJ = j;
                List<Map<List<String>, List<WLNode>>> WL_result = wlIntersection(
                    WN_list.stream()
                           .filter(wl -> wl.containsKey(I_s.get(finalI)))
                           .flatMap(wl -> wl.entrySet().stream())
                           .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                    WN_list.stream()
                           .filter(wl -> wl.containsKey(I_s.get(finalJ)))
                           .flatMap(wl -> wl.entrySet().stream())
                           .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                    window,
                    windowTTW,
                    min_ws
                );
                new_WL.addAll(WL_result);

                for (Map<List<String>, List<WLNode>> item : WL_result) {
                    if (!WL_result.isEmpty() && calculateWs(window, windowTTW, new ArrayList<>(item.keySet()).get(0)) >= min_ws) {
                        FWPs.add(new ArrayList<>(item.keySet()).get(0));
                        I_next.add(new ArrayList<>(item.keySet()).get(0));
                        I_next = sortItemSet(I_next, new_WL);
                    }
                }
            }

            if (!I_next.isEmpty()) {
                findFWPs(I_next, min_ws, new_WL, FWPs, window, tail);
            }
        }
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static List<List<String>> sortI1ByWs(List<List<String>> I1, List<Map<List<String>, List<WLNode>>> WNList, double windowTTW) {
        for (int i = 0; i < I1.size() - 1; i++) {
            final int finalI = i;
            for (int j = i + 1; j < I1.size(); j++) {
                final int finalJ = j;
                double wsi = WNList.stream()
                                   .filter(row -> row.containsKey(Collections.singletonList(I1.get(finalI))))
                                   .flatMap(tuple -> tuple.get(Collections.singletonList(I1.get(finalI))).stream())
                                   .mapToDouble(node -> node.weight)
                                   .sum() / windowTTW;
                double wsj = WNList.stream()
                                   .filter(row -> row.containsKey(Collections.singletonList(I1.get(finalJ))))
                                   .flatMap(tuple -> tuple.get(Collections.singletonList(I1.get(finalJ))).stream())
                                   .mapToDouble(node -> node.weight)
                                   .sum() / windowTTW;

                if (wsj > wsi) {
                    // Swap elements in I1
                    List<String> temp = I1.get(finalI);
                    I1.set(finalI, I1.get(finalJ));
                    I1.set(finalJ, temp);
                }
            }
        }
        return I1;
    }

    public static void wnListCreate(SWNNode root, List<String> item, List<WLNode> wnLSupport) {
        if (root != null && !"root".equals(root.name)) {
            if (item.equals(Arrays.asList(root.name))) {
                wnLSupport.add(new WLNode(root.pre, root.pos, root.weight));
            }
        }
        for (SWNNode child : root.childList) {
            wnListCreate(child, item, wnLSupport);
        }
    }

    public static double calculateWs(List<Map<String, String>> window, double windowTTW, List<String> itemSet) {
        double ws = 0;
        for (Map<String, String> transaction : window) {
            int checkCount = 0;
            for (String item : itemSet) {
                if (transaction.get("Items").contains(item)) {
                    checkCount++;
                }
            }
            if (checkCount == itemSet.size()) {
                ws += Double.parseDouble(transaction.get("tw"));
            }
        }
        ws /= windowTTW;
        return ws;
    }

    public static void scanTreeForI1(SWNNode root, List<List<String>> fwps, List<Map<List<String>,List<WLNode>>> wnList, double windowTTW, double minWs, List<Map<String, String>> window) {
        boolean check = true;
        if (root != null && !"root".equals(root.name)) {
            for (List<String> x : fwps) {
                if (Arrays.asList(root.name).equals(x)) {
                    check = false;
                }
            }
            if (check && calculateWs(window, windowTTW, Arrays.asList(root.name)) >= minWs) {
                fwps.add(Arrays.asList(root.name));
            }
        }
        for (SWNNode child : root.childList) {
            scanTreeForI1(child, fwps, wnList, windowTTW, minWs, window);
        }
    }

    public static void generate1FWPs(SWNNode root, double minWs, List<TailElement> tail, List<Map<String, String>> window, List<List<String>> fwps, List<Map<List<String>,List<WLNode>>> wnList) {

        double windowTTW = tail.stream().mapToDouble(node -> (int) node.weight).sum();

        scanTreeForI1(root, fwps, wnList, windowTTW, minWs, window);

        for (List<String> item : fwps) {
            List<WLNode> wnLSupport = new ArrayList<>();
            wnListCreate(root, item, wnLSupport);
            wnList.add(Collections.singletonMap(item, wnLSupport));
        }

        fwps = sortI1ByWs(fwps, wnList, windowTTW);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static List<List<String>> FWPODS(SWNNode root, Map<String, String> newT, double minWs, List<TailElement> tail, List<Map<String, String>> window, Map<String, Double> itemWeights, int originalPreCounter, int originalPostCounter) {

        // Assuming maintaining_swn_tree and other related functions are defined appropriately
        root = MaintainingSWNTree(window, root, newT, tail, originalPreCounter, originalPostCounter);

        printTail(tail);

        List<List<String>> fwps = new ArrayList<>();
        List<Map<List<String>,List<WLNode>>> wnList = new ArrayList<>();

        generate1FWPs(root, minWs, tail, window, fwps, wnList);

        List<List<String>> I1 = new ArrayList<>(fwps);

        findFWPs(I1, minWs, wnList, fwps, window, tail);

        return fwps;
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
        int windowSize = 990;
        double minWS = 0.3;

        List<Map<String, String>> df = readTransactionData("Custom_data.csv");
        Map<String, Double> itemWeights = readItemWeights("Custom_weights.csv");
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

        printTail(tail);

        for (int i = 1; i <= df.size() - windowSize; i++) {
            Map<String, String> newTransaction = new HashMap<>();
            preCounter = originalPreCounter;
            postCounter = originalPostCounter;

            newTransaction = df.get(i + windowSize);
            System.out.println("New Transaction: " + newTransaction);

            window = df.subList(i, i + windowSize);
            windowNumber = i + 1;

            System.out.println("Window " + windowNumber + ":");
            window.forEach(System.out::println);

            List<List<String>> fwps = FWPODS(swnTree, newTransaction, minWS, tail, window, itemWeights, originalPreCounter, originalPostCounter);
        }
    }
}
