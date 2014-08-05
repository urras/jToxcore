package im.tox.jtoxcore.callbacks;

import im.tox.jtoxcore.ToxFriend;

public interface OnVideoDataCallback<F extends ToxFriend> {

	void execute(int callId, byte[] data, int width, int height);
}
