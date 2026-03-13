import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PracticeProblem {
    record MatchResult(String documentId, int matchingNgrams, double similarityPercent) {}

    static class PlagiarismDetector {
        private final int n;
        private final Map<String, Set<String>> ngramToDocs = new HashMap<>();
        private final Map<String, Set<String>> docToNgrams = new HashMap<>();

        PlagiarismDetector(int n) {
            this.n = n;
        }

        public void indexDocument(String docId, String content) {
            Set<String> grams = extractNgrams(content);
            docToNgrams.put(docId, grams);
            for (String gram : grams) {
                ngramToDocs.computeIfAbsent(gram, g -> new HashSet<>()).add(docId);
            }
        }

        public List<MatchResult> analyzeDocument(String content) {
            Set<String> queryNgrams = extractNgrams(content);
            Map<String, Integer> matchCounts = new HashMap<>();

            for (String gram : queryNgrams) {
                for (String docId : ngramToDocs.getOrDefault(gram, Set.of())) {
                    matchCounts.merge(docId, 1, Integer::sum);
                }
            }

            List<MatchResult> results = new ArrayList<>();
            for (Map.Entry<String, Integer> e : matchCounts.entrySet()) {
                Set<String> targetNgrams = docToNgrams.getOrDefault(e.getKey(), Set.of());
                int unionSize = queryNgrams.size() + targetNgrams.size() - e.getValue();
                double similarity = unionSize == 0 ? 0.0 : (100.0 * e.getValue() / unionSize); // Jaccard
                results.add(new MatchResult(e.getKey(), e.getValue(), similarity));
            }
            results.sort((a, b) -> Double.compare(b.similarityPercent(), a.similarityPercent()));
            return results;
        }

        private Set<String> extractNgrams(String content) {
            String cleaned = content.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ").trim();
            if (cleaned.isEmpty()) return Set.of();

            String[] words = cleaned.split("\\s+");
            Set<String> grams = new HashSet<>();
            for (int i = 0; i + n <= words.length; i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < n; j++) {
                    if (j > 0) sb.append(' ');
                    sb.append(words[i + j]);
                }
                grams.add(sb.toString());
            }
            return grams;
        }
    }

    public static void main(String[] args) {
        PlagiarismDetector detector = new PlagiarismDetector(5);
        detector.indexDocument("essay_089", "data structures and algorithms are core in computer science education for all students");
        detector.indexDocument("essay_092", "hash tables and algorithms are core in computer science education for all learners");

        var result = detector.analyzeDocument("hash tables and algorithms are core in computer science education for all students today");
        for (MatchResult r : result) {
            String label = r.similarityPercent() >= 30.0 ? "PLAGIARISM DETECTED" : "suspicious";
            System.out.printf("%s -> %d matching n-grams, similarity=%.1f%% (%s)%n",
                    r.documentId(), r.matchingNgrams(), r.similarityPercent(), label);
        }
    }
}