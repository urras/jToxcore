package im.tox.jtoxcore.callbacks;

import im.tox.jtoxcore.ToxFriend;
import im.tox.jtoxcore.ToxAvCallbackID;

public interface OnAvCallbackCallback<F extends ToxFriend> {

    void execute(int callId, ToxAvCallbackID callbackId);
}
