package food.togo.payment.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import food.togo.payment.request.ChargeRequest;
import food.togo.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class PaymentEndpoint {

    //@Value("${STRIPE_PUBLIC_KEY}")
    private String stripePublicKey = "pk_test_JMlbR4PSbP5qPVON78QsDW8b";

    @Autowired PaymentService paymentService;

    @PostMapping("/charge")
    public String charge(ChargeRequest chargeRequest, Model model) throws StripeException, Exception {
        chargeRequest.setDescription("Example charge");
        chargeRequest.setCurrency(ChargeRequest.Currency.USD);
        Charge charge = null;
        charge = paymentService.chargeCustomer(chargeRequest);
        model.addAttribute("id", charge.getId());
        model.addAttribute("status", charge.getStatus());
        model.addAttribute("chargeId", charge.getId());
        model.addAttribute("balance_transaction", charge.getBalanceTransaction());
        return "result";
    }

    @ExceptionHandler(StripeException.class)
    public String handleError(Model model, StripeException ex) {
        model.addAttribute("error", ex.getMessage());
        return "result";
    }

    @ExceptionHandler(Exception.class)
    public String handleError(Model model, Exception ex) {
        model.addAttribute("error", ex.getMessage());
        return "result";
    }

    @RequestMapping("/checkout/{customerId}")
    public String checkout(Model model, Integer customerId) {
        model.addAttribute("amount", 50 * 100); // in cents
        model.addAttribute("stripePublicKey", stripePublicKey);
        model.addAttribute("currency", ChargeRequest.Currency.USD);
        model.addAttribute("customerId", customerId);
        return "checkout";
    }
}
