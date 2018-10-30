package food.togo.payment.service;

import com.stripe.Stripe;
import com.stripe.exception.*;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import food.togo.payment.request.ChargeRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service("payment")
public class PaymentService {

    //@Value("${STRIPE_PUBLIC_KEY}")
    private String stripePublicKey = "pk_test_JMlbR4PSbP5qPVON78QsDW8b";

    public PaymentService() {
        Stripe.apiKey = stripePublicKey;
    }


    public Charge chargeCustomer(ChargeRequest chargeRequest)
            throws AuthenticationException, InvalidRequestException,
            ApiConnectionException, CardException, ApiException, Exception {


        if(chargeRequest.getCustomerId() == null) {
            throw new Exception("Customer Id cannot be null while making stripe call");//TODO
        }

        String stripeCustomerId = getStripeCustomerId(chargeRequest);

        Map<String, Object> chargeParams = new HashMap<>();
        chargeParams.put("amount", chargeRequest.getAmount());
        chargeParams.put("currency", chargeRequest.getCurrency());
        chargeParams.put("description", chargeRequest.getDescription());

        if(stripeCustomerId != null) {
            chargeParams.put("customer", stripeCustomerId);
        } else if (chargeRequest.getStripeToken() != null) {
            chargeParams.put("source", chargeRequest.getStripeToken());
        }

        try {
            // Submit charge to credit card
            Charge charge = Charge.create(chargeParams);
            System.out.println(charge);
            return charge;
        } catch (CardException e) {
            // Transaction was declined
            System.out.println("Status is: " + e.getCode());
            System.out.println("Message is: " + e.getMessage());
        } catch (RateLimitException e) {
            // Too many requests made to the API too quickly
        } catch (InvalidRequestException e) {
            // Invalid parameters were supplied to Stripe's API
        } catch (AuthenticationException e) {
            // Authentication with Stripe's API failed (wrong API key?)
        } catch (ApiConnectionException e) {
            // Network communication with Stripe failed
        } catch (StripeException e) {
            // Generic error
        } catch (Exception e) {
            // Something else happened unrelated to Stripe
        }
        return null;
    }

    //calls customer endppoints here to get customer details

    private String getStripeCustomerId(ChargeRequest chargeRequest) {


        Integer customerId = chargeRequest.getCustomerId();

        //make a rest call
        CustomerEntity customerEntity = customerService.getCustomer(customerId);
        String stripeCustomerId = customerEntity.getStripeCustomerID();

        Map<String, Object> chargeParamsForSaveCustomer = new HashMap<>();
        chargeParamsForSaveCustomer.put("source", chargeRequest.getStripeToken());
        chargeParamsForSaveCustomer.put("email", chargeRequest.getStripeEmail());

        Customer customer = null;
        try {
            customer = Customer.create(chargeParamsForSaveCustomer);
        } catch (CardException e) {
            // Transaction was declined
            System.out.println("Status is: " + e.getCode());
            System.out.println("Message is: " + e.getMessage());
        } catch (RateLimitException e) {
            // Too many requests made to the API too quickly
        } catch (InvalidRequestException e) {
            // Invalid parameters were supplied to Stripe's API
        } catch (AuthenticationException e) {
            // Authentication with Stripe's API failed (wrong API key?)
        } catch (ApiConnectionException e) {
            // Network communication with Stripe failed
        } catch (StripeException e) {
            // Generic error
        } catch (Exception e) {
            // Something else happened unrelated to Stripe
        }
        System.out.println(customer.getId());
        stripeCustomerId = customer.getId(); //use this stripe customerId for subsequent payments

        //save stripe customerId in Customer table

        return stripeCustomerId;
    }

}
