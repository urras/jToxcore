/**
 * This package supplies the base classes needed for using the Tox library in java.
 *
 * <p>The main class is {@link im.tox.jtoxcore.JTox}, which enables the creation of native tox instances,
 * and all interaction with these.
 * <p>You may notice that many methods are private. This applies especially to the methods that get status from friends.
 * Getting the most recent friend status should be done through the {@link im.tox.jtoxcore.ToxFriend} interface. When
 * this interface is implemented correctly, the {@link im.tox.jtoxcore.FriendList} will be automatically kept updated
 * by the core functions.
 */
package im.tox.jtoxcore;
