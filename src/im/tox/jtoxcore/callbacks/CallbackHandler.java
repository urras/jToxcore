/* CallbackHandler.java
 *
 *  Copyright (C) 2014 Tox project All Rights Reserved.
 *
 *  This file is part of jToxcore
 *
 *  jToxcore is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  jToxcore is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with jToxcore.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package im.tox.jtoxcore.callbacks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import im.tox.jtoxcore.FriendList;
import im.tox.jtoxcore.JTox;
import im.tox.jtoxcore.ToxFriend;
import im.tox.jtoxcore.ToxFileControl;
import im.tox.jtoxcore.ToxUserStatus;

/**
 * Callback Handler class which contains methods to manage the callbacks for a
 * JTox instance.
 *
 * @author sonOfRa
 * @param <F>
 *            Friend type to use with the CallbackHandler instance
 *
 */
public class CallbackHandler<F extends ToxFriend> {

	private List<OnActionCallback<F>> onActionCallbacks;
	private List<OnConnectionStatusCallback<F>> onConnectionStatusCallbacks;
	private List<OnFriendRequestCallback> onFriendRequestCallbacks;
	private List<OnMessageCallback<F>> onMessageCallbacks;
	private List<OnNameChangeCallback<F>> onNameChangeCallbacks;
	private List<OnReadReceiptCallback<F>> onReadReceiptCallbacks;
	private List<OnStatusMessageCallback<F>> onStatusMessageCallbacks;
	private List<OnUserStatusCallback<F>> onUserStatusCallbacks;
	private List<OnTypingChangeCallback<F>> onTypingChangeCallbacks;
	private List<OnFileControlCallback<F>> onFileControlCallbacks;
	private List<OnFileDataCallback<F>> onFileDataCallbacks;
	private List<OnFileSendRequestCallback<F>> onFileSendRequestCallbacks;

	private FriendList<F> friendlist;

	/**
	 * Default constructor for CallbackHandler. Initializes all Lists as
	 * synchronized lists.
	 *
	 * @param friendlist
	 *            the friendlist of the jtox instance that this handler is
	 *            attached to
	 */
	public CallbackHandler(FriendList<F> friendlist) {
		this.friendlist = friendlist;
		this.onActionCallbacks = Collections.synchronizedList(new ArrayList<OnActionCallback<F>>());
		this.onConnectionStatusCallbacks = Collections.synchronizedList(new ArrayList<OnConnectionStatusCallback<F>>());
		this.onFriendRequestCallbacks = Collections.synchronizedList(new ArrayList<OnFriendRequestCallback>());
		this.onMessageCallbacks = Collections.synchronizedList(new ArrayList<OnMessageCallback<F>>());
		this.onNameChangeCallbacks = Collections.synchronizedList(new ArrayList<OnNameChangeCallback<F>>());
		this.onReadReceiptCallbacks = Collections.synchronizedList(new ArrayList<OnReadReceiptCallback<F>>());
		this.onStatusMessageCallbacks = Collections.synchronizedList(new ArrayList<OnStatusMessageCallback<F>>());
		this.onUserStatusCallbacks = Collections.synchronizedList(new ArrayList<OnUserStatusCallback<F>>());
		this.onTypingChangeCallbacks = Collections.synchronizedList(new ArrayList<OnTypingChangeCallback<F>>());
		this.onFileControlCallbacks = Collections.synchronizedList(new ArrayList<OnFileControlCallback<F>>());
		this.onFileDataCallbacks = Collections.synchronizedList(new ArrayList<OnFileDataCallback<F>>());
		this.onFileSendRequestCallbacks = Collections.synchronizedList(new ArrayList<OnFileSendRequestCallback<F>>());
	}

