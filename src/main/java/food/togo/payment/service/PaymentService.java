package food.togo.payment.service;

import com.stripe.Stripe;
import com.stripe.exception.*;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import food.togo.payment.config.RestTemplateConfig;
import food.togo.payment.dto.CustomerEntity;
import food.togo.payment.entities.CustomerStripe;
import food.togo.payment.entities.PaymentHistory;
import food.togo.payment.repositories.CustomerStripeRepository;
import food.togo.payment.request.ChargeRequest;
import food.togo.platform.EncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service("payment")
public class PaymentService {

    @Autowired
    CustomerStripeRepository customerStripeRepository;

    private String stripePublicKey = "pk_test_JMlbR4PSbP5qPVON78QsDW8b";



    Logger logger = LoggerFactory.getLogger(PaymentService.class);


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

    private String getStripeCustomerId(ChargeRequest chargeRequest) {

        Integer customerId = chargeRequest.getCustomerId();
        String stripeCustomerId = getStripeCustomerId(customerId);

        if( stripeCustomerId == null ) {
            stripeCustomerId = createStripeCustomer(chargeRequest);
        }
        //save stripe customerId in Customer table
        if(stripeCustomerId != null ) {
            saveStripeCustomer(stripeCustomerId, customerId);
        }

        return stripeCustomerId;
    }


    private String createStripeCustomer(ChargeRequest chargeRequest) {
        try {
            Map<String, Object> chargeParamsForSaveCustomer = new HashMap<>();
            chargeParamsForSaveCustomer.put("source", chargeRequest.getStripeToken());
            chargeParamsForSaveCustomer.put("email", chargeRequest.getStripeEmail());
            Customer customer = Customer.create(chargeParamsForSaveCustomer);
            logger.debug("Saved customer info in Stripe {} ", customer.getId());
            if(customer != null) {
                return customer.getId(); //use this stripe customerId for subsequent payments
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
        return null;
    }


    @Async
    public void saveStripeCustomer(String stripeId, Integer customerId) {
        CustomerStripe customerStripe = new CustomerStripe();

        byte[] salt = EncryptionUtil.getSaltBytes();
        String encryptedStripeId = EncryptionUtil.encrypt(stripeId, salt);
        customerStripe.setStripeId(encryptedStripeId);
        customerStripe.setSalt(salt);
        customerStripe.setCustomerId(customerId);
        customerStripeRepository.save(customerStripe);

        //save stripe customerId in Customer table
        if(stripeCustomerId != null ) {
            //encrypt with salt and IV
            try {
                String encryptedStripeCustomerId =
                        EncryptionUtil.encrypt(stripeCustomerId.toCharArray(), customerEntity.getSalt());
                customerEntity.setStripeCustomerID(encryptedStripeCustomerId);
                restTemplate.put(customerEndpointUPDATE, customerEntity);
                logger.debug("Saved Stripe customer Id for customer {} in database", customerId);
            } catch (Exception e) {
                logger.error("Could not Save customer {} Stripe Id.. check customer endpoint", customerId);
            }
        }

        return stripeCustomerId;
    }

    @Async
    public void savePaymentInformation(Charge charge, Integer customerId, Long orderId) {

        if(charge == null) {
            return;
        }

        PaymentHistory paymentHistory = new PaymentHistory();
        paymentHistory.setAmount(charge.getAmount());
        paymentHistory.setCreated(new java.sql.Date(charge.getCreated()));
        paymentHistory.setCurrency(charge.getCurrency());
        paymentHistory.setCustomerID(customerId);
        paymentHistory.setStripeChargeId(charge.getId());
        paymentHistory.setOrderID(orderId);
        paymentHistory.setRefund(charge.getAmountRefunded());
        paymentHistory.setStatus(charge.getStatus());
        paymentHistory.setError(charge.getFailureMessage());

    }

}
