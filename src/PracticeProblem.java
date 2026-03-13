import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PracticeProblem{
    record RateLimitDecision(boolean allowed, int remaining, long retryAfterSeconds, String message) {}

    static class TokenBucket {
        private final int maxTokens;
        private final double refillRatePerSecond;
        private double tokens;
        private long lastRefillMillis;

        TokenBucket(int maxTokens, double refillRatePerSecond) {
            this.maxTokens = maxTokens;
            this.refillRatePerSecond = refillRatePerSecond;
            this.tokens = maxTokens;
            this.lastRefillMillis = System.currentTimeMillis();
        }

        synchronized RateLimitDecision consume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return new RateLimitDecision(true, (int) Math.floor(tokens), 0, "Allowed");
            }
            long retryAfter = (long) Math.ceil((1.0 - tokens) / refillRatePerSecond);
            return new RateLimitDecision(false, 0, retryAfter,
                    "Rate limit exceeded. Retry after " + retryAfter + " seconds.");
        }

        private void refill() {
            long now = System.currentTimeMillis();
            double elapsedSeconds = (now - lastRefillMillis) / 1000.0;
            tokens = Math.min(maxTokens, tokens + elapsedSeconds * refillRatePerSecond);
            lastRefillMillis = now;
        }
    }

    static class DistributedRateLimiter {
        // Simulates shared distributed state (e.g., Redis) by allowing multiple limiter instances
        // to use the same backing map.
        private final Map<String, TokenBucket> sharedBuckets;
        private final int maxTokens;
        private final int windowSeconds;

        DistributedRateLimiter(Map<String, TokenBucket> sharedBuckets, int maxTokens, int windowSeconds) {
            this.sharedBuckets = sharedBuckets;
            this.maxTokens = maxTokens;
            this.windowSeconds = windowSeconds;
        }

        RateLimitDecision checkRateLimit(String clientId) {
            double refillRate = (double) maxTokens / windowSeconds;
            TokenBucket bucket = sharedBuckets.computeIfAbsent(clientId, id -> new TokenBucket(maxTokens, refillRate));
            return bucket.consume();
        }
    }

    public static void main(String[] args) {
        Map<String, TokenBucket> shared = new ConcurrentHashMap<>();
        DistributedRateLimiter nodeA = new DistributedRateLimiter(shared, 5, 10);
        DistributedRateLimiter nodeB = new DistributedRateLimiter(shared, 5, 10);

        System.out.println(nodeA.checkRateLimit("abc123"));
        System.out.println(nodeB.checkRateLimit("abc123"));
        System.out.println(nodeA.checkRateLimit("abc123"));
        System.out.println(nodeB.checkRateLimit("abc123"));
        System.out.println(nodeA.checkRateLimit("abc123"));
        System.out.println(nodeB.checkRateLimit("abc123"));
    }
}