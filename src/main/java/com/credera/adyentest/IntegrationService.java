package com.credera.adyentest;

import com.adyen.Client;
import com.adyen.enums.Environment;
import com.adyen.model.Amount;
import com.adyen.model.ThreeDS2RequestData;
import com.adyen.model.checkout.*;
import com.adyen.service.Checkout;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.javalin.Javalin;

import java.util.HashMap;
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
      app.post("/paymentMethods", ctx -> {
         String paymentJson = new Gson().toJson(getPaymentMethods());
         ctx.result(paymentJson);
      });
      app.post("/payments", ctx -> {
         Map<String,String> body = new Gson().fromJson(ctx.body(), Map.class);
         Gson gson = new Gson();
         String resultJson = gson.toJson(makePayment(body));
         Map modifymap = gson.fromJson(resultJson, Map.class);
         if(modifymap.get("action") != null){
            ((Map)modifymap.get("action")).put("paymentData", modifymap.get("paymentData"));
         }
         ctx.result(gson.toJson(modifymap));
      });
      app.post("/payments/details", ctx -> {
         Map<String,String> body = new Gson().fromJson(ctx.body(), Map.class);
         Gson gson = new Gson();
         String resultJson = gson.toJson(addDetails(body));
         Map modifymap = gson.fromJson(resultJson, Map.class);
         if(modifymap.get("action") != null){
            ((Map)modifymap.get("action")).put("paymentData", modifymap.get("paymentData"));
         }
         ctx.result(gson.toJson(modifymap));
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

      Gson gson = new Gson();
      JsonObject bodyObj = gson.toJsonTree(args).getAsJsonObject();

      JsonObject amountObj = bodyObj.getAsJsonObject("amount");
      JsonObject paymentMethodObj = bodyObj.getAsJsonObject("paymentMethod");
      JsonObject addtlDataObj = bodyObj.getAsJsonObject("additionalData");

      amount.setCurrency(amountObj.get("currency").getAsString());
      amount.setValue((long)(Float.parseFloat(amountObj.get("value").getAsString())));
      System.out.println("Value  " + amount.getDecimalValue() + ", " + amount.getValue());
      paymentsRequest.setAmount(amount);

      // Currently defaulted to true on the request from app
      boolean allow3DS2 = addtlDataObj.get("allow3DS2").getAsBoolean();
      if (allow3DS2) {
         paymentsRequest.setChannel(PaymentsRequest.ChannelEnum.fromValue(bodyObj.get("channel").getAsString()));
         paymentsRequest.setShopperIP("192.168.66.76");
         ThreeDS2RequestData threeDS2RequestData = new ThreeDS2RequestData();
         paymentsRequest.setThreeDS2RequestData(threeDS2RequestData);
         Map <String,String> additionalData = new HashMap<>();
         additionalData.put("allow3DS2", "true");
         paymentsRequest.setAdditionalData(additionalData);
      }

      String encryptedCardNumber = paymentMethodObj.get("encryptedCardNumber").getAsString();//"adyenjs_0_1_18$...encryptedCardNumber";
      String encryptedExpiryMonth = paymentMethodObj.get("encryptedExpiryMonth").getAsString();//"adyenjs_0_1_18$...encryptedExpiryMonth";
      String encryptedExpiryYear = paymentMethodObj.get("encryptedExpiryYear").getAsString();//"adyenjs_0_1_18$...encryptedExpiryYear";
      String encryptedSecurityCode = paymentMethodObj.get("encryptedSecurityCode").getAsString();//"adyenjs_0_1_18$...encryptedSecurityCode";
      String holderName = "John Smith";// paymentMethodObj.get("holderName").getAsString();
      paymentsRequest.setReference(bodyObj.get("reference").getAsString());
      paymentsRequest.addEncryptedCardData(encryptedCardNumber, encryptedExpiryMonth, encryptedExpiryYear, encryptedSecurityCode, holderName);
      paymentsRequest.setReturnUrl(bodyObj.get("returnUrl").getAsString());
      PaymentsResponse paymentsResponse = checkout.payments(paymentsRequest);
      return paymentsResponse;
   }

   private PaymentsResponse addDetails(Map<String, String> details) throws Exception
   {
      Client client = new Client(API_TOKEN, Environment.TEST);

      Checkout checkout = new Checkout(client);
      PaymentsDetailsRequest paymentDetailsRequest = new PaymentsDetailsRequest();
      Gson gson = new Gson();
      JsonObject detailsObj = gson.toJsonTree(details.get("details")).getAsJsonObject();
      Map<String,String> detailsMap = new Gson().fromJson(detailsObj,Map.class);
      paymentDetailsRequest.setDetails(detailsMap);
      paymentDetailsRequest.setPaymentData(details.get("paymentData"));
      PaymentsResponse paymentsResponse = checkout.paymentsDetails(paymentDetailsRequest);
      return paymentsResponse;
   }

   private void challengeShopper()
   {
      return;
   }
}
