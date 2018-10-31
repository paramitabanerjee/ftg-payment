package food.togo.payment.service;

import com.stripe.Stripe;
import com.stripe.exception.*;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import food.togo.payment.config.RestTemplateConfig;
import food.togo.payment.dto.CustomerEntity;
import food.togo.payment.request.ChargeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service("payment")
public class PaymentService {

    //@Value("${STRIPE_PUBLIC_KEY}")
    private String stripePublicKey = "pk_test_JMlbR4PSbP5qPVON78QsDW8b";

    Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private RestTemplate restTemplate;

    public PaymentService() {
        Stripe.apiKey = stripePublicKey;
    }


    public Charge chargeCustomer(ChargeRequest chargeRequest)
            throws AuthenticationException, InvalidRequestException,
            ApiConnectionException, CardException, ApiException, Exception {

        logger.debug("Charging customer {} : Amount(Cents) {} ", chargeRequest.getCustomerId(), chargeRequest.getAmount());
        if(chargeRequest.getCustomerId() == null) {
            logger.error("Customer Id cannot be null while making a stripe call");
            throw new Exception("Customer Id cannot be null while making stripe call");//TODO
        }

        String stripeCustomerId = getStripeCustomerId(chargeRequest);
        logger.debug("Stripe customer Id {}", stripeCustomerId); //TODO: remove

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
            logger.debug("Charge info after successful Stripe call {} ", charge);
            return charge;
        } catch (CardException e) {
            // Transaction was declined
            logger.error("Status is: {}" , e.getCode());
            logger.error("Message is: {}" , e.getMessage());
        } catch (RateLimitException e) {
            // Too many requests made to the API too quickly
            logger.error("RateLimit error while calling stripe");
        } catch (InvalidRequestException e) {
            // Invalid parameters were supplied to Stripe's API
            logger.error("Invalid parameters were supplied to Stripe's API");
        } catch (AuthenticationException e) {
            // Authentication with Stripe's API failed (wrong API key?)
            logger.error("Authentication with Stripe's API failed (wrong API key?)");
        } catch (ApiConnectionException e) {
            // Network communication with Stripe failed
            logger.error("Network communication with Stripe failed");
        } catch (StripeException e) {
            // Generic error
            logger.error("Stripe exception {} ",e.getMessage());
        } catch (Exception e) {
            // Something else happened unrelated to Stripe
            logger.error("Something else happened unrelated to Stripe {} ",e.getMessage());
        }
        return null;
    }

    //calls customer endppoints here to get customer details
    @Value("${get.customer.endpoint}")
    private String customerEndpointGET;
    @Value("${update.customer.endpoint}")
    private String customerEndpointUPDATE;

    private String getStripeCustomerId(ChargeRequest chargeRequest) {

        Integer customerId = chargeRequest.getCustomerId();
        String stripeCustomerId = null;

        //make a rest call
        Map<String, Object> map = new HashMap<>();
        map.put("customerId", customerId);

        CustomerEntity customerEntity = null;

        try {
            ResponseEntity<CustomerEntity> responseEntity
                    = restTemplate.getForObject(customerEndpointGET, ResponseEntity.class, map);
            customerEntity = responseEntity.getBody();
            stripeCustomerId = customerEntity.getStripeCustomerID();
            if(stripeCustomerId != null) {
                return stripeCustomerId;
            }
        } catch (Exception e) {
           logger.error("Could not fetch customer {} details.. check customer endpoint", customerId);
           //return null;
        }


        try {
            if(stripeCustomerId == null ) { //not pre-saved
                Map<String, Object> chargeParamsForSaveCustomer = new HashMap<>();
                chargeParamsForSaveCustomer.put("source", chargeRequest.getStripeToken());
                chargeParamsForSaveCustomer.put("email", chargeRequest.getStripeEmail());
                Customer customer = Customer.create(chargeParamsForSaveCustomer);
                logger.debug("Saved customer info in Stripe {} ", customer.getId());
                if(customer != null) {
                    stripeCustomerId = customer.getId(); //use this stripe customerId for subsequent payments
                }
            }
        } catch (RateLimitException e) {
            // Too many requests made to the API too quickly
            logger.error("RateLimit error while calling stripe");
        } catch (InvalidRequestException e) {
            // Invalid parameters were supplied to Stripe's API
            logger.error("Invalid parameters were supplied to Stripe's API");
        } catch (AuthenticationException e) {
            // Authentication with Stripe's API failed (wrong API key?)
            logger.error("Authentication with Stripe's API failed (wrong API key?)");
        } catch (ApiConnectionException e) {
            // Network communication with Stripe failed
            logger.error("Network communication with Stripe failed");
        } catch (StripeException e) {
            // Generic error
            logger.error("Stripe exception {} ",e.getMessage());
        } catch (Exception e) {
            // Something else happened unrelated to Stripe
            logger.error("Something else happened unrelated to Stripe {} ",e.getMessage());
        }

        //save stripe customerId in Customer table
        if(stripeCustomerId != null ) {
            customerEntity.setStripeCustomerID(stripeCustomerId);
            try {
                restTemplate.put(customerEndpointUPDATE, customerEntity);
                logger.debug("Saved Stripe customer Id for customer {} in database", customerId);
            } catch (Exception e) {
                logger.error("Could not Save customer {} Stripe Id.. check customer endpoint", customerId);
            }
        }

        return stripeCustomerId;
    }

}
