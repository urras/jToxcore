/* JTox.java
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

package im.tox.jtoxcore;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import im.tox.jtoxcore.callbacks.CallbackHandler;

/**
 * This is the main wrapper class for the tox library. It contains wrapper
 * methods for everything in the public API provided by tox.h
 *
 * @author sonOfRa
 * @param <F>
 *            Friend type for this JTox instance
 */
public class JTox<F extends ToxFriend> {

	/**
	 * Maximum length of a status message in Bytes. Non-ASCII characters take
	 * multiple Bytes.
	 */
	public static final int TOX_MAX_STATUSMESSAGE_LENGTH = 128;

	/**
	 * Maximum length of a nickname in Bytes. Non-ASCII characters take multiple
	 * Bytes.
	 */
	public static final int TOX_MAX_NICKNAME_LENGTH = 128;

	static {
		System.loadLibrary("jtoxcore");
	}

	/**
	 * List containing all currently active tox instances
	 */
	private static List<Long> validPointers = Collections.synchronizedList(new ArrayList<Long>());

	private static Map < Integer, JTox<? >> instances = new HashMap < Integer, JTox<? >> ();
	private static ReentrantLock instanceLock = new ReentrantLock();
	private static int instanceCounter = 0;
	private final int instanceNumber;

	private CallbackHandler<F> handler;
	private FriendList<F> friendList;

	/**
	 * This field contains the lock used for thread safety
	 */
	private final ReentrantLock lock;

	/**
	 * This field contains the pointer used in all native tox_ method calls.
	 */
	private final long messengerPointer;

	private final long avPointer;
	/**
	 * Native call to tox_new
	 *
	 * @return the pointer to the messenger struct on success, 0 on failure
	 */
	private native long tox_new(ToxOptions options);

	/**
	 * Creates a new instance of JTox and stores the pointer to the internal
	 * struct in messengerPointer.
	 *
	 * @param friendList
	 *            the friendlist to use with this instance
	 * @param handler
	 *            the callback handler for this instance
	 * @throws ToxException
	 *             when the native call indicates an error
	 */
	public JTox(FriendList<F> friendList, CallbackHandler<F> handler, ToxOptions toxOptions) throws ToxException {
		this.friendList = friendList;
		this.handler = handler;
		long pointer = tox_new(toxOptions);

		if (pointer == 0) {
			throw new ToxException(ToxError.TOX_UNKNOWN);
		}

		this.messengerPointer = pointer;
		long avPointer = toxav_new(this.messengerPointer, 16);

		if (avPointer == 0) {
			throw new ToxException(ToxError.TOX_UNKNOWN);
		}

		this.avPointer = avPointer;
		this.lock = new ReentrantLock();
		validPointers.add(pointer);
		validPointers.add(avPointer);
		instanceLock.lock();

		try {
			this.instanceNumber = instanceCounter++;
			instances.put(instanceCounter, this);
		} finally {
			instanceLock.unlock();
		}
	}

	/**
	 * Creates a new instance of JTox and stores the pointer to the internal
	 * struct in messengerPointer. Also attempts to load the specified byte
	 * array into this instance.
	 *
	 * @param data
	 *            the data to load for the new tox instance
	 * @param friendList
	 *            friend list to use with this tox instance
	 * @param handler
	 *            callback handler to use with this instance
	 * @throws ToxException
	 *             when the native call indicates an error
	 */
	public JTox(byte[] data, FriendList<F> friendList, CallbackHandler<F> handler, ToxOptions toxOptions) throws ToxException {
		this(friendList, handler, toxOptions);
		this.load(data);
	}

	/**
	 * Native call to tox_get_address
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct.
	 *
	 * @return the client's address on success, null on failure
	 */
	private native String tox_get_address(long messengerPointer);

	/**
	 * Get our own address
	 *
	 * @return our client's address
	 * @throws ToxException
	 *             when the instance has been killed or an error occurred when
	 *             trying to get our address
	 */
	public String getAddress() throws ToxException {
		String address;

		this.lock.lock();

		try {
			checkPointer();
			address = tox_get_address(this.messengerPointer);
		} finally {
			this.lock.unlock();
		}

		if (address == null || address.equals("")) {
			throw new ToxException(ToxError.TOX_UNKNOWN);
		}

		return address;
	}

	/**
	 * Native call to tox_get_self_user_status
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 * @return our current status
	 * @throws ToxException
	 *             if the instance has been killed
	 */
	private native ToxUserStatus tox_get_self_user_status(long messengerPointer);

	/**
	 * Get our own current status
	 *
	 * @return one of {@link ToxUserStatus}
	 * @throws ToxException
	 */
	public ToxUserStatus getSelfUserStatus() throws ToxException {
		this.lock.lock();

		try {
			checkPointer();

			return tox_get_self_user_status(this.messengerPointer);
		} finally {
			this.lock.unlock();
		}
	}

	/**
	 * Native call to tox_set_status_message
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 * @param message
	 *            our new status message
	 * @param length
	 *            the length of the new status message in bytes
	 * @return false on success, true on failure
	 */
	private native boolean tox_set_status_message(long messengerPointer, byte[] message, int length);

	/**
	 * Sets our status message
	 *
	 * @param message
	 *            our new status message
	 * @throws ToxException
	 *             if the instance has been killed, the message was too long, or
	 *             another error occurred
	 */
	public void setStatusMessage(String message) throws ToxException {
		byte[] messageArray = getStringBytes(message);

		if (messageArray.length >= TOX_MAX_STATUSMESSAGE_LENGTH) {
			throw new ToxException(ToxError.TOX_TOOLONG);
		}

		boolean error;

		this.lock.lock();

		try {
			checkPointer();

			error = tox_set_status_message(this.messengerPointer, messageArray, messageArray.length);
		} finally {
			this.lock.unlock();
		}

		if (error) {
			throw new ToxException(ToxError.TOX_UNKNOWN);
		}
	}

	/**
	 * Native call to tox_get_self_name
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 * @return our name
	 */
	private native String tox_get_self_name(long messengerPointer);

	/**
	 * Function to get our current name
	 *
	 * @return our name
	 * @throws ToxException
	 *             if the instance has been killed
	 */
	public String getSelfName() throws ToxException {
		this.lock.lock();
		String name;

		try {
			checkPointer();

			name = tox_get_self_name(this.messengerPointer);
		} finally {
			this.lock.unlock();
		}

		if (name == null) {
			throw new ToxException(ToxError.TOX_UNKNOWN);
		}

		return name;
	}

	/**
	 * Native call to tox_set_name
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 * @param newname
	 *            the new name
	 * @param length
	 *            length of the new name in byte
	 * @return false on success, true on failure
	 */
	private native boolean tox_set_name(long messengerPointer, byte[] newname, int length);