	/**
	 * Hook for native API to invoke callback methods
	 *
	 * @param friendnumber
	 *            friend who sent the action
	 * @param action
	 *            the action
	 */
	@SuppressWarnings("unused")
	private void onAction(int friendnumber, byte[] action) {
		String actionString = JTox.getByteString(action);
		F friend = this.friendlist.getByFriendNumber(friendnumber);

		synchronized (this.onActionCallbacks) {
			for (OnActionCallback<F> callback : this.onActionCallbacks) {
				callback.execute(friend, actionString);
			}
		}
	}

	/**
	 * Add the specified callback for receiving actions.
	 *
	 * @param callback
	 *            the callback to register
	 */
	public void registerOnActionCallback(OnActionCallback<F> callback) {
		this.onActionCallbacks.add(callback);
	}

	/**
	 * Remove the speciofied callback for receiving actions
	 *
	 * @param callback
	 *            the callback to remove
	 */
	public void unregisterOnActionCallback(OnActionCallback<F> callback) {
		this.onActionCallbacks.remove(callback);
	}

	/**
	 * Remove all action callbacks
	 */
	public void clearOnActionCallbacks() {
		this.onActionCallbacks.clear();
	}

	/**
	 * Add the given callbacks for receiving actions. Retains all previously
	 * existing callbacks.
	 *
	 * @param callbacks
	 *            callbacks to add
	 */
	public <T extends OnActionCallback<F>> void registerOnActionCallbacks(List<T> callbacks) {
		for (T callback : callbacks) {
			registerOnActionCallback(callback);
		}
	}

	/**
	 * Set the given callbacks for receiving actions. Removes all previously
	 * existing callbacks.
	 *
	 * @param callbacks
	 *            callbacks to set
	 */
	public <T extends OnActionCallback<F>> void setOnActionCallbacks(List<T> callbacks) {
		clearOnActionCallbacks();
		registerOnActionCallbacks(callbacks);
	}

	/**
	 * Hook for native API to invoke callback methods
	 *
	 * @param friendnumber
	 *            the friend who's status changed
	 * @param online
	 *            friend's status
	 */
	@SuppressWarnings("unused")
	private void onConnectionStatus(int friendnumber, boolean online) {
		F friend = this.friendlist.getByFriendNumber(friendnumber);

		synchronized (this.onConnectionStatusCallbacks) {
			for (OnConnectionStatusCallback<F> cb : this.onConnectionStatusCallbacks) {
				cb.execute(friend, online);
			}
		}
	}

	/**
	 * Add the specified callback for receiving connection status changes.
	 *
	 * @param callback
	 *            the callback to register
	 */
	public void registerOnConnectionStatusCallback(OnConnectionStatusCallback<F> callback) {
		this.onConnectionStatusCallbacks.add(callback);
	}

	/**
	 * Remove the specified callback for receiving connection status changes.
	 *
	 * @param callback
	 *            the callback to remove
	 */
	public void unregisterOnConnectionStatusCallback(OnConnectionStatusCallback<F> callback) {
		this.onConnectionStatusCallbacks.remove(callback);
	}

	/**
	 * Remove all connection status callbacks
	 */
	public void clearOnConnectionStatusCallbacks() {
		this.onConnectionStatusCallbacks.clear();
	}

	/**
	 * Add the given callbacks for receiving connection status changes. Retains
	 * all previously existing callbacks.
	 *
	 * @param callbacks
	 *            callbacks to set
	 */
	public <T extends OnConnectionStatusCallback<F>> void registerOnConnectionStatusCallbacks(List<T> callbacks) {
		for (T callback : callbacks) {
			registerOnConnectionStatusCallback(callback);
		}
	}

	/**
	 * Set the given callbacks for receiving connection status changes. Removes
	 * all previously existing callbacks.
	 *
	 * @param callbacks
	 *            callbacks to set
	 */
	public <T extends OnConnectionStatusCallback<F>> void setOnConnectionStatusCallbacks(List<T> callbacks) {
		clearOnConnectionStatusCallbacks();
		registerOnConnectionStatusCallbacks(callbacks);
	}

