package org.onosproject.xmpp;

import org.onosproject.net.DeviceId;

/**
 * Created by Tomek Osiński on 17.07.17.
 */
public interface XmppController {


    public Map<DeviceId, XmppDevice> getXmppDevicesMap();



}
