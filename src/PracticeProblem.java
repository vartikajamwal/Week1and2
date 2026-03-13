import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PracticeProblem {
    record Transaction(int id, int amount, String merchant, String account, LocalDateTime time) {}

    static class Analyzer {
        List<List<Transaction>> findTwoSum(List<Transaction> txs, int target) {
            Map<Integer, List<Transaction>> byAmount = new HashMap<>();
            List<List<Transaction>> pairs = new ArrayList<>();

            for (Transaction tx : txs) {
                int needed = target - tx.amount();
                for (Transaction seen : byAmount.getOrDefault(needed, List.of())) {
                    pairs.add(List.of(seen, tx));
                }
                byAmount.computeIfAbsent(tx.amount(), k -> new ArrayList<>()).add(tx);
            }
            return pairs;
        }

        List<List<Transaction>> findTwoSumWithinWindow(List<Transaction> txs, int target, Duration window) {
            List<List<Transaction>> pairs = findTwoSum(txs, target);
            return pairs.stream()
                    .filter(p -> Duration.between(p.get(0).time(), p.get(1).time()).abs().compareTo(window) <= 0)
                    .toList();
        }

        List<List<Transaction>> findKSum(List<Transaction> txs, int k, int target) {
            List<List<Transaction>> results = new ArrayList<>();
            backtrack(txs, 0, k, target, new ArrayList<>(), results);
            return results;
        }

        List<String> detectDuplicates(List<Transaction> txs) {
            Map<String, Set<String>> grouped = new HashMap<>();
            for (Transaction tx : txs) {
                String key = tx.amount() + "|" + tx.merchant();
                grouped.computeIfAbsent(key, k -> new HashSet<>()).add(tx.account());
            }

            List<String> duplicates = new ArrayList<>();
            for (var e : grouped.entrySet()) {
                if (e.getValue().size() > 1) {
                    duplicates.add("{amount+merchant=" + e.getKey() + ", accounts=" + e.getValue() + "}");
                }
            }
            return duplicates;
        }

        private void backtrack(List<Transaction> txs, int idx, int k, int target,
                               List<Transaction> curr, List<List<Transaction>> out) {
            if (k == 0) {
                if (target == 0) out.add(new ArrayList<>(curr));
                return;
            }
            if (idx == txs.size()) return;

            for (int i = idx; i < txs.size(); i++) {
                curr.add(txs.get(i));
                backtrack(txs, i + 1, k - 1, target - txs.get(i).amount(), curr, out);
                curr.remove(curr.size() - 1);
            }
        }
    }

    public static void main(String[] args) {
        List<Transaction> txs = List.of(
                new Transaction(1, 500, "Store A", "acc1", LocalDateTime.of(2025, 1, 1, 10, 0)),
                new Transaction(2, 300, "Store B", "acc2", LocalDateTime.of(2025, 1, 1, 10, 15)),
                new Transaction(3, 200, "Store C", "acc3", LocalDateTime.of(2025, 1, 1, 10, 30)),
                new Transaction(4, 500, "Store A", "acc9", LocalDateTime.of(2025, 1, 1, 10, 45))
        );

        Analyzer analyzer = new Analyzer();
        System.out.println("findTwoSum(target=500) -> " + analyzer.findTwoSum(txs, 500));
        System.out.println("findTwoSumWithinWindow(1h) -> " + analyzer.findTwoSumWithinWindow(txs, 500, Duration.ofHours(1)));
        System.out.println("findKSum(k=3,target=1000) -> " + analyzer.findKSum(txs, 3, 1000));
        System.out.println("detectDuplicates() -> " + analyzer.detectDuplicates(txs));
    }
}