	/**
	 * Hook for native API to invoke callback methods
	 *
	 * @param publicKey
	 *            the public key of the friend
	 * @param message
	 *            the message they sent with the request
	 */
	@SuppressWarnings("unused")
	private void onFriendRequest(String publicKey, byte[] message) {
		String messageString = JTox.getByteString(message);

		synchronized (this.onFriendRequestCallbacks) {
			for (OnFriendRequestCallback cb : this.onFriendRequestCallbacks) {
				cb.execute(publicKey, messageString);
			}
		}
	}

	/**
	 * Add the specified friend request callback
	 *
	 * @param callback
	 *            callback to add
	 */
	public void registerOnFriendRequestCallback(OnFriendRequestCallback callback) {
		this.onFriendRequestCallbacks.add(callback);
	}

	/**
	 * Remove the specified friend request callback
	 *
	 * @param callback
	 *            callback to remove
	 */
	public void unregisterOnFriendRequestCallback(OnFriendRequestCallback callback) {
		this.onFriendRequestCallbacks.remove(callback);
	}

	/**
	 * Remove all friend request callbacks
	 */
	public void clearOnFriendRequestCallbacks() {
		this.onFriendRequestCallbacks.clear();
	}

	/**
	 * Adds the specified callbacks. Retains all existing callbacks
	 *
	 * @param callbacks
	 *            callbacks to add
	 */
	public <T extends OnFriendRequestCallback> void registerOnFriendRequestCallbacks(List<T> callbacks) {
		for (T callback : callbacks) {
			registerOnFriendRequestCallback(callback);
		}
	}

	/**
	 * Set the specified callbacks. Removes all existing callbacks
	 *
	 * @param callbacks
	 *            callbacks to set
	 */
	public <T extends OnFriendRequestCallback> void setOnFriendRequestCallbacks(List<T> callbacks) {
		clearOnFriendRequestCallbacks();
		registerOnFriendRequestCallbacks(callbacks);
	}

	/**
	 * Hook for native API to invoke callback methods
	 *
	 * @param friendnumber
	 *            the friend who sent the message
	 * @param message
	 *            the message
	 */
	@SuppressWarnings("unused")
	private void onFileControl(int friendnumber, int receive_send, int friend_number, ToxFileControl control_type, byte[] data) {
		F friend = this.friendlist.getByFriendNumber(friendnumber);
		boolean sending;

		if (receive_send == 1) {
			sending = true;
		} else {
			sending = false;
		}

		synchronized (this.onMessageCallbacks) {
			for (OnFileControlCallback<F> cb : this.onFileControlCallbacks) {
				cb.execute(friend, sending, friend_number, control_type, data);
			}
		}
	}

	/**
	 * Add the specified callback
	 *
	 * @param callback
	 *            callback to add
	 */
	public void registerOnFileControlCallback(OnFileControlCallback<F> callback) {
		this.onFileControlCallbacks.add(callback);
	}

	/**
	 * Remove the specified callback
	 *
	 * @param callback
	 *            callback to remove
	 */
	public void unregisterOnFileControlCallback(OnFileControlCallback<F> callback) {
		this.onFileControlCallbacks.remove(callback);
	}

	/**
	 * Remove all callbacks
	 */
	public void clearOnFileControlCallbacks() {
		this.onFileControlCallbacks.clear();
	}

	/**
	 * Add all specified callbacks
	 *
	 * @param callbacks
	 *            callbacks to add
	 */
	public <T extends OnFileControlCallback<F>> void registerOnFileControlCallbacks(List<T> callbacks) {
		for (T callback : callbacks) {
			registerOnFileControlCallback(callback);
		}
	}

