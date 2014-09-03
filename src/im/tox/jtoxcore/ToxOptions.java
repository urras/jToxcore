/* ToxOptions.java
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

/**
 * ToxOptions is used in the JTox constructors to define how certain network functions should work.
 */
public class ToxOptions {
    private boolean ipv6Enabled;
    private boolean udpEnabled;
    private boolean proxyEnabled;
    private String proxyAddress;
    private int port;

    /**
     * Create a new Tox Options with necessary settings
     * @param ipv6 - Enable or Disable ipv6
     * @param udp - Enable or Disable UDP (Disabling will enable TCP only)
     * @param proxy - Enable or Disable Proxy
     */
    public ToxOptions(boolean ipv6, boolean udp, boolean proxy) {
        this.ipv6Enabled = ipv6;
        this.udpEnabled = udp;
        this.proxyEnabled = proxy;
    }

    /**
     * Setter for ipv6Enabled
     * @param enabled
     */
    public void setIpv6Enabled(boolean enabled) {
        ipv6Enabled = enabled;
    }

    /**
     * Setter for udpEnabled
     * @param enabled
     */
    public void setUdpEnabled(boolean enabled) {
        udpEnabled = enabled;
    }

    /**
     * Setter for proxyEnabled
     * @param enabled
     */
    public void setProxyEnabled(boolean enabled) {
        proxyEnabled = enabled;
    }

    /**
     * Setter for proxyAddress
     * @param address
     */
    public void setProxyAddress(String address) {
        proxyAddress = address;
    }

    /**
     * Setter for port
     * @param port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Getter for ipv6Enabled
     * @return true if ipv6 is enabled, false if disabled
     */
    public boolean getIpv6Enabled() {
        return ipv6Enabled;
    }

    /**
     * Getter for udpEnabled
     * @return true if udp is enabled, false if disabled
     */
    public boolean getUdpEnabled() {
        return udpEnabled;
    }

    /**
     * Getter for proxyEnabled
     * @return true if proxy is enabled, false if disabled
     */
    public boolean getProxyEnabled() {
        return proxyEnabled;
    }

    /**
     * Getter for proxyAddress
     * @return the current proxy address being used
     */
    public String getProxyAddress() {
        return proxyAddress;
    }

    /**
     * Getter for port
     * @return the current port being used for the proxy
     */
    public int getPort() {
        return port;
    }
}
