import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class PracticeProblem {
    interface UpstreamDns {
        String resolve(String domain);
    }

    record DNSEntry(String ipAddress, Instant expiresAt) {
        boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    static class DNSCache {
        private final LinkedHashMap<String, DNSEntry> cache;
        private final UpstreamDns upstreamDns;
        private final LongAdder hits = new LongAdder();
        private final LongAdder misses = new LongAdder();
        private final LongAdder totalLookupNanos = new LongAdder();
        private final ScheduledExecutorService cleaner;

        DNSCache(int maxSize, UpstreamDns upstreamDns) {
            this.upstreamDns = upstreamDns;
            this.cache = new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, DNSEntry> eldest) {
                    return size() > maxSize;
                }
            };

            this.cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "dns-cleaner");
                t.setDaemon(true);
                return t;
            });
            cleaner.scheduleAtFixedRate(this::cleanupExpired, 2, 2, TimeUnit.SECONDS);
        }

        public synchronized String resolve(String domain, int ttlSeconds) {
            long start = System.nanoTime();
            DNSEntry entry = cache.get(domain);
            if (entry != null && !entry.expired()) {
                hits.increment();
                totalLookupNanos.add(System.nanoTime() - start);
                return "Cache HIT -> " + entry.ipAddress();
            }

            misses.increment();
            String ip = upstreamDns.resolve(domain);
            cache.put(domain, new DNSEntry(ip, Instant.now().plusSeconds(ttlSeconds)));
            totalLookupNanos.add(System.nanoTime() - start);
            return "Cache MISS -> " + ip;
        }

        public synchronized String getCacheStats() {
            long h = hits.longValue();
            long m = misses.longValue();
            long total = h + m;
            double hitRate = total == 0 ? 0.0 : (h * 100.0 / total);
            double avgMs = total == 0 ? 0.0 : (totalLookupNanos.doubleValue() / total) / 1_000_000.0;
            return String.format("Hit Rate: %.1f%%, Avg Lookup Time: %.3fms", hitRate, avgMs);
        }

        private synchronized void cleanupExpired() {
            cache.entrySet().removeIf(e -> e.getValue().expired());
        }

        public void close() {
            cleaner.shutdownNow();
        }
    }

    public static void main(String[] args) throws Exception {
        UpstreamDns fakeUpstream = domain -> "172.217.14." + (Math.abs(domain.hashCode()) % 200 + 1);
        DNSCache cache = new DNSCache(3, fakeUpstream);

        System.out.println("resolve(google.com) -> " + cache.resolve("google.com", 2));
        System.out.println("resolve(google.com) -> " + cache.resolve("google.com", 2));
        Thread.sleep(2100);
        System.out.println("resolve(google.com) -> " + cache.resolve("google.com", 2));
        System.out.println("getCacheStats() -> " + cache.getCacheStats());

        cache.close();
    }
}