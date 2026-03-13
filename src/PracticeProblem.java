import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PracticeProblem {
    record VideoData(String videoId, String payload) {}

    static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;

        LRUCache(int capacity) {
            super(16, 0.75f, true);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }

    static class MultiLevelCache {
        private final LRUCache<String, VideoData> l1; // in-memory
        private final LRUCache<String, VideoData> l2; // simulated SSD cache
        private final Map<String, VideoData> l3Database; // source of truth
        private final Map<String, Integer> accessCount = new HashMap<>();
        private final int promoteThreshold;

        private long requests;
        private long l1Hits;
        private long l2Hits;
        private long l3Hits;

        MultiLevelCache(int l1Size, int l2Size, int promoteThreshold, Map<String, VideoData> db) {
            this.l1 = new LRUCache<>(l1Size);
            this.l2 = new LRUCache<>(l2Size);
            this.promoteThreshold = promoteThreshold;
            this.l3Database = db;
        }

        public synchronized VideoData getVideo(String videoId) {
            requests++;

            VideoData data = l1.get(videoId);
            if (data != null) {
                l1Hits++;
                return data;
            }

            data = l2.get(videoId);
            if (data != null) {
                l2Hits++;
                int count = accessCount.merge(videoId, 1, Integer::sum);
                if (count >= promoteThreshold) l1.put(videoId, data);
                return data;
            }

            data = l3Database.get(videoId);
            if (data != null) {
                l3Hits++;
                l2.put(videoId, data);
                accessCount.merge(videoId, 1, Integer::sum);
            }
            return data;
        }

        public synchronized void invalidate(String videoId) {
            l1.remove(videoId);
            l2.remove(videoId);
            accessCount.remove(videoId);
        }

        public synchronized void updateContent(VideoData newData) {
            l3Database.put(newData.videoId(), newData);
            invalidate(newData.videoId());
        }

        public synchronized String getStatistics() {
            double l1Rate = requests == 0 ? 0 : l1Hits * 100.0 / requests;
            double l2Rate = requests == 0 ? 0 : l2Hits * 100.0 / requests;
            double l3Rate = requests == 0 ? 0 : l3Hits * 100.0 / requests;
            double overall = requests == 0 ? 0 : (l1Hits + l2Hits + l3Hits) * 100.0 / requests;
            return String.format("L1: Hit Rate %.1f%%, L2: Hit Rate %.1f%%, L3: Hit Rate %.1f%%, Overall: %.1f%%",
                    l1Rate, l2Rate, l3Rate, overall);
        }
    }

    public static void main(String[] args) {
        Map<String, VideoData> db = new HashMap<>();
        db.put("video_123", new VideoData("video_123", "movie-data"));
        db.put("video_999", new VideoData("video_999", "documentary-data"));

        MultiLevelCache cache = new MultiLevelCache(2, 3, 2, db);
        System.out.println("getVideo(video_123) -> " + cache.getVideo("video_123")); // l3
        System.out.println("getVideo(video_123) -> " + cache.getVideo("video_123")); // l2
        System.out.println("getVideo(video_123) -> " + cache.getVideo("video_123")); // l2->l1 promotion
        System.out.println("getVideo(video_123) -> " + cache.getVideo("video_123")); // l1
        System.out.println("getVideo(video_999) -> " + cache.getVideo("video_999"));
        System.out.println("getStatistics() -> " + cache.getStatistics());
    }
}