	/**
	 * Sets our nickname
	 *
	 * @param newname
	 *            the new name to set. Maximum length is 128 bytes. This means
	 *            that a name containing UTF-8 characters has a shorter
	 *            character limit than one only using ASCII.
	 * @throws ToxException
	 *             if the instance was killed, the name was too long, or another
	 *             error occurred
	 */
	public void setName(String newname) throws ToxException {
		byte[] newnameArray = getStringBytes(newname);

		if (newnameArray.length >= TOX_MAX_NICKNAME_LENGTH) {
			throw new ToxException(ToxError.TOX_TOOLONG);
		}

		boolean error;

		this.lock.lock();

		try {
			checkPointer();

			error = tox_set_name(this.messengerPointer, newnameArray, newnameArray.length);
		} finally {
			this.lock.unlock();
		}

		if (error) {
			throw new ToxException(ToxError.TOX_UNKNOWN);
		}
	}

	/**
	 * Native call to tox_set_user_status
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 * @param status
	 *            status to set
	 * @return false on success, true on failure
	 */
	private native boolean tox_set_user_status(long messengerPointer, int status);

	/**
	 * Set our current {@link ToxUserStatus}.
	 *
	 * @param status
	 *            the status to set
	 * @throws ToxException
	 *             if the instance was killed, we tried to set our status to
	 *             invalid, or an error occurred while setting status
	 */
	public void setUserStatus(ToxUserStatus status) throws ToxException {
		if (status == ToxUserStatus.TOX_USERSTATUS_INVALID) {
			throw new ToxException(ToxError.TOX_STATUS_INVALID);
		}

		boolean error;

		this.lock.lock();

		try {
			checkPointer();

			error = tox_set_user_status(this.messengerPointer, status.ordinal());
		} finally {
			this.lock.unlock();
		}

		if (error) {
			throw new ToxException(ToxError.TOX_UNKNOWN);
		}
	}

	/**
	 * Native call to tox_add_friend
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 * @param address
	 *            address of the friend
	 * @param data
	 *            optional message sent to friend
	 * @param length
	 *            length of the message sent to friend
	 * @return friend number on success, error code on failure
	 */
	private native int tox_add_friend(long messengerPointer, byte[] address, byte[] data, int length);

	/**
	 * Method used to add a friend. On success, the friend is added to the list
	 * of friends, and a reference to the friend is returned.
	 *
	 * @param address
	 *            the address of the friend you want to add
	 * @param data
	 *            an optional message you want to send to your friend
	 * @return the friend
	 * @throws ToxException
	 *             if the instance has been killed or an error code is returned
	 *             by the native tox_addfriend call
	 * @throws FriendExistsException
	 *             if the friend already exists
	 */
	public F addFriend(String address, String data) throws ToxException, FriendExistsException {
		byte[] dataArray = getStringBytes(data);
		byte[] addressArray = hexToByteArray(address);
		int errcode;
		this.lock.lock();

		try {
			checkPointer();
			errcode = tox_add_friend(this.messengerPointer, addressArray, dataArray, dataArray.length);
		} finally {
			this.lock.unlock();
		}

		return getFriendOrFail(address, errcode);
	}

	/**
	 * Native call to tox_add_friend_norequest
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 * @param address
	 *            the address of the client you want to add
	 * @return the local number of the friend in your list
	 */
	private native int tox_add_friend_norequest(long messengerPointer, byte[] address);

	/**
	 * Confirm a friend request, or add a friend to your own list without
	 * sending them a friend request. If successful, the Friend is added to the
	 * list, and a reference to the friend is returned.
	 *
	 * @param address
	 *            address of the friend to add
	 * @return the friend
	 * @throws ToxException
	 *             if the instance was killed or an error occurred when adding
	 *             the friend
	 * @throws FriendExistsException
	 *             if the friend already exists
	 */
	public F confirmRequest(String address) throws ToxException, FriendExistsException {
		byte[] addressArray = hexToByteArray(address);
		int errcode;
		this.lock.lock();

		try {
			checkPointer();

			errcode = tox_add_friend_norequest(this.messengerPointer, addressArray);
		} finally {
			this.lock.unlock();
		}

		return getFriendOrFail(address, errcode);
	}

	private F getFriendOrFail(String address, int errcode) throws FriendExistsException, ToxException {
		if (errcode >= 0) {
			F friend = this.friendList.addFriend(errcode);
			friend.setId(address);
			return friend;
		}

		throw new ToxException(errcode);
	}

	/**
	 * Native call to tox_del_friend
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 * @param friendnumber
	 *            the number of the friend
	 * @return false on success, true on failure
	 */
	private native boolean tox_del_friend(long messengerPointer, int friendnumber);

	/**
	 * Method used to delete a friend
	 *
	 * @param friendnumber
	 *            the friend to delete
	 * @throws ToxException
	 *             if the instance has been killed or an error occurred
	 */
	public void deleteFriend(int friendnumber) throws ToxException {
		boolean error;
		this.lock.lock();

		try {
			checkPointer();

			error = tox_del_friend(this.messengerPointer, friendnumber);
		} finally {
			this.lock.unlock();
		}

		if (error) {
			throw new ToxException(ToxError.TOX_UNKNOWN);
		}

		this.friendList.removeFriend(friendnumber);
	}

	/**
	 * Native call to tox_send_message
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 * @param friendnumber
	 *            the number of the friend
	 * @param message
	 *            the message
	 * @param length
	 *            length of the message in bytes
	 * @return the message ID on success, 0 on failure
	 */
	private native int tox_send_message(long messengerPointer, int friendnumber, byte[] message, int length);

	/**
	 * Sends a message to the specified friend. Add the message ID of the sent
	 * message to the list of sent messages of the receiving friend.
	 *
	 * @param friend
	 *            the friend
	 * @param message
	 *            the message
	 * @return the message ID of the sent message. This is stored in the
	 *         Friend's list of sent messages.
	 * @throws ToxException
	 *             if the instance has been killed or the message was not sent
	 */
	public int sendMessage(F friend, String message) throws ToxException {
		byte[] messageArray = getStringBytes(message);
		int result;

		this.lock.lock();

		try {
			checkPointer();

			result = tox_send_message(this.messengerPointer, friend.getFriendnumber(), messageArray,
									  messageArray.length);
		} finally {
			this.lock.unlock();
		}

		if (result == 0) {
			throw new ToxException(ToxError.TOX_SEND_FAILED);
		}

		return result;
	}

