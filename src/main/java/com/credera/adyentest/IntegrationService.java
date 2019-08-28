package com.credera.adyentest;

import com.adyen.Client;
import com.adyen.enums.Environment;
import com.adyen.model.Amount;
import com.adyen.model.checkout.*;
import com.adyen.service.Checkout;
import com.google.gson.Gson;
import io.javalin.Javalin;

import java.util.Map;

/**
 * @author Matthew Shepard 8/28/19
 */
public class IntegrationService
{
   private static final int PORT = 8019;
   private static final String API_TOKEN = "AQEihmfuXNWTK0Qc+iSTgGE8ovaauog63+AKZeLgKsSreQW4yhDBXVsNvu" +
         "R83LVYjEgiTGAH-eMiXUh8k3r6lt2U8Ct+rGGmdVAQqkKlwD9xzj3M1md4=-2UHgT9IK3DUwwuNt";
   private static final String MERCHANT_ACCOUNT = "CrederaECOM";

   public static void main(String[] argv){
      new IntegrationService().startService();
   }

   public void startService(){
      System.out.println("Starting test integration server on port " + PORT + "...");
      Javalin app = Javalin.create().start(PORT);
      app.get("/paymentmethods", ctx -> {
         String paymentJson = new Gson().toJson(getPaymentMethods());
         ctx.result(paymentJson);
      });
      app.post("/makepayment", ctx -> {
         Map<String,String> body = new Gson().fromJson(ctx.body(), Map.class);
         String resultJson = new Gson().toJson(makePayment(body));
         ctx.result(resultJson);
      });
      app.post("/paymentdetails", ctx -> {
         Map<String,String> body = new Gson().fromJson(ctx.body(), Map.class);
         String resultJson = new Gson().toJson(addDetails(body));
         ctx.result(resultJson);
      });
   }

   private PaymentMethodsResponse getPaymentMethods() throws Exception
   {
      Client client = new Client(API_TOKEN, Environment.TEST);

      Checkout checkout = new Checkout(client);
      PaymentMethodsRequest paymentMethodsRequest = new PaymentMethodsRequest();
      paymentMethodsRequest.setMerchantAccount(MERCHANT_ACCOUNT);
      paymentMethodsRequest.setCountryCode("US");
      Amount amount = new Amount();
      amount.setCurrency("USD");
      amount.setValue(3500L);
      paymentMethodsRequest.setAmount(amount);
      paymentMethodsRequest.setChannel(PaymentMethodsRequest.ChannelEnum.ANDROID);
      PaymentMethodsResponse response = checkout.paymentMethods(paymentMethodsRequest);
      return response;
   }

   private PaymentsResponse makePayment(Map<String, String> args) throws Exception
   {
      Client client = new Client(API_TOKEN, Environment.TEST);

      Checkout checkout = new Checkout(client);
      PaymentsRequest paymentsRequest = new PaymentsRequest();
      paymentsRequest.setMerchantAccount(MERCHANT_ACCOUNT);
      Amount amount = new Amount();
      amount.setCurrency(args.get("currency"));
      amount.setValue((long)(Float.parseFloat(args.get("amount")) * 100));
      paymentsRequest.setAmount(amount);
      String encryptedCardNumber = args.get("encryptedCardNumber");//"adyenjs_0_1_18$...encryptedCardNumber";
      String encryptedExpiryMonth = args.get("encryptedExpiryMonth");//"adyenjs_0_1_18$...encryptedExpiryMonth";
      String encryptedExpiryYear = args.get("encryptedExpiryYear");//"adyenjs_0_1_18$...encryptedExpiryYear";
      String encryptedSecurityCode = args.get("encryptedSecurityCode");//"adyenjs_0_1_18$...encryptedSecurityCode";
      String holderName = args.get("holderName");
      paymentsRequest.setReference(args.get("referenceText"));
      paymentsRequest.addEncryptedCardData(encryptedCardNumber, encryptedExpiryMonth, encryptedExpiryYear, encryptedSecurityCode, holderName);
      paymentsRequest.setReturnUrl(args.get("returnUrl"));
      PaymentsResponse paymentsResponse = checkout.payments(paymentsRequest);
      return paymentsResponse;
   }

   private PaymentsResponse addDetails(Map<String, String> details) throws Exception
   {
      Client client = new Client(API_TOKEN, Environment.TEST);

      Checkout checkout = new Checkout(client);
      PaymentsDetailsRequest paymentDetailsRequest = new PaymentsDetailsRequest();
      paymentDetailsRequest.setDetails(details);
      PaymentsResponse paymentsResponse = checkout.paymentsDetails(paymentDetailsRequest);
      return paymentsResponse;
   }
}