	/**
	 * Set the specified callbacks. This removes all previously set callbacks
	 *
	 * @param callbacks
	 *            callbacks to set
	 */
	public <T extends OnFileControlCallback<F>> void setOnFileControlCallbacks(List<T> callbacks) {
		clearOnFileControlCallbacks();
		registerOnFileControlCallbacks(callbacks);
	}
	/**
	 * Hook for native API to invoke callback methods
	 *
	 * @param friendnumber
	 *            the friend who sent the message
	 * @param message
	 *            the message
	 */
	@SuppressWarnings("unused")
	private void onFileData(int friendnumber, int filenumber, byte[] data) {
		F friend = this.friendlist.getByFriendNumber(friendnumber);

		synchronized (this.onMessageCallbacks) {
			for (OnFileDataCallback<F> cb : this.onFileDataCallbacks) {
				cb.execute(friend, filenumber, data);
			}
		}
	}

	/**
	 * Add the specified callback
	 *
	 * @param callback
	 *            callback to add
	 */
	public void registerOnFileDataCallback(OnFileDataCallback<F> callback) {
		this.onFileDataCallbacks.add(callback);
	}

	/**
	 * Remove the specified callback
	 *
	 * @param callback
	 *            callback to remove
	 */
	public void unregisterOnFileDataCallback(OnFileDataCallback<F> callback) {
		this.onFileDataCallbacks.remove(callback);
	}

	/**
	 * Remove all callbacks
	 */
	public void clearOnFileDataCallbacks() {
		this.onFileDataCallbacks.clear();
	}

	/**
	 * Add all specified callbacks
	 *
	 * @param callbacks
	 *            callbacks to add
	 */
	public <T extends OnFileDataCallback<F>> void registerOnFileDataCallbacks(List<T> callbacks) {
		for (T callback : callbacks) {
			registerOnFileDataCallback(callback);
		}
	}

	/**
	 * Set the specified callbacks. This removes all previously set callbacks
	 *
	 * @param callbacks
	 *            callbacks to set
	 */
	public <T extends OnFileDataCallback<F>> void setOnFileDataCallbacks(List<T> callbacks) {
		clearOnFileDataCallbacks();
		registerOnFileDataCallbacks(callbacks);
	}
	/**
	 * Hook for native API to invoke callback methods
	 *
	 * @param friendnumber
	 *            the friend who sent the message
	 * @param message
	 *            the message
	 */
	@SuppressWarnings("unused")
	private void onFileSendRequest(int friendnumber, int filenumber, long filesize, byte[] filename) {
		F friend = this.friendlist.getByFriendNumber(friendnumber);

		synchronized (this.onMessageCallbacks) {
			for (OnFileSendRequestCallback<F> cb : this.onFileSendRequestCallbacks) {
				cb.execute(friend, filenumber, filesize, filename);
			}
		}
	}

	/**
	 * Add the specified callback
	 *
	 * @param callback
	 *            callback to add
	 */
	public void registerOnFileSendRequestCallback(OnFileSendRequestCallback<F> callback) {
		this.onFileSendRequestCallbacks.add(callback);
	}

	/**
	 * Remove the specified callback
	 *
	 * @param callback
	 *            callback to remove
	 */
	public void unregisterOnFileSendRequestCallback(OnFileSendRequestCallback<F> callback) {
		this.onFileSendRequestCallbacks.remove(callback);
	}

	/**
	 * Remove all callbacks
	 */
	public void clearOnFileSendRequestCallbacks() {
		this.onFileSendRequestCallbacks.clear();
	}

	/**
	 * Add all specified callbacks
	 *
	 * @param callbacks
	 *            callbacks to add
	 */
	public <T extends OnFileSendRequestCallback<F>> void registerOnFileSendRequestCallbacks(List<T> callbacks) {
		for (T callback : callbacks) {
			registerOnFileSendRequestCallback(callback);
		}
	}