	/**
	 * Native call to tox_send_message_withid
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 * @param friendnumber
	 *            the number of the friend
	 * @param message
	 *            the message
	 * @param length
	 *            length of the message in bytes
	 *
	 * @param messageID
	 *            the message ID to use
	 * @return the message ID on success, 0 on failure
	 */
	private native int tox_send_message_withid(long messengerPointer, int friendnumber, byte[] message, int length,
			int messageID);

	/**
	 * Sends a message to the specified friend, with a specified ID
	 *
	 * @param friend
	 *            the friend
	 * @param message
	 *            the message
	 * @param messageID
	 *            the message ID to use
	 * @return the message ID of the sent message. If you want to receive read
	 *         receipts, hang on to this value.
	 * @throws ToxException
	 *             if the instance has been killed or the message was not sent.
	 */
	public int sendMessage(F friend, String message, int messageID) throws ToxException {
		byte[] messageArray = getStringBytes(message);
		int result;

		this.lock.lock();

		try {
			checkPointer();

			result = tox_send_message_withid(this.messengerPointer, friend.getFriendnumber(), messageArray,
											 messageArray.length, messageID);
		} finally {
			this.lock.unlock();
		}

		if (result == 0) {
			throw new ToxException(ToxError.TOX_SEND_FAILED);
		}

		return result;
	}

	/**
	 * Native call to tox_send_action
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 * @param friendnumber
	 *            the number of the friend
	 * @param action
	 *            the action to send
	 * @param length
	 *            length of the action in bytes
	 * @return false on success, true on failure
	 */
	private native boolean tox_send_action(long messengerPointer, int friendnumber, byte[] action, int length);

	/**
	 * Sends an IRC-like /me-action to a friend
	 *
	 * @param friend
	 *            the friend
	 * @param action
	 *            the action
	 * @throws ToxException
	 *             if the instance has been killed or the send failed
	 */
	public void sendAction(F friend, String action) throws ToxException {
		byte[] actionArray = getStringBytes(action);
		boolean error;

		this.lock.lock();

		try {
			checkPointer();

			error = tox_send_action(this.messengerPointer, friend.getFriendnumber(), actionArray, actionArray.length);
		} finally {
			this.lock.unlock();
		}

		if (error) {
			throw new ToxException(ToxError.TOX_UNKNOWN);
		}
	}

	/**
	 * Native call to tox_set_user_is_typing
	 * @param messengerPointer pointer to the internal messenger struct
	 * @param friendnumber the friend's number
	 * @param typing <code>true</code> indicates we are typing, <code>false</code> indicates we stopped typing
	 * @return false on success, true on failure
	 */
	private native boolean tox_set_user_is_typing(long messengerPointer, int friendnumber, boolean typing);

	/**
	 * Indicate to the specified friend that we are currently typing
	 * @param friendnumber the friend's number
	 * @param typing <code>true</code> indicates we are typing, <code>false</code> indicates we stopped typing
	 * @throws ToxException if the instance has been killed
	 */
	public void sendIsTyping(int friendnumber, boolean typing) throws ToxException {
		boolean error;

		this.lock.lock();

		try {
			checkPointer();

			error = tox_set_user_is_typing(this.messengerPointer, friendnumber, typing);
		} finally {
			this.lock.unlock();
		}

		if (error) {
			throw new ToxException(ToxError.TOX_UNKNOWN);
		}
	}

	/**
	 * Native call to tox_do
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 */
	private native void tox_do(long messengerPointer);

	/**
	 * The main tox loop that needs to be run at least 20 times per second. When
	 * implementing this, either use it in a main loop to guarantee execution,
	 * or start an asynchronous Thread or Service to do it for you.
	 *
	 * @throws ToxException
	 *             if the instance has been killed
	 */
	public void doTox() throws ToxException {
		this.lock.lock();

		try {
			checkPointer();

			tox_do(this.messengerPointer);
		} finally {
			this.lock.unlock();
		}
	}

	private native int tox_do_interval(long messengerPointer);

	/**
	 * Return the time in milliseconds before doTox() should be called again
	 * for optimal performance.
	 * @return time in ms, -1 on failure
	 * @throws ToxException
	 */
	public int doToxInterval() throws ToxException {
		this.lock.lock();
		int result = -1;

		try {
			checkPointer();

			result = tox_do_interval(this.messengerPointer);
		} finally {
			this.lock.unlock();
		}

		return result;
	}
	/**
	 * Native call to tox_bootstrap_from_address
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 * @param ip
	 *            ip address to bootstrap with
	 * @param port
	 *            port to bootstrap with
	 * @param pubkey
	 *            public key of the bootstrap node
	 */
	private native int tox_bootstrap_from_address(long messengerPointer, String ip, int port, byte[] pubkey);

	/**
	 * Method used to bootstrap the client's connection.
	 *
	 * @param host
	 *            Hostname or IP(v4, v6) address to connect to. If the hostname
	 *            contains non-ASCII characters, convert it to punycode when
	 *            calling this method.
	 * @param port
	 *            port to connect to
	 * @param pubkey
	 *            public key of the bootstrap node
	 * @throws ToxException
	 *             if the instance has been killed or an invalid port was
	 *             specified
	 * @throws UnknownHostException
	 *             if the host could not be resolved or the IP address was
	 *             invalid
	 */
	public void bootstrap(String host, int port, String pubkey) throws ToxException, UnknownHostException {
		byte[] pubkeyArray = hexToByteArray(pubkey);
		boolean error;

		if (port < 0 || port > 65535) {
			throw new ToxException(ToxError.TOX_INVALID_PORT);
		}

		this.lock.lock();

		try {
			checkPointer();

			error = tox_bootstrap_from_address(this.messengerPointer, host, port, pubkeyArray) == 0;
		} finally {
			this.lock.unlock();
		}

		if (error) {
			throw new UnknownHostException(host);
		}
	}

	/**
	 * Native call to tox_isconnected
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 */
	private native int tox_isconnected(long messengerPointer);

	/**
	 * Check if the client is connected to the DHT
	 *
	 * @return true if connected, false otherwise
	 * @throws ToxException
	 *             if the instance has been killed
	 */
	public boolean isConnected() throws ToxException {
		this.lock.lock();

		try {
			checkPointer();

			return tox_isconnected(this.messengerPointer) != 0;
		} finally {
			this.lock.unlock();
		}
	}

	/**
	 * Native call to tox_kill
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 */
	private native void tox_kill(long messengerPointer);

