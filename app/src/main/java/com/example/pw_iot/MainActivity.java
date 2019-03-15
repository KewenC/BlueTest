package com.example.pw_iot;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {
	private Handler handler = new Handler();// 创建一个handler对象

	Thread autoSendThread = null;
	private static final int REQUEST_ENABLE_BT = 2;
	private static final int REQUEST_SELECT_DEVICE = 1;
	private static final int UART_PROFILE_DISCONNECTED = 21;
	private static final int UART_PROFILE_CONNECTED = 20;
	private int mState = UART_PROFILE_DISCONNECTED;

	private ArrayAdapter<String> listAdapter;
	private ListView messageListView;
	private BluetoothDevice mDevice = null;
	// private BluetoothDevice mDevice = null;
	private UartService mService = null;
	private Button btnHome, btnScan, btnSend, btnReset, btnClear;
	private BluetoothAdapter mBtAdapter = null;
	private EditText editText_sendMessage;
	private TextView textview_iscConnected;
	private TextView sendValueLength;
	private long sendValueNum = 0;
	private long recValueNum = 0;
	private TextView sendTimes;
	private CheckBox checkBox_dataRec;
	private CheckBox checkBox_autoSend;
	private EditText editText_sendIntervalVal;
	private RadioButton radioSendASCII, radioSendHEX, radioReASCII, radioReHEX;
	private TextView textViewRecLength;
	// private ImageButton imagebuttonHome;
	private ImageButton imagebuttonScan;
	private Spinner spinnerInterval;
	private TextView textViewRecNumVal;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.second_layout);

		// imagebuttonScan=(ImageButton) findViewById(R.id.imageButton_scan);
		btnScan = (Button) findViewById(R.id.button_scan);
		btnSend = (Button) findViewById(R.id.button_send);
		btnReset = (Button) findViewById(R.id.button_reset);
		btnClear = (Button) findViewById(R.id.button_clear);
		editText_sendMessage = (EditText) findViewById(R.id.edittext_sendText);
		textview_iscConnected = (TextView) findViewById(R.id.textView_isconnected_info);
		sendValueLength = (TextView) findViewById(R.id.textView_send_length_val);
		sendTimes = (TextView) findViewById(R.id.textView_send_val);
		checkBox_dataRec = (CheckBox) findViewById(R.id.checkBox_data_rec);
		checkBox_autoSend = (CheckBox) findViewById(R.id.checkBox_auto_send);
		editText_sendIntervalVal = (EditText) findViewById(R.id.edittext_send_interval_val);
		radioSendASCII = (RadioButton) findViewById(R.id.radio_send_ASCII);
		radioSendHEX = (RadioButton) findViewById(R.id.radio_send_HEX);
		radioReASCII = (RadioButton) findViewById(R.id.radio_receive_ASCII);
		radioReHEX = (RadioButton) findViewById(R.id.radio_receive_HEX);
		textViewRecLength = (TextView) findViewById(R.id.textView_rec_length_val);
		btnHome = (Button) findViewById(R.id.button_home);
		textViewRecNumVal = (TextView) findViewById(R.id.textView_Rec_Num_Val);
		// imagebuttonHome = (ImageButton) findViewById(R.id.imageButton_home);
		// 发送时间间隔配置
		// spinnerInterval = (Spinner) findViewById(R.id.spinner_interval);
		// final Integer arrInt[] = new Integer[] { 10, 20, 30, 40, 50, 60, 70
		// };
		// ArrayAdapter<Integer> arrayAdapterInt = new
		// ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item,
		// arrInt);
		// spinnerInterval.setAdapter(arrayAdapterInt);

		// 接收框配置
		messageListView = (ListView) findViewById(R.id.listMessage);
		listAdapter = new ArrayAdapter<String>(this, R.layout.message_detail);
		messageListView.setAdapter(listAdapter);
		messageListView.setDivider(null);
		// 在连接成功之前保证其他发送接收的控件不可用
		editText_sendMessage.setEnabled(false);
		checkBox_autoSend.setEnabled(false);
		editText_sendIntervalVal.setEnabled(false);
		btnSend.setEnabled(false);
		Init_service();// 初始化后台服务

		new Thread() {
			public void run() {
				while (true) {
					if (checkBox_autoSend.isChecked()) {
						try {
							String message = editText_sendMessage.getText().toString();
							final byte[] Tx_value = message.getBytes("UTF-8");
							mService.writeRXCharacteristic(Tx_value);
							handler.post(new Runnable() {
								@Override
								public void run() {
									sendValueLength.setText(Tx_value.length + "");
									sendTimes.setText((++sendValueNum) + "");
								}
							});
							Thread.sleep(Integer.parseInt(editText_sendIntervalVal.getText().toString()));
						} catch (UnsupportedEncodingException e) {
							System.out.println(e.toString());
							e.printStackTrace();
						} catch (InterruptedException e) {
							System.out.println(e.toString());
							e.printStackTrace();
						}
					}

				}
			};
		}.start();
		// "scan/stop"按钮对应的监听器
		btnScan.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.e("TAGF","btnScan"+btnScan.getText());
				// 创建一个蓝牙适配器对象
				mBtAdapter = BluetoothAdapter.getDefaultAdapter();
				// 如果未打开蓝牙就弹出提示对话框提示用户打开蓝牙
				if (!mBtAdapter.isEnabled()) {
					toastMessage("对不起，蓝牙还没有打开");
					System.out.println("蓝牙还没有打开");
					// 弹出请求打开蓝牙对话框
					Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
					startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
				} else {
					// 如果已经打开蓝牙则与远程蓝牙设备进行连接
					if (btnScan.getText().toString().equals("搜索")) {
						Log.e("TAGF","btnScan2");
						/**
						 * 当"scan"按钮点击后，进入DeviceListActivity.class类，弹出该类对应的窗口
						 * ，并自动在窗口内搜索周围的蓝牙设备
						 */
						Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
						startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
					} else {
						Log.e("TAGF","btnScan3");
						/**
						 * 当scan按钮点击之后，该按钮就会变成stop按钮， 如果此时点击了stop按钮，那么就会执行下面的内容
						 */
						if (mDevice != null) {
							// 断开连接
							mService.disconnect();
						}
					}
				}
			}
		});
		// "Send"按钮对应的监听器
		btnSend.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (radioSendASCII.isChecked()) {
					try {
						String message = editText_sendMessage.getText().toString();
						byte[] Tx_value = message.getBytes("UTF-8");
						mService.writeRXCharacteristic(Tx_value);
						sendValueLength.setText(Tx_value.length + "");
						sendTimes.setText((++sendValueNum) + "");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				} else if (radioSendHEX.isChecked()) {
					boolean hex_flag = true;
					String s1 = editText_sendMessage.getText().toString();
					for (int i = 0; i < s1.length(); i++) {
						char charV = s1.charAt(i);
						if ((charV >= '0' && charV <= '9') || (charV >= 'a' && charV <= 'f')
								|| (charV >= 'A' && charV <= 'F')) {
						} else {
							hex_flag = false;
							break;
						}
					}
					if (hex_flag) {
						byte[] bytes;
						if (0 == s1.length() % 2) {
							bytes = Utils.hexStringToBytes(s1);
							mService.writeRXCharacteristic(bytes);
							sendValueLength.setText(s1.length() + "");
							sendTimes.setText((++sendValueNum) + "");
						} else {
							String s2 = s1.substring(0, (s1.length() - 1));
							bytes = Utils.hexStringToBytes(s2);
							mService.writeRXCharacteristic(bytes);
							sendValueLength.setText((s1.length() - 1) + "");
							sendTimes.setText((++sendValueNum) + "");
						}
					} else {
						Toast toast = Toast.makeText(getApplicationContext(), "【错误】: 输入的字符不是 16进制", Toast.LENGTH_SHORT);
						toast.setGravity(Gravity.CENTER, 0, 0);
						toast.show();
					}
				}
			}
		});
		btnReset.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				editText_sendMessage.setText("");
				sendValueLength.setText("");
				sendTimes.setText("");
				sendValueNum = 0;
			}
		});
		btnClear.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				listAdapter.clear();
				textViewRecLength.setText("");
				textViewRecNumVal.setText("");
				recValueNum = 0;
			}
		});
		btnHome.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_SELECT_DEVICE:
			// 如果选择搜索到的蓝牙设备页面操作成功（即选择远程设备成功，并返回所选择的远程设备地址信息）
			if (resultCode == Activity.RESULT_OK && data != null) {
				String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
				mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
				System.out.println("远程蓝牙Address：" + mDevice);
				System.out.println("mserviceValue:" + mService);
				boolean isconnected = mService.connect(deviceAddress);
				System.out.println("已连接吗？" + isconnected);
			}
			break;
		case REQUEST_ENABLE_BT:
			// 如果请求打开蓝牙页面操作成功（蓝牙成功打开）
			if (resultCode == Activity.RESULT_OK) {
				Toast.makeText(this, "蓝牙已经成功打开", Toast.LENGTH_SHORT).show();
			} else {
				// 请求打开蓝牙页面操作不成功（蓝牙为打开或者打开错误）
				// Log.d(TAG, "蓝牙未打开");
				System.out.println("蓝牙未打开");
				Toast.makeText(this, "打开蓝牙时发生错误", Toast.LENGTH_SHORT).show();
				finish();
			}
			break;
		default:
			System.out.println("wrong request code");
			break;
		}
	}

	private void Init_service() {
		System.out.println("Init_service");
		Intent bindIntent = new Intent(this, UartService.class);
		bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
		LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver,
				makeGattUpdateIntentFilter());
	}

	// UART service connected/disconnected
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		// 与UART服务的连接建立
		public void onServiceConnected(ComponentName className, IBinder rawBinder) {
			mService = ((UartService.LocalBinder) rawBinder).getService();
			System.out.println("uart服务对象：" + mService);
			if (!mService.initialize()) {
				System.out.println("创建蓝牙适配器失败");
				// 因为创建蓝牙适配器失败，导致下面的工作无法进展，所以需要关闭当前uart服务
				finish();
			}
		}

		// 与UART服务的连接失去
		public void onServiceDisconnected(ComponentName classname) {
			// mService.disconnect(mDevice);
			mService = null;
		}
	};

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
		return intentFilter;
	}

	private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			final Intent mIntent = intent;
			// 建立连接
			if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
				System.out.println("BroadcastReceiver:ACTION_GATT_CONNECTED");
				textview_iscConnected.setText("已建立连接");
				String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());

				btnScan.setText("断开");
				editText_sendMessage.setEnabled(true);
				checkBox_autoSend.setEnabled(true);
				editText_sendIntervalVal.setEnabled(true);
				btnSend.setEnabled(true);
				listAdapter.add("[" + currentDateTimeString + "] Connected to: " + mDevice.getName());
				messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
				mState = UART_PROFILE_CONNECTED;
			}
			// 断开连接
			if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
				System.out.println("BroadcastReceiver:ACTION_GATT_DISCONNECTED");
				textview_iscConnected.setText("已断开连接");
				String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
				btnScan.setText("搜索");
				editText_sendMessage.setEnabled(false);
				checkBox_autoSend.setEnabled(false);
				editText_sendIntervalVal.setEnabled(false);
				btnSend.setEnabled(false);
				listAdapter.add("[" + currentDateTimeString + "] Disconnected to: " + mDevice.getName());
				mState = UART_PROFILE_DISCONNECTED;
				mService.close();
			}
			// 有数据可以接收
			if ((action.equals(UartService.ACTION_DATA_AVAILABLE)) && (checkBox_dataRec.isChecked())) {
				byte[] rxValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
				if (radioReASCII.isChecked()) {
					try {
						String Rx_str = new String(rxValue, "UTF-8");
						listAdapter.add("[" + DateFormat.getTimeInstance().format(new Date()) + "] RX: " + Rx_str);
						messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
					} catch (Exception e) {
						System.out.println(e.toString());
					}
				} else {
					String Rx_str = "";
					for (int i = 0; i < rxValue.length; i++) {
						if (rxValue[i] >= 0)
							Rx_str = Rx_str + Integer.toHexString(rxValue[i]) + " ";
						else
							Rx_str = Rx_str + Integer.toHexString(rxValue[i] & 0x0ff) + " ";
					}
					listAdapter.add("[" + DateFormat.getTimeInstance().format(new Date()) + "] RX: " + Rx_str);
					messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
				}
				textViewRecLength.setText(Integer.toString(rxValue.length));
				textViewRecNumVal.setText((++recValueNum) + "");
			}
			// 未知功能1
			if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
				mService.enableTXNotification();
			}
			// 未知功能2
			if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
				toastMessage("Device doesn't support UART. Disconnecting");
				mService.disconnect();
			}
		}
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		System.out.println("onDestroy");
		try {
			// 解注册广播过滤器
			LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
		} catch (Exception ignore) {
			System.out.println(ignore.toString());
		}
		// 解绑定服务
		unbindService(mServiceConnection);
		// 关闭服务对象
		mService.stopSelf();
		mService = null;
	}

	private void toastMessage(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		System.out.println("在MainActivity下按下了back键");
	}
}
