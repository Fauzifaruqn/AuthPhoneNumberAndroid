package com.example.applikasihawker;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.applikasihawker.Model.CheckUserResponse;
import com.example.applikasihawker.Model.User;
import com.example.applikasihawker.Retrofit.IDHawkerAPI;
import com.example.applikasihawker.Utils.Common;
import com.facebook.accountkit.Account;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.facebook.accountkit.AccountKitLoginResult;
import com.facebook.accountkit.ui.AccountKitActivity;
import com.facebook.accountkit.ui.AccountKitConfiguration;
import com.facebook.accountkit.ui.LoginType;
import com.facebook.accountkit.ui.ThemeUIManager;
import com.facebook.accountkit.ui.UIManager;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.szagurskii.patternedtextwatcher.PatternedTextWatcher;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


import dmax.dialog.SpotsDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1000;
    Button btn_continue;
    IDHawkerAPI mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_layout);


        mService = Common.getAPI();

        btn_continue = (Button)findViewById(R.id.btn_continue);
        btn_continue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLoginType(LoginType.PHONE);
            }
        });
    }


    private void startLoginType(LoginType phone) {
        Intent intent = new Intent(this, AccountKitActivity.class);

        AccountKitConfiguration.AccountKitConfigurationBuilder builder =
                new AccountKitConfiguration.AccountKitConfigurationBuilder(LoginType.PHONE,
                        AccountKitActivity.ResponseType.TOKEN);
        intent.putExtra(AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION,builder.build());
        startActivityForResult(intent,REQUEST_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE)
        {
            AccountKitLoginResult result = data.getParcelableExtra(AccountKitLoginResult.RESULT_KEY);

            if(result.getError() != null)
            {
                Toast.makeText(this, ""+result.getError().getErrorType().getMessage(), Toast.LENGTH_SHORT).show();
            }

            else if (result.wasCancelled())
            {
                Toast.makeText(this, "Cancel", Toast.LENGTH_SHORT).show();
            }


            else
            {
                if(result.getAccessToken() != null)
                {
                    final android.app.AlertDialog alertDialog = new SpotsDialog(MainActivity.this);
                    alertDialog.show();
                    alertDialog.setMessage("Please Waiting...");

                    AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                        @Override
                        public void onSuccess(final Account account) {


                            mService.checkUserExists(account.getPhoneNumber().toString())
                                    .enqueue(new Callback<CheckUserResponse>() {

                                        @Override
                                        public void onResponse(Call<CheckUserResponse> call, Response<CheckUserResponse> response) {
                                            CheckUserResponse userResponse = response.body();
                                            if(userResponse.isExists())
                                            {
                                                //if user already exists, just login
                                                final android.app.AlertDialog waitingDialog = new SpotsDialog(MainActivity.this);
                                                alertDialog.dismiss();
                                                alertDialog.setMessage("Please waiting ");
                                            }
                                            else
                                            {
                                                //need register
                                                alertDialog.dismiss();

                                                showRegisterDialog(account.getPhoneNumber().toString());
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<CheckUserResponse> call, Throwable t) {

                                        }
                                    });

                        }

                        @Override
                        public void onError(AccountKitError accountKitError) {

                            Log.d("ERROR",accountKitError.getErrorType().getMessage());

                        }
                    });
                }
            }
        }
    }




    private void showRegisterDialog(final String phone) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("REGISTER");


        LayoutInflater inflater = this.getLayoutInflater();
        View register_layout = inflater.inflate(R.layout.register_layout,null);

        final MaterialEditText  edt_name = (MaterialEditText)register_layout.findViewById(R.id.edt_name);
        final MaterialEditText edt_address = (MaterialEditText)register_layout.findViewById(R.id.edt_address);
        final MaterialEditText edt_birthdate = (MaterialEditText)register_layout.findViewById(R.id.edt_birthdate);

        Button btn_register = (Button)register_layout.findViewById(R.id.btn_register);
        edt_birthdate.addTextChangedListener(new PatternedTextWatcher("####-##-##"));

        builder.setView(register_layout);

        final AlertDialog dialog = builder.create();

        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();



                if (TextUtils.isEmpty(edt_address.getText().toString())) {
                    Toast.makeText(MainActivity.this, "Please Enter Your Address", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(edt_birthdate.getText().toString())) {
                    Toast.makeText(MainActivity.this, "Please Enter Your Birthdate", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(edt_name.getText().toString())) {
                    Toast.makeText(MainActivity.this, "Please Enter Your Name", Toast.LENGTH_SHORT).show();
                    return;
                }

                final android.app.AlertDialog waitingDialog = new SpotsDialog(MainActivity.this);
                waitingDialog.show();
                waitingDialog.setMessage("Please Waiting...");



                mService.registerNewUser(phone,
                        edt_name.getText().toString(),
                        edt_address.getText().toString(),
                        edt_birthdate.getText().toString())
                        .enqueue(new Callback<User>() {
                            @Override
                            public void onResponse(Call<User> call, Response<User> response) {
                                waitingDialog.dismiss();
                                User user = response.body();

                                if(TextUtils.isEmpty(user.getError_msg()))
                                {
                                    Toast.makeText(MainActivity.this, "User Register Successfuly ", Toast.LENGTH_SHORT).show();
                                }

                            }

                            @Override
                            public void onFailure(Call<User> call, Throwable t) {
                                waitingDialog.dismiss();

                            }
                        });

            }
        });

        dialog.show();

    }


    private void printKeyHash() {

        try {
            PackageInfo info = getPackageManager().getPackageInfo("com.example.applikasihawker", PackageManager.GET_SIGNATURES);

            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KEYHASH", Base64.encodeToString(md.digest(),Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

}