	/**
	 * Kills the current instance, triggering a cleanup of all internal data
	 * structures. All subsequent calls on any method in this class will result
	 * in a {@link ToxException} with {@link ToxError#TOX_KILLED_INSTANCE} as an
	 * error code.
	 *
	 * @throws ToxException
	 *             in case the instance has already been killed
	 */
	public void killTox() throws ToxException {
		this.lock.lock();

		try {
			checkPointer();

			validPointers.remove(this.messengerPointer);
			tox_kill(this.messengerPointer);

			checkAvPointer();
			validPointers.remove(this.avPointer);
			toxav_kill(this.avPointer);
		} finally {
			this.lock.unlock();
		}

		instances.remove(this.instanceNumber);
	}

	/**
	 * Native call to tox_save
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 * @return a byte array containing the saved data
	 */
	private native byte[] tox_save(long messengerPointer);

	/**
	 * Save the internal messenger data to a byte array, which can be saved to a
	 * file or database
	 *
	 * @return a byte array containing the saved data
	 * @throws ToxException
	 *             if the instance has been killed
	 */
	public byte[] save() throws ToxException {
		this.lock.lock();

		try {
			checkPointer();

			return tox_save(this.messengerPointer);
		} finally {
			this.lock.unlock();
		}
	}

	/**
	 * Native call to tox_load
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 * @param data
	 *            a byte array containing the data to load
	 * @param length
	 *            the length of the byte array
	 * @return false on success, true on failure
	 */
	private native boolean tox_load(long messengerPointer, byte[] data, int length);

	/**
	 * Load the specified data into this tox instance.
	 *
	 * @param data
	 *            a byte array containing the data to load
	 * @throws ToxException
	 *             if the instance has been killed, or an error occurred while
	 *             loading
	 */
	private void load(byte[] data) throws ToxException {
		boolean error;

		this.lock.lock();

		try {
			checkPointer();

			error = tox_load(this.messengerPointer, data, data.length);
			refreshList();
		} finally {
			this.lock.unlock();
		}

		if (error) {
			throw new ToxException(ToxError.TOX_UNKNOWN);
		}
	}

	/**
	 * Refresh the friend list, looking for new friends, status changes, name
	 * changes etc. Generally, the core should keep this
	 *
	 * @throws ToxException
	 *             if the instance was killed, or an internal error occured
	 */
	public void refreshList() throws ToxException {
		for (int i : getInternalFriendList()) {
			this.friendList.addFriendIfNotExists(i);
			refreshClientId(i);
			refreshFriendName(i);
			refreshStatusMessage(i);
			refreshUserStatus(i);
			refreshFriendConnectionStatus(i);
		}
	}

	/**
	 * Native call to tox_get_friendlist
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 * @return array containing valid friend numbers
	 */
	private native int[] tox_get_friendlist(long messengerPointer);

	/**
	 * Get a list of all currently valid friend numbers.
	 *
	 * @return ArrayList containing the
	 * @throws ToxException
	 *             if the instance was killed, or the list was not retrieved
	 */
	private int[] getInternalFriendList() throws ToxException {
		int[] ids;

		this.lock.lock();

		try {
			checkPointer();

			ids = tox_get_friendlist(this.messengerPointer);
		} finally {
			this.lock.unlock();
		}

		if (ids == null) {
			throw new ToxException(ToxError.TOX_UNKNOWN);
		}

		return ids;
	}

	/**
	 * Native call to tox_get_client_id
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 * @param friendnumber
	 *            local number of the friend
	 * @return the public key of the specified friend
	 */
	private native String tox_get_client_id(long messengerPointer, int friendnumber);

	/**
	 * Refresh the client ID for a given friend.
	 *
	 * @param friendnumber
	 *            the friendnumber
	 * @throws ToxException
	 *             if the instance has been killed, or an error occurred when
	 *             attempting to fetch the client id
	 */
	public void refreshClientId(int friendnumber) throws ToxException {
		String result;
		this.lock.lock();

		try {
			checkPointer();

			result = tox_get_client_id(this.messengerPointer, friendnumber);
		} finally {
			this.lock.unlock();
		}

		if (result == null || result.equals("")) {
			throw new ToxException(ToxError.TOX_UNKNOWN);
		}

		this.friendList.getByFriendNumber(friendnumber).setId(result);
	}

	/**
	 * Native call to tox_get_friend_connection_status
	 *
	 * @param friendnumber
	 *            the friend's number
	 * @return connecting status of a friend
	 */
	private native int tox_get_friend_connection_status(long messengerPointer, int friendnumber);

	/**
	 * Refresh the connection status for a given friend
	 *
	 * @param friendnumber
	 *            the friendnumber
	 * @throws ToxException
	 *             if the instance has been killed, or an error occurred when
	 *             attempting to fetch the connection status
	 */
	public void refreshFriendConnectionStatus(int friendnumber) throws ToxException {
		this.lock.lock();
		int result;

		try {
			checkPointer();
			result = tox_get_friend_connection_status(this.messengerPointer, friendnumber);
		} finally {
			this.lock.unlock();
		}

		if (result == -1) {
			throw new ToxException(ToxError.TOX_UNKNOWN);
		}

		F friend = this.friendList.getByFriendNumber(friendnumber);

		if (result == 0) {
			friend.setOnline(false);
		} else {
			friend.setOnline(true);
		}
	}

	/**
	 * Checks if there exists a friend with given friendnumber.
	 *
	 * @param friendnumber
	 *            the friendnumber
	 * @throws ToxException
	 *             if the instance has been killed, or an error occurred when
	 *             attempting to fetch the connection status
	 */
	private native boolean tox_get_friend_exists(long messengerPointer, int friendnumber);

	/**
	 * Check whether a friend with the given friendnumber exists
	 *
	 * @param friendnumber
	 *            the friendnumber
	 */
	public boolean toxFriendExists(int friendnumber) throws ToxException {
		boolean exists;

		this.lock.lock();

		try {
			checkPointer();
			exists = tox_get_friend_exists(this.messengerPointer, friendnumber);
		} finally {
			this.lock.unlock();
		}

		return exists;
	}

	/**
	 * Native call to tox_get_name
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 * @param friendnumber
	 *            the friend's number
	 * @return the specified friend's name
	 */
	private native byte[] tox_get_name(long messengerPointer, int friendnumber);

	/**
	 * Refresh the specified friend's name
	 *
	 * @param friendnumber
	 *            the friend's number
	 * @throws ToxException
	 *             if the instance has been killed or an error occurred
	 */
	public void refreshFriendName(int friendnumber) throws ToxException {
		byte[] name;

		this.lock.lock();

		try {
			checkPointer();

			name = tox_get_name(this.messengerPointer, friendnumber);
		} finally {
			this.lock.unlock();
		}

		if (name == null) {
			throw new ToxException(ToxError.TOX_UNKNOWN);
		}

		this.friendList.getByFriendNumber(friendnumber).setName(getByteString(name));
	}

