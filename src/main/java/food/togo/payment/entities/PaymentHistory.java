package food.togo.payment.entities;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;
import java.sql.Date;

@Entity
@Setter
@Getter
public class PaymentHistory implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentID;
    private String stripeChargeId;
    private Long orderID;
    private Integer customerID;
    private Long amount;
    private Long refund;
    private String currency;
    private String status;
    private String error;
    private Date created;

}
