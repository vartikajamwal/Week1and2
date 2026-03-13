import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PracticeProblem{
    static class UsernameService {
        private final ConcurrentHashMap<String, String> usernameToUserId = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, AtomicInteger> attemptFrequency = new ConcurrentHashMap<>();

        public boolean registerUsername(String username, String userId) {
            return usernameToUserId.putIfAbsent(normalize(username), userId) == null;
        }

        public boolean checkAvailability(String username) {
            String normalized = normalize(username);
            attemptFrequency.computeIfAbsent(normalized, k -> new AtomicInteger()).incrementAndGet();
            return !usernameToUserId.containsKey(normalized);
        }

        public List<String> suggestAlternatives(String username, int limit) {
            String normalized = normalize(username);
            List<String> suggestions = new ArrayList<>(limit);

            for (int i = 1; i <= 1000 && suggestions.size() < limit; i++) {
                addIfAvailable(suggestions, normalized + i, limit);
            }
            addIfAvailable(suggestions, normalized.replace('_', '.'), limit);
            addIfAvailable(suggestions, normalized + "_official", limit);
            addIfAvailable(suggestions, normalized + "_real", limit);

            return suggestions;
        }

        public String getMostAttempted() {
            return attemptFrequency.entrySet().stream()
                    .max(Comparator.comparingInt(e -> e.getValue().get()))
                    .map(e -> e.getKey() + " (" + e.getValue().get() + " attempts)")
                    .orElse("No attempts yet");
        }

        public Map<String, String> getRegisteredUsersView() {
            return Map.copyOf(usernameToUserId);
        }

        private void addIfAvailable(List<String> out, String candidate, int limit) {
            if (out.size() < limit && !usernameToUserId.containsKey(candidate) && !out.contains(candidate)) {
                out.add(candidate);
            }
        }

        private String normalize(String username) {
            return username.trim().toLowerCase();
        }
    }

    public static void main(String[] args) {
        UsernameService service = new UsernameService();
        service.registerUsername("vartikajamwal", "u1001");
        service.registerUsername("admin", "u0001");

        System.out.println("checkAvailability(\"vartikajamwal\") -> " + service.checkAvailability("vartikajamwal"));
        System.out.println("checkAvailability(\"sohambiswas\") -> " + service.checkAvailability("sohambiswas"));
        System.out.println("suggestAlternatives(\"vartikajamwal\") -> " + service.suggestAlternatives("vartikajamwal", 3));

        for (int i = 0; i < 10; i++) service.checkAvailability("admin");
        System.out.println("getMostAttempted() -> " + service.getMostAttempted());
    }
}