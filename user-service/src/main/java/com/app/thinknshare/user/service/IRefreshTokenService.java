package com.app.thinknshare.user.service;

import com.app.thinknshare.user.dtos.DeviceDetails;
import com.app.thinknshare.user.dtos.RefreshTokenDetails;
import com.app.thinknshare.user.dtos.UserPrincipal;

public interface IRefreshTokenService {
    RefreshTokenDetails issue(UserPrincipal userPrincipal, DeviceDetails deviceDetails);
    RefreshTokenDetails verifyAndRotate(String incomingRefreshToken, DeviceDetails deviceDetails);
    void revoke(String incomingRefreshToken);
}
