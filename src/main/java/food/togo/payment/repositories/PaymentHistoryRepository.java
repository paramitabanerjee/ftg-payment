package food.togo.payment.repositories;

import food.togo.payment.entities.PaymentHistory;
import org.springframework.data.repository.CrudRepository;

public interface PaymentHistoryRepository extends CrudRepository<PaymentHistory, Long> {
}
