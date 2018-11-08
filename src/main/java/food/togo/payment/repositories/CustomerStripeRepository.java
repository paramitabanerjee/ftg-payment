package food.togo.payment.repositories;

import food.togo.payment.entities.CustomerStripe;
import food.togo.payment.entities.PaymentHistory;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface CustomerStripeRepository extends CrudRepository<CustomerStripe, Long> {

    public Optional<CustomerStripe> findByCustomerId(Integer customerId);
}
