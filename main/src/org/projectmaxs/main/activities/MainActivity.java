package org.projectmaxs.main.activities;

import java.util.Iterator;
import java.util.Set;

import org.jivesoftware.smack.Connection;
import org.projectmaxs.main.MAXSService;
import org.projectmaxs.main.MAXSService.LocalBinder;
import org.projectmaxs.main.R;
import org.projectmaxs.main.Settings;
import org.projectmaxs.main.util.XMPPUtil;
import org.projectmaxs.main.xmpp.StateChangeListener;
import org.projectmaxs.main.xmpp.XMPPService;
import org.projectmaxs.main.xmpp.XMPPService.State;
import org.projectmaxs.shared.activities.EditTextWatcher;
import org.projectmaxs.shared.util.Log;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final Log LOG = Log.getLog();

	private boolean serviceWasNotConnectedBefore = true;
	private MAXSService mMAXSLocalService = null;

	private Settings mSettings;

	private LinearLayout mMasterAddresses;
	private EditText mFirstMasterAddress;
	private EditText mJID;
	private String mLastJidText;
	private EditText mPassword;
	private Button mConnButton;
	private TextView mStatusText;

	public void openAdvancedSettings(View view) {
		startActivity(new Intent(this, AdvancedSettings.class));
	}

	public void openModules(View view) {
		startActivity(new Intent(this, Modules.class));
	}

	public void openImportExportSettings(View view) {
		startActivity(new Intent(this, ImportExportSettings.class));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mSettings = Settings.getInstance(this);

		// Views
		mMasterAddresses = (LinearLayout) findViewById(R.id.masterAddresses);
		mFirstMasterAddress = (EditText) findViewById(R.id.firstMasterAddress);
		mJID = (EditText) findViewById(R.id.jid);
		mPassword = (EditText) findViewById(R.id.password);
		mConnButton = (Button) findViewById(R.id.buttonConnect);
		mStatusText = (TextView) findViewById(R.id.statusText);

		new MasterAddressCallbacks(mFirstMasterAddress);
		new EditTextWatcher(mJID) {
			@Override
			public void lostFocusOrDone(View v) {
				String text = mJID.getText().toString();
				if (!XMPPUtil.isValidBareJid(text)) {
					Toast.makeText(MainActivity.this, "This is not a valid bare JID", Toast.LENGTH_LONG).show();
					mJID.setText(mLastJidText);
					return;
				}
				mSettings.setJid(text);
			}
		};
		new EditTextWatcher(mPassword) {
			@Override
			public void lostFocusOrDone(View v) {
				mSettings.setPassword(mPassword.getText().toString());
			}
		};

		// initialize the master jid linear layout if there are already some
		// configured
		Set<String> masterJids = mSettings.getMasterJids();
		if (!masterJids.isEmpty()) {
			Iterator<String> it = masterJids.iterator();
			mFirstMasterAddress.setText(it.next());
			while (it.hasNext()) {
				EditText et = addEmptyMasterJidEditText();
				et.setText(it.next());
			}
			addEmptyMasterJidEditText();
		}
		if (!mSettings.getJid().equals("")) mJID.setText(mSettings.getJid());
		if (!mSettings.getPassword().equals("")) mPassword.setText(mSettings.getPassword());

		mConnButton.setEnabled(false);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (mMAXSLocalService == null) {
			Intent intent = new Intent(this, MAXSService.class);
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mMAXSLocalService != null) {
			unbindService(mConnection);
			mMAXSLocalService = null;
		}
	}

	ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			mMAXSLocalService = binder.getService();

			if (serviceWasNotConnectedBefore) {
				StateChangeListener listener = new StateChangeListener() {
					@Override
					public void connected(Connection con) {
						status("connected");
					}

					@Override
					public void disconnected(Connection con) {
						status("disconnected");
					}

					@Override
					public void connecting() {
						status("connecting");
					}

					@Override
					public void disconnecting() {
						status("disconnecting");
					}

					private void status(final String status) {
						MainActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mStatusText.setText(status);
							}
						});
					}

				};
				mMAXSLocalService.getXMPPService().addListener(listener);
				State state = mMAXSLocalService.getXMPPService().getCurrentState();
				switch (state) {
				case Connected:
					listener.connected(null);
					break;
				case Disconnected:
					listener.disconnected(null);
					break;
				default:
					break;
				}
				serviceWasNotConnectedBefore = false;
			}

			mConnButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (mMAXSLocalService != null) {
						XMPPService.State state = mMAXSLocalService.getXMPPService().getCurrentState();
						switch (state) {
						case Connected:
							mMAXSLocalService.stopService();
							break;
						case Disconnected:
							String failureReason = null;
							if (mSettings.getPassword().length() == 0) failureReason = "Password not set or empty";
							if (mSettings.getJid().length() == 0) failureReason = "JID not set or empty";
							if (mSettings.getMasterJidCount() == 0) failureReason = "Master JID(s) not configured";

							if (failureReason != null) {
								mStatusText.setText("Can not connect: " + failureReason);
								return;
							}

							LOG.d("onClick: connection button clicked, calling startService");
							mMAXSLocalService.startService();
							break;
						default:
							break;
						}
					}
				}

			});
			mConnButton.setEnabled(true);

			if (mSettings.connectOnMainScreen() && MAXSService.isRunning()) {
				LOG.d("connectOnMainScreen enabled and service not running, calling startService");
				mMAXSLocalService.startService();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mMAXSLocalService = null;
			mConnButton.setEnabled(false);
		}
	};

	private final EditText addEmptyMasterJidEditText() {
		EditText newEditText = new EditText(MainActivity.this);
		newEditText.setHint(MainActivity.this.getString(R.string.hint_jid));
		new MasterAddressCallbacks(newEditText);
		mMasterAddresses.addView(newEditText);
		return newEditText;
	}

	private final class MasterAddressCallbacks extends EditTextWatcher {

		MasterAddressCallbacks(EditText editText) {
			super(editText);
		}

		public void lostFocusOrDone(View v) {
			String text = mEditText.getText().toString();
			if (text.equals("") && !mBeforeText.equals("")) {
				int childCount = mMasterAddresses.getChildCount();
				mSettings.removeMasterJid(mBeforeText);
				mMasterAddresses.removeView(mEditText);
				if (childCount <= 2) {
					mMasterAddresses.addView(mEditText, 2);
					mEditText.setHint(MainActivity.this.getString(R.string.hint_jid));
				}
				return;
			}

			if (text.equals("")) return;

			// an attempt to change an empty master jid to an invalid jid. abort
			// here and leave the original value untouched
			if (!XMPPUtil.isValidBareJid(text)) {
				Toast.makeText(MainActivity.this, "This is not a valid bare JID", Toast.LENGTH_LONG).show();
				mEditText.setText(mBeforeText);
			}
			// an empty master jid was change to a valid jid
			else if (mBeforeText.equals("")) {
				mSettings.addMasterJid(text);
				addEmptyMasterJidEditText();
			}
			// a valid master jid was changed with another valid value
			else if (!mBeforeText.equals(text)) {
				mSettings.removeMasterJid(mBeforeText);
				mSettings.addMasterJid(text);
			}
			return;
		}

	}
}