	/**
	 * Set the specified callbacks. This removes all previously set callbacks
	 *
	 * @param callbacks
	 *            callbacks to set
	 */
	public <T extends OnFileSendRequestCallback<F>> void setOnFileSendRequestCallbacks(List<T> callbacks) {
		clearOnFileSendRequestCallbacks();
		registerOnFileSendRequestCallbacks(callbacks);
	}
	/**
	 * Hook for native API to invoke callback methods
	 *
	 * @param friendnumber
	 *            the friend who sent the message
	 * @param message
	 *            the message
	 */
	@SuppressWarnings("unused")
	private void onMessage(int friendnumber, byte[] message) {
		F friend = this.friendlist.getByFriendNumber(friendnumber);
		String messageString = JTox.getByteString(message);

		synchronized (this.onMessageCallbacks) {
			for (OnMessageCallback<F> cb : this.onMessageCallbacks) {
				cb.execute(friend, messageString);
			}
		}
	}

	/**
	 * Add the specified callback
	 *
	 * @param callback
	 *            callback to add
	 */
	public void registerOnMessageCallback(OnMessageCallback<F> callback) {
		this.onMessageCallbacks.add(callback);
	}

	/**
	 * Remove the specified callback
	 *
	 * @param callback
	 *            callback to remove
	 */
	public void unregisterOnMessageCallback(OnMessageCallback<F> callback) {
		this.onMessageCallbacks.remove(callback);
	}

	/**
	 * Remove all callbacks
	 */
	public void clearOnMessageCallbacks() {
		this.onMessageCallbacks.clear();
	}

	/**
	 * Add all specified callbacks
	 *
	 * @param callbacks
	 *            callbacks to add
	 */
	public <T extends OnMessageCallback<F>> void registerOnMessageCallbacks(List<T> callbacks) {
		for (T callback : callbacks) {
			registerOnMessageCallback(callback);
		}
	}

	/**
	 * Set the specified callbacks. This removes all previously set callbacks
	 *
	 * @param callbacks
	 *            callbacks to set
	 */
	public <T extends OnMessageCallback<F>> void setOnMessageCallbacks(List<T> callbacks) {
		clearOnMessageCallbacks();
		registerOnMessageCallbacks(callbacks);
	}

	/**
	 * Hook for native API to invoke callback methods
	 *
	 * @param friendnumber
	 *            friend who changed their name
	 * @param newname
	 *            friend's new name
	 */
	@SuppressWarnings("unused")
	private void onNameChange(int friendnumber, byte[] newname) {
		F friend = this.friendlist.getByFriendNumber(friendnumber);
		String newnameString = JTox.getByteString(newname);

		synchronized (this.onNameChangeCallbacks) {
			for (OnNameChangeCallback<F> cb : this.onNameChangeCallbacks) {
				cb.execute(friend, newnameString);
			}
		}
	}

	/**
	 * Add the specified callback
	 *
	 * @param callback
	 *            callback to add
	 */
	public void registerOnNameChangeCallback(OnNameChangeCallback<F> callback) {
		this.onNameChangeCallbacks.add(callback);
	}

	/**
	 * Remove the specified callback
	 *
	 * @param callback
	 *            callback to remove
	 */
	public void unregisterOnNameChangeCallback(OnNameChangeCallback<F> callback) {
		this.onNameChangeCallbacks.remove(callback);
	}

	/**
	 * Remove all callbacks
	 */
	public void clearOnNameChangeCallbacks() {
		this.onNameChangeCallbacks.clear();
	}

	/**
	 * Add the specified callbacks
	 *
	 * @param callbacks
	 *            callbacks to add
	 */
	public <T extends OnNameChangeCallback<F>> void addOnNameChangeCallbacks(List<T> callbacks) {
		for (T callback : callbacks) {
			registerOnNameChangeCallback(callback);
		}
	}

	/**
	 * Set the specified callbacks. Removes all previously set callbacks
	 *
	 * @param callbacks
	 *            callbacks to set
	 */
	public <T extends OnNameChangeCallback<F>> void setOnNameChangeCallbacks(List<T> callbacks) {
		clearOnNameChangeCallbacks();
		addOnNameChangeCallbacks(callbacks);
	}

