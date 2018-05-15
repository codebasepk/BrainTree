package com.byteshaft.braintree;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.byteshaft.requests.HttpRequest;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.Collections;

public class MainActivity extends AppCompatActivity implements HttpRequest.OnErrorListener {


    private String mAuthorization;
    private BraintreeFragment mBraintreeFragment;
    private Button pay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pay = findViewById(R.id.pay);
        pay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTokenForPayment();
            }
        });
        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, "sandbox_yzv5vq45_zf39nyvdzr5y8666");
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        }

    }

    private void getTokenForPayment() {
        HttpRequest request = new HttpRequest(this);
        request.setOnReadyStateChangeListener(new HttpRequest.OnReadyStateChangeListener() {
            @Override
            public void onReadyStateChange(HttpRequest request, int readyState) {
                switch (readyState) {
                    case HttpRequest.STATE_DONE:
                        switch (request.getStatus()) {
                            case HttpURLConnection.HTTP_OK:
                                Log.i("TAG", "token " + request.getResponseText());
                                try {
                                    JSONObject jsonObject = new JSONObject(request.getResponseText());
                                    doTransaction(jsonObject.getString("token"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                break;
                        }
                }

            }
        });
        request.setOnErrorListener(this);
        request.open("GET", String.format("%spayments/token", AppGlobals.BASE_URL));
        request.send();
    }

    public void doTransaction(String token) {
        mBraintreeFragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                Log.i("TAG", "payment method nounce");
            }
        });
        DropInRequest dropInRequest = new DropInRequest()
                .clientToken(token);
        Log.i("TAG", "enabled " + dropInRequest.isPayPalEnabled());
        dropInRequest.amount("30");
        dropInRequest.collectDeviceData(false);
        dropInRequest.requestThreeDSecureVerification(true);
        dropInRequest.tokenizationKey(token);
        dropInRequest.paypalAdditionalScopes(Collections.singletonList(PayPal.SCOPE_ADDRESS));
        startActivityForResult(dropInRequest.getIntent(this), 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101) {
            if (resultCode == Activity.RESULT_OK) {
                //                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                //                if (result.getPaymentMethodNonce() instanceof PayPalAccountNonce) {
                //                    PayPalAccountNonce payPalAccountNonce = (PayPalAccountNonce) result.getPaymentMethodNonce();
                //
                //                    // Access additional information
                //                    String email = payPalAccountNonce.getEmail();
                //                    String firstName = payPalAccountNonce.getFirstName();
                //                    String lastName = payPalAccountNonce.getLastName();
                //                    String phone = payPalAccountNonce.getPhone();
                //
                //                    // See PostalAddress.java for details
                //                    PostalAddress billingAddress = payPalAccountNonce.getBillingAddress();
                //                    PostalAddress shippingAddress = payPalAccountNonce.getShippingAddress();
                //                    Log.i("email" , email);
                //                }
                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                String paymentMethodNonce = result.getPaymentMethodNonce().getNonce();
                // send paymentMethodNonce to your server
                sendRequestToDoPayment(paymentMethodNonce);
                Log.i("paymentMethodNonce", paymentMethodNonce);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // canceled
            } else {
                // an error occurred, checked the returned exception
                Exception exception = (Exception) data.getSerializableExtra(DropInActivity.EXTRA_ERROR);
                Log.i("exception", exception.getMessage());
                exception.printStackTrace();
            }
        }
    }

    private void sendRequestToDoPayment(String paymentMethodNonce) {
        HttpRequest request = new HttpRequest(this);
        request.setOnReadyStateChangeListener(new HttpRequest.OnReadyStateChangeListener() {
            @Override
            public void onReadyStateChange(HttpRequest request, int readyState) {
                switch (readyState) {
                    case HttpRequest.STATE_DONE:
                        switch (request.getStatus()) {
                            case HttpURLConnection.HTTP_OK:
                                Log.i("TAG", "res " + request.getResponseText());
                                try {
                                    JSONObject jsonObject = new JSONObject(request.getResponseText());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case HttpURLConnection.HTTP_BAD_REQUEST:
                                Log.i("TAG", "res " + request.getResponseText());
                                break;
                        }
                }
            }
        });
        request.setOnErrorListener(this);
        request.open("POST", String.format("%spayments/pay", AppGlobals.BASE_URL));
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("payment_method_nonce", paymentMethodNonce);
        } catch (
                JSONException e)

        {
            e.printStackTrace();
        }
        request.send(jsonObject.toString());
    }

    @Override
    public void onError(HttpRequest request, int readyState, short error, Exception exception) {

    }
}
