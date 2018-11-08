package food.togo.payment.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

@Entity
@Setter
@Getter
public class CustomerStripe {

    private Integer customerId;
    private String stripeId;
    private byte[] salt;
    private byte[] iv;
}