	/**
	 * Hook for native API to invoke callback methods
	 *
	 * @param friendnumber
	 *            the friend who sent the receipt
	 * @param receipt
	 *            number of the receipt
	 */
	@SuppressWarnings("unused")
	private void onReadReceipt(int friendnumber, int receipt) {
		F friend = this.friendlist.getByFriendNumber(friendnumber);

		synchronized (this.onReadReceiptCallbacks) {
			for (OnReadReceiptCallback<F> cb : this.onReadReceiptCallbacks) {
				cb.execute(friend, receipt);
			}
		}
	}

	/**
	 * Add the specified callback
	 *
	 * @param callback
	 *            callback to add
	 */
	public void registerOnReadReceiptCallback(OnReadReceiptCallback<F> callback) {
		this.onReadReceiptCallbacks.add(callback);
	}

	/**
	 * Remove the specified callback
	 *
	 * @param callback
	 *            callback to remove
	 */
	public void unregisterOnReadReceiptCallback(OnReadReceiptCallback<F> callback) {
		this.onReadReceiptCallbacks.remove(callback);
	}

	/**
	 * Remove all callbacks
	 */
	public void clearOnReadReceiptCallbacks() {
		this.onReadReceiptCallbacks.clear();
	}

	/**
	 * Add the specified callbacks
	 *
	 * @param callbacks
	 *            callbacks to add
	 */
	public <T extends OnReadReceiptCallback<F>> void registerOnReadReceiptCallbacks(List<T> callbacks) {
		for (T callback : callbacks) {
			registerOnReadReceiptCallback(callback);
		}
	}

	/**
	 * Set the specified callbacks. Remove all previously existing callbacks
	 *
	 * @param callbacks
	 *            callbacks to set
	 */
	public <T extends OnReadReceiptCallback<F>> void setOnReadReceiptcallbacks(List<T> callbacks) {
		clearOnReadReceiptCallbacks();
		registerOnReadReceiptCallbacks(callbacks);
	}

	/**
	 * Hook for native API to invoke callback methods
	 *
	 * @param friendnumber
	 *            the friend who changed their method
	 * @param statusmessage
	 *            the friend's new status message
	 */
	@SuppressWarnings("unused")
	private void onStatusMessage(int friendnumber, byte[] statusmessage) {
		String newStatus = JTox.getByteString(statusmessage);
		F friend = this.friendlist.getByFriendNumber(friendnumber);

		synchronized (this.onStatusMessageCallbacks) {
			for (OnStatusMessageCallback<F> cb : this.onStatusMessageCallbacks) {
				cb.execute(friend, newStatus);
			}
		}
	}

	/**
	 * Add the specified callback
	 *
	 * @param callback
	 *            callback to add
	 */
	public void registerOnStatusMessageCallback(OnStatusMessageCallback<F> callback) {
		this.onStatusMessageCallbacks.add(callback);
	}

	/**
	 * Remove the specified callback
	 *
	 * @param callback
	 *            callback to remove
	 */
	public void unregisterOnStatusMessageCallback(OnStatusMessageCallback<F> callback) {
		this.onStatusMessageCallbacks.remove(callback);
	}

	/**
	 * Remove all callbacks
	 */
	public void clearOnStatusMessageCallbacks() {
		this.onStatusMessageCallbacks.clear();
	}

	/**
	 * Add the specified callbacks
	 *
	 * @param callbacks
	 *            callbacks to add
	 */
	public <T extends OnStatusMessageCallback<F>> void registerOnStatusMessageCallbacks(List<T> callbacks) {
		for (T callback : callbacks) {
			registerOnStatusMessageCallback(callback);
		}
	}

	/**
	 * Set the specified callbacks. All previously set callbacks will be removed
	 *
	 * @param callbacks
	 *            callbacks to set
	 */
	public <T extends OnStatusMessageCallback<F>> void setOnStatusMessageCallbacks(List<T> callbacks) {
		clearOnStatusMessageCallbacks();
		registerOnStatusMessageCallbacks(callbacks);
	}