	/**
	 * Native call to tox_get_status_message
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 * @param friendnumber
	 *            the friend's number
	 * @return the status message
	 */
	private native byte[] tox_get_status_message(long messengerPointer, int friendnumber);

	/**
	 * Refresh the friend's status message.
	 *
	 * @param friendnumber
	 *            the friend's number
	 * @throws ToxException
	 *             if the instance has been killed, or an error occurred while
	 *             getting the status message
	 */
	public void refreshStatusMessage(int friendnumber) throws ToxException {
		byte[] status;

		this.lock.lock();

		try {
			checkPointer();

			status = tox_get_status_message(this.messengerPointer, friendnumber);
		} finally {
			this.lock.unlock();
		}

		if (status == null) {
			throw new ToxException(ToxError.TOX_UNKNOWN);
		}

		this.friendList.getByFriendNumber(friendnumber).setStatusMessage(getByteString(status));
	}

	/**
	 * Native call to tox_get_userstatus
	 *
	 * @param messengerPointer
	 *            pointer to the internal messenger struct
	 * @param friendnumber
	 *            the friend's number
	 * @return the friend's status
	 */
	private native ToxUserStatus tox_get_user_status(long messengerPointer, int friendnumber);

	/**
	 * Refresh status for the specified friend
	 *
	 * @param friendnumber
	 *            the friend's number
	 * @throws ToxException
	 *             if the instance has been killed
	 */
	public void refreshUserStatus(int friendnumber) throws ToxException {
		ToxUserStatus status = ToxUserStatus.TOX_USERSTATUS_INVALID;

		this.lock.lock();

		try {
			checkPointer();

			status = tox_get_user_status(this.messengerPointer, friendnumber);
		} finally {
			this.lock.unlock();
		}

		this.friendList.getByFriendNumber(friendnumber).setStatus(status);
	}

	/**
	 * Native call to tox_get_is_typing
	 * @param messengerPointer pointer to the internal messenger struct
	 * @param friendnumber the friend's number
	 * @return <code>true</code> if the friend is typing, <code>false</code> otherwise
	 */
	private native boolean tox_get_is_typing(long messengerPointer, int friendnumber);

	/**
	 * Refresh the typing status for the specified friend
	 * @param friendnumber the friend's number
	 * @throws ToxException if the instance has been killed
	 */
	public void refreshTypingStatus(int friendnumber) throws ToxException {
		boolean result;
		this.lock.lock();

		try {
			checkPointer();

			result = tox_get_is_typing(this.messengerPointer, friendnumber);
		} finally {
			this.lock.unlock();
		}

		this.friendList.getByFriendNumber(friendnumber).setTyping(result);
	}

	/****** GROUP CHAT FUNCTIONS ******/

	/**
	 * Creates a new groupchat and puts it in the chats array.
	 * @param messengerPointer pointer to the internal messenger struct
	 * @return group number on success, -1 on failure
	 *
	private native int tox_add_groupchat(long messengerPointer);

	/**
	 * Delete a groupchat from the chats array.
	 * @param messengerPointer pointer to the internal messenger struct
	 * @param groupnumber group to delete
	 * @return false on success, true on failure
	 *
	private native boolean tox_del_groupchat(long messengerPointer, int groupnumber);

	/**
	 * Return the name of peernumber who is in groupnumber
	 * @param messengerPointer pointer to the internal messenger struct
	 * @param groupnumber
	 * @param peernumber
	 * @return the name of the peer as a byte array
	 *
	private native byte[] tox_group_peername(long messengerPointer, int groupnumber, int peernumber);

	/**
	 * Invite friendnumber to groupnumber
	 * @param messengerPointer pointer to the internal messenger struct
	 * @param friendnumber
	 * @param groupnumber
	 * @return false on success, true on failure
	 *
	private native boolean tox_invite_friend(long messengerPointer, int friendnumber, int groupnumber);

	/**
	 * Join a group (you need to have been invited first.)
	 * @param messengerPointer pointer to the internal messenger struct
	 * @param friendnumber
	 * @param friend_group_public_key
	 * @return group number on success, -1 on failure
	 *
	private native int tox_join_groupchat(long messengerPointer, int friendnumber,
	                                      byte[] friend_group_public_key);

	/**
	 * Sends a group message
	 * @param messengerPointer pointer to the internal messenger struct
	 * @param groupnumber
	 * @param message
	 * @param length
	 * @return false on success, true on failure
	 *
	private native boolean tox_group_message_send(long messengerPointer, int groupnumber,
	                                              byte[] message, int length);

	/**
	 * Send a group action
	 * @param messengerPointer
	 * @param groupnumber
	 * @param action
	 * @param length
	 * @return false on success, true on failure
	 *
	private native boolean tox_group_action_send(long messengerPointer, int groupnumber,
	                                         byte[] action, int length);

	/**
	 * Return the number of peers in the group chat on success.
	 * @param messengerPointer
	 * @param groupnumber
	 * @return the number of peers in the group chat on success, -1 on failure
	 *
	private native int tox_group_number_peers(long messengerPointer, int groupnumber);

	*
	private native byte[][] tox_group_get_names(long messengerPointer, int groupnumber);

	public byte[][] toxGroupGetNames(int groupnumber) throws ToxException {
	    byte[][] result;
	    this.lock.lock();
	    try {
	        checkPointer();
	        result = tox_group_get_names(this.messengerPointer, groupnumber);
	    } finally {
	        this.lock.unlock();
	    }
	    return result;
	}
	*/

	/****** GROUP CHAT FUNCTIONS END ******/

	private native int tox_get_nospam(long messengerPointer);

	/**
	 * Get your nospam
	 * @return nospam
	 * @throws ToxException
	 */
	public int getNospam() throws ToxException {
		int result;
		this.lock.lock();

		try {
			checkPointer();
			result = tox_get_nospam(this.messengerPointer);
		} finally {
			this.lock.unlock();
		}

		return result;
	}

	private native void tox_set_nospam(long messengerPointer, int nospam);

	/**
	 * Set your no spam
	 * @param nospam
	 * @throws ToxException
	 */
	public void setNospam(int nospam) throws ToxException {
		this.lock.lock();

		try {
			checkPointer();
			tox_set_nospam(this.messengerPointer, nospam);
		} finally {
			this.lock.unlock();
		}
	}

	/****** FILE SENDING FUNCTIONS BEGIN ******/
	private native int tox_new_file_sender(long messengerPointer, int friendnumber, long filesize, byte[] filename,
										   int length);

