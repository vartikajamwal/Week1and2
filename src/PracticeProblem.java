import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class PracticeProblem {
    static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isTerminal;
    }

    static class Autocomplete {
        private final TrieNode root = new TrieNode();
        private final Map<String, Integer> queryFrequency = new HashMap<>();

        void addOrUpdateQuery(String query, int delta) {
            queryFrequency.merge(query, delta, Integer::sum);
            TrieNode node = root;
            for (char c : query.toCharArray()) {
                node = node.children.computeIfAbsent(c, k -> new TrieNode());
            }
            node.isTerminal = true;
        }

        List<String> search(String prefix, int topK) {
            TrieNode node = root;
            for (char c : prefix.toCharArray()) {
                node = node.children.get(c);
                if (node == null) return List.of();
            }

            PriorityQueue<String> minHeap = new PriorityQueue<>(Comparator.comparingInt(queryFrequency::get));
            collect(prefix, node, minHeap, topK);

            List<String> result = new ArrayList<>(minHeap);
            result.sort((a, b) -> Integer.compare(queryFrequency.get(b), queryFrequency.get(a)));
            return result;
        }

        List<String> suggestCorrections(String input, int maxDistance, int topK) {
            return queryFrequency.keySet().stream()
                    .filter(q -> levenshtein(input, q) <= maxDistance)
                    .sorted((a, b) -> Integer.compare(queryFrequency.get(b), queryFrequency.get(a)))
                    .limit(topK)
                    .toList();
        }

        private void collect(String prefix, TrieNode node, PriorityQueue<String> heap, int topK) {
            if (node.isTerminal) {
                heap.offer(prefix);
                if (heap.size() > topK) heap.poll();
            }
            for (Map.Entry<Character, TrieNode> e : node.children.entrySet()) {
                collect(prefix + e.getKey(), e.getValue(), heap, topK);
            }
        }

        private int levenshtein(String a, String b) {
            int[][] dp = new int[a.length() + 1][b.length() + 1];
            for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
            for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
            for (int i = 1; i <= a.length(); i++) {
                for (int j = 1; j <= b.length(); j++) {
                    int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
                }
            }
            return dp[a.length()][b.length()];
        }
    }

    public static void main(String[] args) {
        Autocomplete ac = new Autocomplete();
        ac.addOrUpdateQuery("java tutorial", 1_234_567);
        ac.addOrUpdateQuery("javascript", 987_654);
        ac.addOrUpdateQuery("java download", 456_789);
        ac.addOrUpdateQuery("java 21 features", 1);
        ac.addOrUpdateQuery("java collections", 240_000);

        System.out.println("search(\"jav\") -> " + ac.search("jav", 10));
        ac.addOrUpdateQuery("java 21 features", 2);
        System.out.println("updateFrequency(java 21 features) -> " + ac.search("java 21", 10));
        System.out.println("suggestCorrections(\"jvaa\") -> " + ac.suggestCorrections("jvaa", 4, 3));
    }
}