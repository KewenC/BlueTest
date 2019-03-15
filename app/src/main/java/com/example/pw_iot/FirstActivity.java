package com.example.pw_iot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class FirstActivity extends Activity {
	private TextView tvPWlink;
	private Button buttonBLETransimit;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.first_layout);

		tvPWlink = (TextView) findViewById(R.id.textView_pw_link);
		tvPWlink.setMovementMethod(LinkMovementMethod.getInstance());
		Spanned text = Html.fromHtml("<a href=\"http://www.phangwei.com\">www.phangwei.com</a>");
		tvPWlink.setText(text);

		buttonBLETransimit = (Button) findViewById(R.id.button_ble_transmit);
		buttonBLETransimit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(FirstActivity.this, MainActivity.class);
				startActivity(intent);
			}
		});

	}
}
