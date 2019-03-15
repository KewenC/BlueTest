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
	private Handler handler = new Handler();// ����һ��handler����

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
		// ����ʱ��������
		// spinnerInterval = (Spinner) findViewById(R.id.spinner_interval);
		// final Integer arrInt[] = new Integer[] { 10, 20, 30, 40, 50, 60, 70
		// };
		// ArrayAdapter<Integer> arrayAdapterInt = new
		// ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item,
		// arrInt);
		// spinnerInterval.setAdapter(arrayAdapterInt);

		// ���տ�����
		messageListView = (ListView) findViewById(R.id.listMessage);
		listAdapter = new ArrayAdapter<String>(this, R.layout.message_detail);
		messageListView.setAdapter(listAdapter);
		messageListView.setDivider(null);
		// �����ӳɹ�֮ǰ��֤�������ͽ��յĿؼ�������
		editText_sendMessage.setEnabled(false);
		checkBox_autoSend.setEnabled(false);
		editText_sendIntervalVal.setEnabled(false);
		btnSend.setEnabled(false);
		Init_service();// ��ʼ����̨����

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
		// "scan/stop"��ť��Ӧ�ļ�����
		btnScan.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.e("TAGF","btnScan"+btnScan.getText());
				// ����һ����������������
				mBtAdapter = BluetoothAdapter.getDefaultAdapter();
				// ���δ�������͵�����ʾ�Ի�����ʾ�û�������
				if (!mBtAdapter.isEnabled()) {
					toastMessage("�Բ���������û�д�");
					System.out.println("������û�д�");
					// ��������������Ի���
					Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
					startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
				} else {
					// ����Ѿ�����������Զ�������豸��������
					if (btnScan.getText().toString().equals("����")) {
						Log.e("TAGF","btnScan2");
						/**
						 * ��"scan"��ť����󣬽���DeviceListActivity.class�࣬���������Ӧ�Ĵ���
						 * �����Զ��ڴ�����������Χ�������豸
						 */
						Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
						startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
					} else {
						Log.e("TAGF","btnScan3");
						/**
						 * ��scan��ť���֮�󣬸ð�ť�ͻ���stop��ť�� �����ʱ�����stop��ť����ô�ͻ�ִ�����������
						 */
						if (mDevice != null) {
							// �Ͽ�����
							mService.disconnect();
						}
					}
				}
			}
		});
		// "Send"��ť��Ӧ�ļ�����
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
						Toast toast = Toast.makeText(getApplicationContext(), "������: ������ַ����� 16����", Toast.LENGTH_SHORT);
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
			// ���ѡ���������������豸ҳ������ɹ�����ѡ��Զ���豸�ɹ�����������ѡ���Զ���豸��ַ��Ϣ��
			if (resultCode == Activity.RESULT_OK && data != null) {
				String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
				mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
				System.out.println("Զ������Address��" + mDevice);
				System.out.println("mserviceValue:" + mService);
				boolean isconnected = mService.connect(deviceAddress);
				System.out.println("��������" + isconnected);
			}
			break;
		case REQUEST_ENABLE_BT:
			// ������������ҳ������ɹ��������ɹ��򿪣�
			if (resultCode == Activity.RESULT_OK) {
				Toast.makeText(this, "�����Ѿ��ɹ���", Toast.LENGTH_SHORT).show();
			} else {
				// ���������ҳ��������ɹ�������Ϊ�򿪻��ߴ򿪴���
				// Log.d(TAG, "����δ��");
				System.out.println("����δ��");
				Toast.makeText(this, "������ʱ��������", Toast.LENGTH_SHORT).show();
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
		// ��UART��������ӽ���
		public void onServiceConnected(ComponentName className, IBinder rawBinder) {
			mService = ((UartService.LocalBinder) rawBinder).getService();
			System.out.println("uart�������" + mService);
			if (!mService.initialize()) {
				System.out.println("��������������ʧ��");
				// ��Ϊ��������������ʧ�ܣ���������Ĺ����޷���չ��������Ҫ�رյ�ǰuart����
				finish();
			}
		}

		// ��UART���������ʧȥ
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
			// ��������
			if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
				System.out.println("BroadcastReceiver:ACTION_GATT_CONNECTED");
				textview_iscConnected.setText("�ѽ�������");
				String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());

				btnScan.setText("�Ͽ�");
				editText_sendMessage.setEnabled(true);
				checkBox_autoSend.setEnabled(true);
				editText_sendIntervalVal.setEnabled(true);
				btnSend.setEnabled(true);
				listAdapter.add("[" + currentDateTimeString + "] Connected to: " + mDevice.getName());
				messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
				mState = UART_PROFILE_CONNECTED;
			}
			// �Ͽ�����
			if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
				System.out.println("BroadcastReceiver:ACTION_GATT_DISCONNECTED");
				textview_iscConnected.setText("�ѶϿ�����");
				String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
				btnScan.setText("����");
				editText_sendMessage.setEnabled(false);
				checkBox_autoSend.setEnabled(false);
				editText_sendIntervalVal.setEnabled(false);
				btnSend.setEnabled(false);
				listAdapter.add("[" + currentDateTimeString + "] Disconnected to: " + mDevice.getName());
				mState = UART_PROFILE_DISCONNECTED;
				mService.close();
			}
			// �����ݿ��Խ���
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
			// δ֪����1
			if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
				mService.enableTXNotification();
			}
			// δ֪����2
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
			// ��ע��㲥������
			LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
		} catch (Exception ignore) {
			System.out.println(ignore.toString());
		}
		// ��󶨷���
		unbindService(mServiceConnection);
		// �رշ������
		mService.stopSelf();
		mService = null;
	}

	private void toastMessage(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		System.out.println("��MainActivity�°�����back��");
	}
}