	/**
	 * Send a file send request.
	 * Maximum filename length is 255 bytes
	 * @param friendnumber
	 * @param filesize
	 * @param filename
	 * @return filenumber on success, -1 on failure
	 * @throws ToxException
	 */
	public int newFileSender(int friendnumber, long filesize, String filename) throws ToxException {
		int result;
		byte[] _filename = filename.getBytes(Charset.forName("UTF-8"));
		this.lock.lock();

		try {
			checkPointer();
			result = tox_new_file_sender(this.messengerPointer, friendnumber, filesize, _filename, _filename.length);
		} finally {
			this.lock.unlock();
		}

		return result;
	}

	private native int tox_file_send_control(long messengerPointer, int friendnumber, int send_receive, int filenumber,
			int message_id, byte[] data, int length);

	/**
	 * Send a file control request.
	 * sending is true if we want the control packet to target a file we are currently sending,
	 * false if it targets a file we are currently receiving.
	 * @param friendnumber
	 * @param sending
	 * @param filenumber
	 * @param message_id
	 * @param data
	 * @return 0 on success, -1 on failure
	 * @throws ToxException
	 */
	public int fileSendControl(int friendnumber, boolean sending, int filenumber, int message_id,
							   byte[] data) throws ToxException {
		int result;
		int send_receive;

		if (sending) {
			send_receive = 0;
		} else {
			send_receive = 1;
		}

		this.lock.lock();

		try {
			checkPointer();
			result = tox_file_send_control(this.messengerPointer, friendnumber, send_receive, filenumber, message_id, data,
										   data.length);
		} finally {
			this.lock.unlock();
		}

		return result;
	}

	private native int tox_file_send_data(long messengerPointer, int friendnumber, int filenumber, byte[] data, int length);

	/**
	 * Send file data
	 * @param friendnumber
	 * @param filenumber
	 * @param data
	 * @return 0 on success, -1 on failure
	 * @throws ToxException
	 */
	public int fileSendData(int friendnumber, int filenumber, byte[] data) throws ToxException {
		int result;
		this.lock.lock();

		try {
			checkPointer();
			result = tox_file_send_data(this.messengerPointer, friendnumber, filenumber, data, data.length);
		} finally {
			this.lock.unlock();
		}

		return result;
	}

	private native int tox_file_data_size(long messengerPointer, int friendnumber);

	/**
	 * Give the number of bytes left to be sent/received.
	 * @param friendnumber
	 * @return size in bytes on success, -1 on failure
	 * @throws ToxException
	 */
	public int fileDataSize(int friendnumber) throws ToxException {
		int result;
		this.lock.lock();

		try {
			checkPointer();
			result = tox_file_data_size(this.messengerPointer, friendnumber);
		} finally {
			this.lock.unlock();
		}

		return result;
	}

	private native long tox_file_data_remaining(long messengerPointer, int friendnumber, int filenumber, int send_receive);

	/**
	 * Give the number of bytes left to be sent/received.
	 * sending is true if we want a file we are sending, false if we want one we are receiving
	 * @param friendnumber
	 * @param filenumber
	 * @param sending
	 * @return number of bytes left on success, 0 on failure
	 * @throws ToxException
	 */
	public long fileDataRemaining(int friendnumber, int filenumber, boolean sending) throws ToxException {
		long result;
		int send_receive;

		if (sending) {
			send_receive = 0;
		} else {
			send_receive = 1;
		}

		this.lock.lock();

		try {
			checkPointer();
			result = tox_file_data_remaining(this.messengerPointer, friendnumber, filenumber, send_receive);
		} finally {
			this.lock.unlock();
		}

		return result;
	}
	/****** FILE SENDING FUNCTIONS END ******/


	/**
	 * Utility method that checks the current pointer and throws an exception if
	 * it is not valid
	 *
	 * @throws ToxException
	 *             if the instance has been killed
	 */
	private void checkPointer() throws ToxException {
		if (!validPointers.contains(this.messengerPointer)) {
			throw new ToxException(ToxError.TOX_KILLED_INSTANCE);
		}
	}

	private void checkAvPointer() throws ToxException {
		if (!validPointers.contains(this.avPointer)) {
			throw new ToxException(ToxError.TOX_KILLED_INSTANCE);
		}
	}
	/**
	 * If you need to pass a JTox instance around between different contexts,
	 * and are unable to pass instances directly, use this method to acquire the
	 * instance number of this instance. Once acquired, you can acquire the
	 * instance with {@link JTox#getInstance(int)}.
	 *
	 * @return the instance number
	 */
	public int getInstanceNumber() {
		return this.instanceNumber;
	}

	/**
	 * @return the friendlist
	 */
	public FriendList<F> getFriendList() {
		return this.friendList;
	}

	/**
	 * Get the instance associated with the specified instance number. This may
	 * return <code>null</code> if either the instance was killed, or if no
	 * instance with that number exists.
	 *
	 * @param instancenumber
	 * @return the associated instance
	 */
	public static JTox<?> getInstance(int instancenumber) {
		return instances.get(instancenumber);
	}

	/**
	 * Turns the given String into an array of UTF-8 encoded bytes, also adding
	 * a nullbyte at the end for convenience
	 *
	 * @param in
	 *            the String to convert
	 * @return a byte array
	 */
	public static byte[] getStringBytes(String in) {
		return (in + '\000').getBytes(Charset.forName("UTF-8"));
	}

