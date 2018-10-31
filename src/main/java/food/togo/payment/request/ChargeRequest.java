package food.togo.payment.request;

import lombok.Data;

@Data
public class ChargeRequest {

    public enum Currency {
        EUR, USD, INR;
    }
    private String description;
    private int amountCents; // cents
    private Currency currency;
    private String stripeEmail;
    private String stripeToken;
    private Integer customerId;
}
