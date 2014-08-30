package im.tox.jtoxcore.callbacks;

import im.tox.jtoxcore.ToxFriend;

public interface OnAudioDataCallback<F extends ToxFriend> {

	void execute(int callId, byte[] data);
}
