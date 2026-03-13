import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class PracticeProblem{
    record Event(String url, String userId, String source) {}
    record PageStat(String pageUrl, long views, int uniqueUsers) {}
    record Dashboard(List<PageStat> topPages, Map<String, Long> sourceDistribution) {}

    static class AnalyticsEngine {
        private final ConcurrentHashMap<String, LongAdder> pageViews = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Set<String>> uniqueVisitors = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, LongAdder> sourceCounts = new ConcurrentHashMap<>();

        void processEvent(Event event) {
            pageViews.computeIfAbsent(event.url(), u -> new LongAdder()).increment();
            uniqueVisitors.computeIfAbsent(event.url(), u -> ConcurrentHashMap.newKeySet()).add(event.userId());
            sourceCounts.computeIfAbsent(event.source(), s -> new LongAdder()).increment();
        }

        Dashboard getDashboard() {
            List<PageStat> topPages = pageViews.entrySet().stream()
                    .map(e -> new PageStat(
                            e.getKey(),
                            e.getValue().longValue(),
                            uniqueVisitors.getOrDefault(e.getKey(), Set.of()).size()))
                    .sorted(Comparator.comparingLong(PageStat::views).reversed())
                    .limit(10)
                    .toList();

            Map<String, Long> sourceDistribution = sourceCounts.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> e.getValue().longValue()));

            return new Dashboard(topPages, sourceDistribution);
        }
    }

    public static void main(String[] args) {
        AnalyticsEngine engine = new AnalyticsEngine();
        engine.processEvent(new Event("/article/breaking-news", "user_123", "google"));
        engine.processEvent(new Event("/article/breaking-news", "user_456", "facebook"));
        engine.processEvent(new Event("/sports/championship", "user_111", "direct"));
        engine.processEvent(new Event("/sports/championship", "user_222", "google"));
        engine.processEvent(new Event("/sports/championship", "user_111", "google"));

        Dashboard dash = engine.getDashboard();
        System.out.println("Top Pages: " + dash.topPages());
        System.out.println("Traffic Sources: " + dash.sourceDistribution());
    }
}