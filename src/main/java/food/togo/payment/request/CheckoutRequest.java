package food.togo.payment.request;

import lombok.Data;

@Data
public class CheckoutRequest {

    private float amount;
    private Long customerId;
}