	/**
	 * Hook for native API to invoke callback methods
	 *
	 * @param friendnumber
	 *            the friend who changed their status
	 * @param status
	 *            the new status
	 */
	@SuppressWarnings("unused")
	private void onUserStatus(int friendnumber, ToxUserStatus status) {
		F friend = this.friendlist.getByFriendNumber(friendnumber);

		synchronized (this.onUserStatusCallbacks) {
			for (OnUserStatusCallback<F> cb : this.onUserStatusCallbacks) {
				cb.execute(friend, status);
			}
		}
	}

	/**
	 * Add the specified callback
	 *
	 * @param callback
	 *            callback to add
	 */
	public void registerOnUserStatusCallback(OnUserStatusCallback<F> callback) {
		this.onUserStatusCallbacks.add(callback);
	}

	/**
	 * Remove the specified callback
	 *
	 * @param callback
	 *            callback to remove
	 */
	public void unregisterOnUserStatusCallbacks(OnUserStatusCallback<F> callback) {
		this.onUserStatusCallbacks.remove(callback);
	}

	/**
	 * Remove all callbacks
	 */
	public void clearOnUserStatusCallbacks() {
		this.onUserStatusCallbacks.clear();
	}

	/**
	 * Add the specified callbacks
	 *
	 * @param callbacks
	 *            callbacks to add
	 */
	public <T extends OnUserStatusCallback<F>> void registerOnUserStatusCallbacks(List<T> callbacks) {
		for (T callback : callbacks) {
			registerOnUserStatusCallback(callback);
		}
	}

	/**
	 * Set the specified callbacks. All previously existing callbacks will be
	 * removed.
	 *
	 * @param callbacks
	 */
	public <T extends OnUserStatusCallback<F>> void setOnUserStatusCallbacks(List<T> callbacks) {
		clearOnUserStatusCallbacks();
		registerOnUserStatusCallbacks(callbacks);
	}

	/**
	 * Hook for native API to invoke callback methods
	 *
	 * @param friendnumber
	 *            the friend who changed their typing status
	 * @param isTyping
	 *            <code>true</code> if the user is typing now, <code>false</code>otherwise
	 */
	@SuppressWarnings("unused")
	private void onTypingChange(int friendnumber, boolean isTyping) {
		F friend = this.friendlist.getByFriendNumber(friendnumber);

		synchronized (this.onTypingChangeCallbacks) {
			for (OnTypingChangeCallback<F> callback : this.onTypingChangeCallbacks) {
				callback.execute(friend, isTyping);
			}
		}
	}

	/**
	 * Add the specified callback
	 *
	 * @param callback
	 *            callback to add
	 */
	public void registerOnTypingChangeCallback(OnTypingChangeCallback<F> callback) {
		this.onTypingChangeCallbacks.add(callback);
	}

	/**
	 * Remove the specified callback
	 * @param callback callback to remove
	 */
	public void unregisterOnTypingChangeCallback(OnTypingChangeCallback<F> callback) {
		this.onTypingChangeCallbacks.remove(callback);
	}

	/**
	 * Remove all callbacks
	 */
	public void clearOnTypingChangeCallbacks() {
		this.onTypingChangeCallbacks.clear();
	}

	/**
	 * Add the specified callbacks
	 * @param callbacks the callbacks to add
	 */
	public <T extends OnTypingChangeCallback<F>> void registerOnTypingChangeCallbacks(List<T> callbacks) {
		for (T callback : callbacks) {
			registerOnTypingChangeCallback(callback);
		}
	}

	/**
	 * Set the specified callbacks. All previously existing callbacks will be removed
	 * @param callbacks the callbacks to set
	 */
	public <T extends OnTypingChangeCallback<F>> void setOnTypingChangeCallbacks(List<T> callbacks) {
		clearOnTypingChangeCallbacks();
		registerOnTypingChangeCallbacks(callbacks);
	}
}