	/**
	 * Turns the given byte array into a UTF-8 encoded string
	 *
	 * @param in
	 *            the byte array to convert
	 * @return an UTF-8 String based on the given byte array
	 */
	public static String getByteString(byte[] in) {
		try {
			return new String(in, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new InternalError("UTF-8 support is needed to continue.");
		}
	}

	/**
	 * Convert a given hexadecimal String to a byte array.
	 *
	 * @param in
	 *            String to convert
	 * @return byte array representation of the hexadecimal String
	 */
	public static byte[] hexToByteArray(String in) {
		int length = in.length();
		byte[] out = new byte[length / 2];

		for (int i = 0; i < length; i += 2) {
			out[i / 2] = (byte) ((Character.digit(in.charAt(i), 16) << 4) + Character.digit(in.charAt(i + 1), 16));
		}

		return out;
	}
	///////////////////////AUDIO / VIDEO///////////////////////////////////////////////
	/**
	 * Native call to toxav_new
	 * Start new a/v session, There can only be one session at the time. If you register more
	 * it will result in undefined behaviour.
	 *
	 * @param messengerPointer messenger pointer
	 * @param max_calls max number of calls
	 * @return the pointer to the av session struct on success, null on failure
	 */
	private native long toxav_new(long messengerPointer, int max_calls);

	/**
	* Remove A/V session.
	*
	* @param avPointer av handler pointer
	* @return void
	*/
	private native void toxav_kill(long avPointer);

	/**
	* Call user. Use its friend_id.
	*
	* @param av Handler.
	* @param call_index Call index
	* @param user The user.
	* @param csettings Codec settings
	* @param ringing_seconds Ringing timeout.
	* @return int, 0 on success
	*/
	private native int toxav_call(long avPointer, int user, ToxCodecSettings csettings, int ringing_seconds);

	/**
	 * Call user using friend_id
	 * @param user friend_id of the user
	 * @param csettings codec settings
	 * @param ringingSeconds seconds to ring
	 * @return 0 on success, otherwise error value
	 * @throws ToxException
	 */
	public int avCall(int user, ToxCodecSettings csettings, int ringingSeconds) throws ToxException {
		this.lock.lock();
		int ret;

		try {
			checkPointer();
			ret = toxav_call(this.avPointer, user, csettings, ringingSeconds);
		} finally {
			this.lock.unlock();
		}

		return ret;
	}

	/**
	* Hangup active call.
	*
	* @param av Handler.
	* @return 0 on success
	*/
	private native int toxav_hangup(long avPointer, int call_index);

	/**
	 * Hangup active call
	 * @param callIndex
	 * @return 0 on success
	 * @throws ToxException
	 */
	public int avHangup(int callIndex) throws ToxException {
		this.lock.lock();
		int ret;

		try {
			checkPointer();
			ret = toxav_hangup(this.avPointer, callIndex);
		} finally {
			this.lock.unlock();
		}

		return ret;
	}

	/**
	* Answer incoming call.
	*
	* @param av Handler.
	* @param call_index call index
    * @param csettings codec settings
	* @return 0 on success
	*/
	private native int toxav_answer(long avPointer, int call_index, ToxCodecSettings csettings );

	/**
	 * Answer incoming call
	 * @param callIndex call index
	 * @param csettings codec settings
	 * @return 0 on success
	 * @throws ToxException
	 */
	public int avAnswer(int callIndex, ToxCodecSettings csettings) throws ToxException {
		this.lock.lock();
		int ret;

		try {
			checkPointer();
			ret = toxav_answer(this.avPointer, callIndex, csettings);
		} finally {
			this.lock.unlock();
		}

		return ret;
	}

	/**
	* Reject incomming call.
	*
	* @param av Handler.
    * @param call_index call index
	* @param reason Optional reason. Set NULL if none.
	* @return int
	*/
	private native int toxav_reject(long avPointer, int call_index, String reason);

	/**
	 * Reject incoming call
	 * @param callIndex
	 * @param reason
	 * @return 0 on success
	 * @throws ToxException
	 */
	public int avReject(int callIndex, String reason) throws ToxException {
		this.lock.lock();
		int ret;

		try {
			checkPointer();
			ret = toxav_reject(this.avPointer, callIndex, reason);
		} finally {
			this.lock.unlock();
		}

		return ret;
	}

	/**
	* Cancel outgoing request.
	*
	* @param av Handler.
    * @param call_index call index
	* @param reason Optional reason.
	* @param peer_id peer friend_id
	* @return 0 on success
	*/
	private native int toxav_cancel(long avPointer, int call_index, int peer_id, String reason);

	/**
    * Cancel outgoing request
    * @param callIndex
    * @param peerId
    * @param reason
    * @return 0 on success
    * @throws ToxException
    */
	public int avCancel(int callIndex, int peerId, String reason) throws ToxException {
		this.lock.lock();
		int ret;

		try {
			checkPointer();
			ret = toxav_cancel(this.avPointer, callIndex, peerId, reason);
		} finally {
			this.lock.unlock();
		}

		return ret;
	}

	/**
	* Notify peer that we are changing call settings
	*
	* @param av Handler.
    * @param call_index call index
    * @param csettings codec settings
	* @return 0 on success
	*/
	private native int toxav_change_settings(long avPointer, int call_index, ToxCodecSettings csettings);

	/**
	 * Change call settings
	 * @param callIndex
	 * @param csettings
	 * @return 0 on success
	 * @throws ToxException
	 */
	public int avChangeSettings(int callIndex, ToxCodecSettings csettings) throws ToxException {
		this.lock.lock();
		int ret;

		try {
			checkPointer();
			ret = toxav_change_settings(this.avPointer, callIndex, csettings);
		} finally {
			this.lock.unlock();
		}

		return ret;
	}

	/**
	* Terminate transmission. Note that transmission will be terminated without informing remote peer.
	*
	* @param av Handler.
    * @param call_index call index
	* @return 0 on success
	*/
	private native int toxav_stop_call(long avPointer, int call_index);

	/**
	 * Terminate transmission, without informing remote peer.
	 * @param callIndex
	 * @return 0 on success
	 * @throws ToxException
	 */
	public int avStopCall(int callIndex) throws ToxException {
		this.lock.lock();
		int ret;

		try {
			checkPointer();
			ret = toxav_stop_call(this.avPointer, callIndex);
		} finally {
			this.lock.unlock();
		}

		return ret;
	}

	/**
	* Must be call before any RTP transmission occurs.
	*
	* @param av Handler.
    * @param call_index call index
    * @param jbuf_size buffer size
    * @param VAD_treshold VAD threshold
	* @param support_video Is video supported ? 1 : 0
	* @return 0 on success
	*/
	private native int toxav_prepare_transmission(long avPointer, int call_index, int jbuf_size,
			int VAD_treshold, int support_video);

	/**
	 * Prepare call for RTP transmission
	 * @param callIndex
	 * @param jBufSize
	 * @param VADThreshold
	 * @param supportVideo
	 * @return 0 on success
	 * @throws ToxException
	 */
	public int avPrepareTransmission(int callIndex, int jBufSize, int VADThreshold, boolean supportVideo) throws ToxException {
		this.lock.lock();
		int ret;
		int s;

		if (supportVideo) {
			s = 1;
		} else {
			s = 0;
		}

		try {
			checkPointer();
			ret = toxav_prepare_transmission(this.avPointer, callIndex, jBufSize, VADThreshold, s);
		} finally {
			this.lock.unlock();
		}

		return ret;
	}

	/**
	* Call this at the end of the transmission.
	*
	* @param av Handler.
    * @param call_index call index
	* @return 0 on success
	*/
	private native int toxav_kill_transmission(long avPointer, int call_index);

	/**
	 * Call this at the end of the transmission
	 * @param callIndex
	 * @return 0 on success
	 * @throws ToxException
	 */
	public int avKillTransmission(int callIndex) throws ToxException {
		this.lock.lock();
		int ret;

		try {
			checkPointer();
			ret = toxav_kill_transmission(this.avPointer, callIndex);
		} finally {
			this.lock.unlock();
		}

		return ret;
	}

	/**
	* Encode and send video packet.
	*
	* @param av Handler.
    * @param call_index call index
	* @param frame The encoded frame.
	* @param frame_size The size of the encoded frame.
	* @return 0 on success
	*/
	private native int toxav_send_video (long avPointer, int call_index, byte[] frame, int frame_size);

	/**
	 * Send video frame
	 * @param callIndex
	 * @param frame
	 * @return 0 on success
	 * @throws ToxException
	 */
	public int avSendVideo(int callIndex, byte[] frame) throws ToxException {
		this.lock.lock();
		int ret;

		try {
			checkPointer();
			ret = toxav_send_video(this.avPointer, callIndex, frame, frame.length);
		} finally {
			this.lock.unlock();
		}

		return ret;
	}

	/**
	* Send audio frame.
	*
	* @param av Handler.
    * @param call_index call index
	* @param frame The frame (raw 16 bit signed pcm with AUDIO_CHANNELS channels audio.)
	* @param frame_size Its size in number of frames/samples (one frame/sample is 16 bits or 2 bytes)
	* frame size should be AUDIO_FRAME_SIZE.
	* @return 0 on success
	*/
	private native int toxav_send_audio (long avPointer, int call_index, byte[] frame, int frame_size);

	/**
	 * Send audio frame
	 * @param callIndex
	 * @param frame
	 * @return 0 on success
	 * @throws ToxException
	 */
	public int avSendAudio(int callIndex, byte[] frame) throws ToxException {
		this.lock.lock();
		int ret;

		try {
			checkPointer();
			ret = toxav_send_audio(this.avPointer, callIndex, frame, frame.length);
		} finally {
			this.lock.unlock();
		}

		return ret;
	}

	/**
	* Encode video frame
	*
	* @param av Handler
    * @param call_index call index
	* @param dest_max Max size
	* @param data What to encode
    * @param width width
    * @param height height
	* @return byte array on success, null on fail
	*/
	private native byte[] toxav_prepare_video_frame (long avPointer, int call_index, int dest_max, byte[] data, int width, int height);

	/**
	 * Encode video frame
	 * @param callIndex
	 * @param destMax
	 * @param data
	 * @param width
	 * @param height
	 * @return The encoded video frame
	 * @throws ToxException
	 */
	public byte[] avPrepareVideoFrame(int callIndex, int destMax, byte[] data, int width, int height) throws ToxException {
		this.lock.lock();
		byte[] ret;

		try {
			checkPointer();
			ret = toxav_prepare_video_frame(this.avPointer, callIndex, destMax, data, width, height);
		} finally {
			this.lock.unlock();
		}

		return ret;
	}

	/**
	* Encode audio frame
	*
	* @param av Handler
    * @param call_index call index
	* @param dest_max Max dest size
	* @param frame The frame
	* @param frame_size The frame size
	* @return byte array on success, else null
	*/
	private native byte[] toxav_prepare_audio_frame (long avPointer, int call_index, int dest_max, int[] frame, int frame_size);

	/**
	 * Encode audio frame
	 * @param callIndex
	 * @param destMax
	 * @param data
	 * @param frameSize
	 * @return The encoded audio frame
	 * @throws ToxException
	 */
	public byte[] avPrepareAudioFrame(int callIndex, int destMax, int[] data, int frameSize) throws ToxException {
		this.lock.lock();
		byte[] ret;

		try {
			checkPointer();
			ret = toxav_prepare_audio_frame(this.avPointer, callIndex, destMax, data, frameSize);
		} finally {
			this.lock.unlock();
		}

		return ret;
	}

	/**
	* Get peer transmission type. It can either be audio or video.
	*
	* @param av Handler.
    * @param call_index call index
	* @param peer The peer
	* @return codec settings on success
	*/
	private native ToxCodecSettings toxav_get_peer_csettings (long avPointer, int call_index, int peer);

	/**
	 * Get peer transmission type, either audio or video
	 * @param callIndex
	 * @param peer
	 * @return codec settings
	 * @throws ToxException
	 */
	public ToxCodecSettings avGetPeerCodecSettings(int callIndex, int peer) throws ToxException {
		this.lock.lock();
		ToxCodecSettings ret;

		try {
			checkPointer();
			ret = toxav_get_peer_csettings(this.avPointer, callIndex, peer);
		} finally {
			this.lock.unlock();
		}

		return ret;
	}

	/**
	* Get id of peer participating in conversation
	*
	* @param av Handler
    * @param call_index call index
	* @param peer peer index
	* @return peer id
	*/
	private native int toxav_get_peer_id (long avPointer, int call_index, int peer );

	/**
	 * Get id of peer in conversation
	 * @param callIndex
	 * @param peer
	 * @return peer id
	 * @throws ToxException
	 */
	public int avGetPeerId(int callIndex, int peer) throws ToxException {
		this.lock.lock();
		int ret;

		try {
			checkPointer();
			ret = toxav_get_peer_id(this.avPointer, callIndex, peer);
		} finally {
			this.lock.unlock();
		}

		return ret;
	}

	/**
	* Get current call state
	*
	* @param av Handler
	* @param call_index What call
	* @return ToxAvCallState
	*/
	private native ToxAvCallState toxav_get_call_state (long avPointer, int call_index );

	/**
	 * Get call state
	 * @param callIndex
	 * @return call state
	 * @throws ToxException
	 */
	public ToxAvCallState avGetCallState(int callIndex) throws ToxException {
		this.lock.lock();
		ToxAvCallState ret;

		try {
			checkPointer();
			ret = toxav_get_call_state(this.avPointer, callIndex);
		} finally {
			this.lock.unlock();
		}

		return ret;
	}
	/**
	* Is certain capability supported
	*
	* @param av Handler
	* @param call_index call index
	* @return 1 yes, 0 no
	*/
	private native int toxav_capability_supported (long avPointer, int call_index, ToxAvCapabilities capability );

	/**
	 * Check for certain av capability
	 * @param callIndex
	 * @param capability
	 * @return True if exists, otherwise False
	 * @throws ToxException
	 */
	public boolean avCapabilitySupported(int callIndex, ToxAvCapabilities capability) throws ToxException {
		this.lock.lock();
		int ret;

		try {
			checkPointer();
			ret = toxav_capability_supported(this.avPointer, callIndex, capability);
		} finally {
			this.lock.unlock();
		}

		return ret == 1;
	}

	/* not implemented
	    private native long toxav_get_tox(long avPointer);

	    private native int toxav_has_activity (long avPointer, int call_index, int[] PCM, int frame_size, float ref_energy );
	    */
}
