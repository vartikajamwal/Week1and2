import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class PracticeProblem{
    record PurchaseResult(boolean success, String message) {}

    static class ProductState {
        private final AtomicInteger stock;
        private final Queue<Long> waitingList = new ConcurrentLinkedQueue<>();
        private final AtomicInteger waitlistCount = new AtomicInteger(0);

        ProductState(int initialStock) {
            this.stock = new AtomicInteger(initialStock);
        }

        PurchaseResult purchase(long userId) {
            while (true) {
                int current = stock.get();
                if (current <= 0) {
                    waitingList.offer(userId);
                    int position = waitlistCount.incrementAndGet();
                    return new PurchaseResult(false, "Added to waiting list, position #" + position);
                }
                if (stock.compareAndSet(current, current - 1)) {
                    return new PurchaseResult(true, "Success, " + (current - 1) + " units remaining");
                }
            }
        }

        int checkStock() {
            return stock.get();
        }
    }

    static class InventoryManager {
        private final Map<String, ProductState> inventory = new ConcurrentHashMap<>();

        void addProduct(String productId, int stock) {
            inventory.put(productId, new ProductState(stock));
        }

        int checkStock(String productId) {
            ProductState product = inventory.get(productId);
            return product == null ? -1 : product.checkStock();
        }

        PurchaseResult purchaseItem(String productId, long userId) {
            ProductState product = inventory.get(productId);
            if (product == null) {
                return new PurchaseResult(false, "Unknown product: " + productId);
            }
            return product.purchase(userId);
        }
    }

    public static void main(String[] args) {
        InventoryManager manager = new InventoryManager();
        manager.addProduct("IPHONE15_256GB", 2);

        System.out.println("checkStock(\"IPHONE15_256GB\") -> " + manager.checkStock("IPHONE15_256GB") + " units available");
        System.out.println("purchaseItem(...,12345) -> " + manager.purchaseItem("IPHONE15_256GB", 12345).message());
        System.out.println("purchaseItem(...,67890) -> " + manager.purchaseItem("IPHONE15_256GB", 67890).message());
        System.out.println("purchaseItem(...,99999) -> " + manager.purchaseItem("IPHONE15_256GB", 99999).message());
    }
}