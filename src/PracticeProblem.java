import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class PracticeProblem {
    enum SpotStatus { EMPTY, OCCUPIED, DELETED }

    static class Spot {
        SpotStatus status = SpotStatus.EMPTY;
        String plate;
        LocalDateTime entryTime;
    }

    static class ParkingLot {
        private final Spot[] spots;
        private final int entranceIndex;
        private int occupiedCount;
        private int totalProbes;
        private int parkOps;
        private final Map<Integer, Integer> entriesByHour = new HashMap<>();

        ParkingLot(int capacity, int entranceIndex) {
            this.spots = new Spot[capacity];
            this.entranceIndex = entranceIndex;
            for (int i = 0; i < capacity; i++) spots[i] = new Spot();
        }

        public synchronized String parkVehicle(String plate) {
            int preferred = hash(plate);
            int probes = 0;
            for (int i = 0; i < spots.length; i++) {
                int idx = (preferred + i) % spots.length;
                if (spots[idx].status == SpotStatus.EMPTY || spots[idx].status == SpotStatus.DELETED) {
                    assign(idx, plate, probes);
                    return "Assigned spot #" + idx + " (" + probes + " probes)";
                }
                probes++;
            }
            return "Parking lot full";
        }

        public synchronized int findNearestAvailableSpotToEntrance() {
            int best = -1;
            int bestDistance = Integer.MAX_VALUE;
            for (int i = 0; i < spots.length; i++) {
                if (spots[i].status == SpotStatus.EMPTY || spots[i].status == SpotStatus.DELETED) {
                    int distance = circularDistance(entranceIndex, i, spots.length);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = i;
                    }
                }
            }
            return best;
        }

        public synchronized String exitVehicle(String plate, double hourlyRate) {
            int idx = findVehicle(plate);
            if (idx == -1) return "Vehicle not found";

            Spot spot = spots[idx];
            Duration d = Duration.between(spot.entryTime, LocalDateTime.now());
            double hours = Math.max(1.0, d.toMinutes() / 60.0);
            double fee = hours * hourlyRate;

            spot.status = SpotStatus.DELETED;
            spot.plate = null;
            spot.entryTime = null;
            occupiedCount--;

            return String.format("Spot #%d freed, Duration: %dh %dm, Fee: $%.2f",
                    idx, d.toHours(), d.toMinutesPart(), fee);
        }

        public synchronized String getStatistics() {
            double occupancy = 100.0 * occupiedCount / spots.length;
            double avgProbes = parkOps == 0 ? 0 : (double) totalProbes / parkOps;
            int peakHour = entriesByHour.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(-1);
            return String.format("Occupancy: %.1f%%, Avg Probes: %.2f, Peak Hour: %s",
                    occupancy, avgProbes, peakHour == -1 ? "N/A" : peakHour + "-" + (peakHour + 1));
        }

        private void assign(int idx, String plate, int probes) {
            Spot spot = spots[idx];
            spot.status = SpotStatus.OCCUPIED;
            spot.plate = plate;
            spot.entryTime = LocalDateTime.now();
            occupiedCount++;
            parkOps++;
            totalProbes += probes;
            entriesByHour.merge(LocalDateTime.now().getHour(), 1, Integer::sum);
        }

        private int findVehicle(String plate) {
            int preferred = hash(plate);
            for (int i = 0; i < spots.length; i++) {
                int idx = (preferred + i) % spots.length;
                Spot s = spots[idx];
                if (s.status == SpotStatus.EMPTY) return -1;
                if (s.status == SpotStatus.OCCUPIED && plate.equals(s.plate)) return idx;
            }
            return -1;
        }

        private int hash(String plate) {
            return Math.abs(plate.hashCode()) % spots.length;
        }

        private int circularDistance(int a, int b, int n) {
            int direct = Math.abs(a - b);
            return Math.min(direct, n - direct);
        }
    }

    public static void main(String[] args) {
        ParkingLot lot = new ParkingLot(10, 0);
        System.out.println(lot.parkVehicle("ABC-1234"));
        System.out.println(lot.parkVehicle("ABC-1235"));
        System.out.println(lot.parkVehicle("XYZ-9999"));
        System.out.println("Nearest spot to entrance -> #" + lot.findNearestAvailableSpotToEntrance());
        System.out.println(lot.exitVehicle("ABC-1234", 5.5));
        System.out.println(lot.getStatistics());
    }
}