package food.togo.payment.dto;


import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class CustomerEntity {

    private Long customerID;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String password;
    private String pin;
    private String address1;
    private String address2;
    private String city;
    private String state;
    private Integer zip;
    private Integer status;
    private String stripeCustomerID;


}
